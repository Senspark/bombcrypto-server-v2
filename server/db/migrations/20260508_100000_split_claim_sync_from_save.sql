-- Migration: Split claim_synced sync logic out of sp_save_user_claim_reward_data
-- Date: 2026-05-08
-- Applies to: bombcrypto database
--
-- Background: see CLAIM_TOKENS_INVALID_BUG_REPORT.md and CLAIM_TOKENS_REFACTOR_DESIGN.md
--
-- The original sp_save had a Branch B that fired when DB.claim_synced < on-chain totalClaim.
-- Branch B both (a) caught DB up to chain and (b) tried to handle the in-flight claim — these
-- two responsibilities conflicted, causing the contract to revert with "Claimed invalid"
-- whenever the user had pending > 0 in a stale-DB scenario.
--
-- Refactor:
-- 1. NEW sp_sync_user_claim_synced — pure idempotent sync. No claim semantics, no throw.
-- 2. NEW fn_sync_user_claim_synced — wrapper for CONFIRM path. Returns scalar (old_pending).
-- 3. sp_save_user_claim_reward_data — drops _api_synced_value param, drops Branch B.
--    Now does only the claim execution (= old Branch A semantics).
-- 4. fn_save_user_claim_reward_data — drops _claim_confirmed param, drops sp_fix call.
--    Now always calls sp_sync first, then re-reads state, then runs sp_save.
-- 5. DROP sp_fix_user_claim_reward_data — its behavior is a strict subset of sp_sync.
--
-- Order matters: drop fn_save first (depends on sp_save + sp_fix), then drop the SPs,
-- then create the new SPs, then re-create fn_save with the new signature.

----------------------------------------------------------------------
-- 1. Drop dependents first
----------------------------------------------------------------------
DROP FUNCTION IF EXISTS public.fn_save_user_claim_reward_data(
    integer, character varying, character varying, double precision, double precision, boolean
);

DROP PROCEDURE IF EXISTS public.sp_save_user_claim_reward_data(
    integer, character varying, character varying, double precision, double precision, double precision
);

DROP PROCEDURE IF EXISTS public.sp_fix_user_claim_reward_data(
    integer, character varying, character varying, double precision
);

----------------------------------------------------------------------
-- 2. NEW sp_sync_user_claim_synced — idempotent sync DB → on-chain
----------------------------------------------------------------------
CREATE PROCEDURE public.sp_sync_user_claim_synced(
    IN _uid integer,
    IN _data_type character varying,
    IN _reward_type character varying,
    IN _api_synced_value double precision
)
    LANGUAGE plpgsql
AS
$procedure$
DECLARE
    _old_pending DECIMAL          := 0;
    _old_synced  DECIMAL          := 0;
    _fee_percent DOUBLE PRECISION := 0;
BEGIN
    SELECT claim_pending, claim_synced
    INTO _old_pending, _old_synced
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    IF ROUND(_old_synced) >= ROUND(_api_synced_value) THEN
        -- DB already in sync (or ahead). No-op.
        RETURN;
    END IF;

    -- Catch up: chain has advanced past DB. Conservative semantic — assume any pending
    -- has already been minted on-chain (we cannot tell otherwise). Wipe pending, advance synced.
    -- `values` (new earnings since last claim) is preserved.
    UPDATE user_block_reward
    SET claim_pending           = 0,
        claim_synced            = _api_synced_value,
        modify_date             = NOW() AT TIME ZONE 'utc',
        last_time_claim_success = CURRENT_TIMESTAMP
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    -- Audit log: matches the old sp_save Branch B "Claim successful" log entry.
    -- Fee tier is recomputed from _old_pending (the amount that landed on chain).
    _fee_percent = CASE
                       WHEN _reward_type IN ('BOMBERMAN', 'BCOIN_DEPOSITED') THEN 0
                       WHEN _old_pending >= 80 THEN 3
                       WHEN _old_pending >= 60 THEN 6
                       ELSE 10 END;

    IF _old_pending > 0 THEN
        INSERT INTO log_user_claim_reward(uid, claim_date, value, reward_type, data_type)
        VALUES (_uid, CURRENT_TIMESTAMP, _old_pending - (_old_pending * _fee_percent / 100),
                _reward_type, _data_type);
    END IF;

    INSERT INTO logs.user_block_reward (uid, reward_type, network,
                                        claim_pending_old, claim_pending_changed, claim_pending_new,
                                        claim_synced, claim_synced_changed, claim_synced_new,
                                        reason)
    VALUES (_uid, _reward_type, _data_type,
            _old_pending, -_old_pending, 0,
            _old_synced, _api_synced_value - _old_synced, _api_synced_value,
            'Sync claim_synced from chain');

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$procedure$;

