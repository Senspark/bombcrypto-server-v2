-- Migration: Fix race condition (TOCTOU double-spend) in fn_sub_user_gem
-- Date: 2026-03-15
-- Applies to: bombcrypto database
-- Related: PR #2
--
-- This adds PERFORM 1 ... FOR UPDATE row-level lock before the balance SELECT
-- in fn_sub_user_gem, preventing concurrent transactions from reading stale
-- balances and bypassing the sufficiency check.

CREATE OR REPLACE FUNCTION public.fn_sub_user_gem(_uid integer, _datatype character varying, _amount double precision, _reason character varying) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    _gem_locked_amount     DOUBLE PRECISION;
    _gem_amount            DOUBLE PRECISION;
    _sub_gem_locked_amount DOUBLE PRECISION;
    _sub_amount            DOUBLE PRECISION;
    _message               text;
    _new_gem_locked_amount DOUBLE PRECISION;
    _new_gem_amount        DOUBLE PRECISION;
BEGIN

    PERFORM 1
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type IN ('GEM_LOCKED', 'GEM')
      AND "type" = _dataType
    FOR UPDATE;

    SELECT COALESCE(SUM(CASE WHEN reward_type = 'GEM_LOCKED' THEN values ELSE 0 END), 0),
           COALESCE(SUM(CASE WHEN reward_type = 'GEM' THEN values ELSE 0 END), 0)
    INTO _gem_locked_amount,
        _gem_amount
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type IN ('GEM_LOCKED', 'GEM')
      AND "type" = _dataType;

    IF _amount > (_gem_locked_amount + _gem_amount)
    THEN
        _message = (SELECT CONCAT('Not enough ', _amount, 'GEM'));
        RAISE EXCEPTION 'Not enough % %',_amount, 'GEM';
    END IF;

    _sub_gem_locked_amount = CASE WHEN _amount <= _gem_locked_amount THEN _amount ELSE _gem_locked_amount END;
    _sub_amount = _amount - _sub_gem_locked_amount;

    IF _sub_gem_locked_amount != 0 THEN
        WITH reward AS (
            UPDATE user_block_reward
                SET values = values - _sub_gem_locked_amount,
                    modify_date = CURRENT_TIMESTAMP
                WHERE uid = _uid
                    AND reward_type = 'GEM_LOCKED'
                    AND "type" = _dataType
                RETURNING values)
        SELECT *
        INTO _new_gem_locked_amount
        FROM reward;

        INSERT INTO logs.logs_user_block_reward_template (uid, reward_type, network, values_old, values_changed,
                                                          values_new, reason)
        VALUES (_uid, 'GEM_LOCKED', _dataType, _gem_locked_amount, -_sub_gem_locked_amount, _new_gem_locked_amount,
                _reason);
    END IF;

    IF _sub_amount != 0 THEN
        WITH reward AS (
            UPDATE user_block_reward
                SET values = values - _sub_amount,
                    modify_date = CURRENT_TIMESTAMP
                WHERE uid = _uid
                    AND reward_type = 'GEM'
                    AND "type" = _dataType
                RETURNING values)
        SELECT *
        INTO _new_gem_amount
        FROM reward;

        INSERT INTO logs.logs_user_block_reward_template (uid, reward_type, network, values_old, values_changed,
                                                          values_new, reason)
        VALUES (_uid, 'GEM', _dataType, _gem_amount, -_sub_amount, _new_gem_amount, _reason);
    END IF;

    RETURN JSON_BUILD_OBJECT('GEM_LOCKED', _sub_gem_locked_amount, 'GEM', _sub_amount)::text;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%',SQLERRM;
END;
$$;
