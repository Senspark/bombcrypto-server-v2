-- Migration: Make sp_modify_rock_from_user_wallet idempotent and conflict-safe
-- Date: 2026-07-02
-- Applies to: bombcrypto database
--
-- Bug: pay-rock's crediting SP did a plain INSERT into user_create_rock, which has a
-- UNIQUE (tx, uid) constraint. When a row already existed for that (tx, uid) -- e.g. the
-- game had already written a PENDING/FALSE/ERROR row (a smart-account "delegated" burn was
-- rejected by the old verifyCreateRock sender check) -- the retry failed with:
--   duplicate key value violates unique constraint "user_create_rock_pk"
--
-- Fix:
--   1. Skip only when the burn is already credited: status = 'DONE' -> no-op (idempotent).
--      Only 'DONE' counts as credited; PENDING/FALSE/ERROR are not credited yet.
--   2. Upsert instead of plain INSERT: a prior PENDING/FALSE/ERROR row is flipped to 'DONE'
--      (and its heroes/rock_amount/network refreshed) rather than colliding on (tx, uid).
--
-- Idempotent: uses CREATE OR REPLACE, safe to run multiple times.

CREATE OR REPLACE PROCEDURE public.sp_modify_rock_from_user_wallet(IN _wallet character varying,
                                                                   IN _tx character varying, IN _heroes_ids jsonb,
                                                                   IN _amount integer, IN _network character varying)
    LANGUAGE plpgsql
    SECURITY DEFINER
AS
$procedure$
DECLARE
    _uid           INT;
    _current_value NUMERIC;
BEGIN
    -- Get user id
    SELECT id_user INTO _uid FROM public.user WHERE user_name = LOWER(_wallet);

    -- If user exists
    IF _uid IS NOT NULL THEN
        -- Idempotency: only status = 'DONE' counts as already credited. If this (tx, uid) is
        -- already DONE, do nothing. PENDING / FALSE / ERROR rows (e.g. the game's rejected
        -- verify of a smart-account delegated burn) are NOT credited yet and must proceed.
        IF EXISTS (SELECT 1 FROM public.user_create_rock
                   WHERE tx = _tx AND uid = _uid AND status = 'DONE') THEN
            RETURN;
        END IF;

        -- Get current rock value
        SELECT COALESCE((SELECT values FROM user_block_reward WHERE uid = _uid AND reward_type = 'ROCK'), 0)
        INTO _current_value;
        -- If the new value is not negative
        IF _current_value + _amount >= 0 THEN
            PERFORM fn_add_user_reward(_uid, 'TR', GREATEST(_amount, 0), 'ROCK', 'BURN_FAILED');
        ELSE
            -- Throw exception if new value is negative
            RAISE EXCEPTION 'Not enough rock to perform this operation';
        END IF;

        -- Lưu lại lịch sử
        INSERT INTO logs.logs_user_buy_rock_pack (uid, time_stamp, package_name, rock_amount, price,
                                                  token_name, network)
        VALUES (_uid, CURRENT_TIMESTAMP, 'BURN_FAILED', _amount, 0, 'ROCK', 'TR');

        -- Upsert on the (tx, uid) unique key: flip a prior PENDING/FALSE/ERROR row to DONE
        -- instead of colliding with it (which raised the duplicate-key error on retry).
        INSERT INTO public.user_create_rock(uid, tx, heroes, rock_amount, network, status)
        VALUES (_uid, _tx, _heroes_ids, _amount, _network, 'DONE')
        ON CONFLICT (tx, uid) DO UPDATE
            SET heroes = excluded.heroes,
                rock_amount = excluded.rock_amount,
                network = excluded.network,
                status = 'DONE',
                "timestamp" = CURRENT_TIMESTAMP;
    END IF;
END;
$procedure$;