----------------------------------------------------------------------
-- 3. sp_save_user_claim_reward_data — claim execution only (no sync, no Branch B)
----------------------------------------------------------------------
CREATE PROCEDURE public.sp_save_user_claim_reward_data(
    IN _uid integer,
    IN _data_type character varying,
    IN _reward_type character varying,
    IN _min_claim double precision,
    IN _claim_fee_percent double precision
)
    LANGUAGE plpgsql
AS
$procedure$
DECLARE
    _current_value     DECIMAL := 0;
    _pending_value     DECIMAL := 0;
    _claim_value       DECIMAL := 0;
    _old_value         DECIMAL := 0;
    _old_pending_value DECIMAL := 0;
BEGIN
    SELECT values, claim_pending, values + claim_pending
    INTO _current_value, _pending_value, _claim_value
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    _old_value         = _current_value;
    _old_pending_value = _pending_value;

    IF _claim_value < _min_claim THEN
        RAISE EXCEPTION '%,%', 'Not enough reward to claim', 1019;
    END IF;

    -- Pre-condition guaranteed by fn_save: caller has already invoked sp_sync,
    -- so claim_synced == on-chain totalClaim. We only execute the claim here.
    _current_value = 0;
    _pending_value = _claim_value;

    UPDATE user_block_reward
    SET values        = _current_value,
        claim_pending = _pending_value,
        modify_date   = NOW() AT TIME ZONE 'utc'
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    INSERT INTO logs.user_block_reward (uid, reward_type, network,
                                        values_old, values_changed, values_new,
                                        claim_pending_old, claim_pending_changed, claim_pending_new,
                                        reason)
    VALUES (_uid, _reward_type, _data_type,
            _old_value, _current_value - _old_value, _current_value,
            _old_pending_value, _pending_value - _old_pending_value, _pending_value,
            'Claim');

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$procedure$;

----------------------------------------------------------------------
-- 4. fn_save_user_claim_reward_data — always sync first, then claim
----------------------------------------------------------------------
CREATE FUNCTION public.fn_save_user_claim_reward_data(
    _uid integer,
    _data_type character varying,
    _reward_type character varying,
    _min_claim double precision,
    _api_synced_value double precision
)
    RETURNS text
    LANGUAGE plpgsql
AS
$function$
DECLARE
    result             DOUBLE PRECISION;
    _claim_value       DOUBLE PRECISION;
    _claim_fee_percent DOUBLE PRECISION;
    _reward_gift       json := '[]';
BEGIN
    -- Step 1: bring DB.claim_synced up to chain. No-op if already in sync.
    -- After this call, claim_pending may have been wiped (if sync forwarded).
    CALL sp_sync_user_claim_synced(_uid, _data_type, _reward_type, _api_synced_value);

    -- Step 2: re-read state after sync.
    SELECT values + claim_pending
    INTO _claim_value
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    -- Step 3: compute fee tier from the post-sync claim value.
    SELECT CASE
               WHEN _reward_type IN ('BOMBERMAN', 'BCOIN_DEPOSITED') THEN 0
               WHEN _claim_value >= 80 THEN 3
               WHEN _claim_value >= 60 THEN 6
               ELSE 10 END
    INTO _claim_fee_percent;

    -- Step 4: execute the claim (will throw 'Not enough reward to claim' if below min).
    CALL sp_save_user_claim_reward_data(_uid, _data_type, _reward_type, _min_claim, _claim_fee_percent);

    result = (SELECT claim_synced + claim_pending - (claim_pending * _claim_fee_percent / 100)
              FROM user_block_reward
              WHERE uid = _uid
                AND reward_type = _reward_type
                AND type = _data_type);

    RETURN
        JSON_BUILD_OBJECT('value', result,
                          'received', _claim_value - (_claim_value * _claim_fee_percent / 100),
                          'gifts', _reward_gift
        )::text;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$function$;

----------------------------------------------------------------------
-- 5. NEW fn_sync_user_claim_synced — wrapper for CONFIRM_CLAIM_REWARD_SUCCESS_V2
----------------------------------------------------------------------
CREATE FUNCTION public.fn_sync_user_claim_synced(
    _uid integer,
    _data_type character varying,
    _reward_type character varying,
    _api_synced_value double precision
)
    RETURNS double precision
    LANGUAGE plpgsql
AS
$function$
DECLARE
    _old_pending DOUBLE PRECISION := 0;
BEGIN
    SELECT claim_pending
    INTO _old_pending
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    CALL sp_sync_user_claim_synced(_uid, _data_type, _reward_type, _api_synced_value);

    -- Returns the amount that was wiped (= the amount that landed on chain since last sync).
    -- Used by the client to display "you received X".
    RETURN _old_pending;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$function$;