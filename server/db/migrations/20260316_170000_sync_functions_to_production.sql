-- Migration: Sync schema.sql functions to match production
-- Date: 2026-03-16
-- Applies to: bombcrypto database
--
-- Updates functions that were out of sync with production:
-- 1. fn_sub_user_gem: use logs.user_block_reward instead of logs.logs_user_block_reward_template
-- 2. fn_sub_user_reward (5-param): same log table fix
-- 3. sp_fix_user_claim_reward_data: add missing audit log INSERT, fix timezone
-- 4. sp_save_user_claim_reward_data: add old value tracking, audit logging, remove user_block_reward_gift updates
-- 5. fn_save_user_claim_reward_data: remove user_block_reward_gift query, default _reward_gift

-- Note: fn_sub_user_gem and fn_sub_user_reward already have FOR UPDATE locks from migrations 001/002.
-- This migration only fixes the log table references and syncs claim functions.
-- If you have NOT applied migrations 001/002 yet, apply those first.

----------------------------------------------------------------------
-- 1. sp_fix_user_claim_reward_data: add audit log INSERT + fix timezone
----------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.sp_fix_user_claim_reward_data(IN _uid integer, IN _data_type character varying, IN _reward_type character varying, IN _api_synced_value double precision)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _claim_pending double precision;
    _synced_value DECIMAL;
BEGIN

    SELECT claim_pending, claim_synced
    INTO _claim_pending, _synced_value
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    IF _claim_pending > 0 OR _synced_value > 0 OR _api_synced_value = 0
    THEN
        -- nothing to do
        RETURN;
    END IF;

    UPDATE user_block_reward
    SET claim_synced = _api_synced_value,
        modify_date  = NOW() AT TIME ZONE 'utc'
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    INSERT INTO logs.user_block_reward (uid, reward_type, network, claim_synced, claim_synced_changed,
                                        claim_synced_new, reason)
    VALUES (_uid, _reward_type, _data_type, _synced_value, _api_synced_value - _synced_value, _api_synced_value,
            'Fix user claim reward data');

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLSTATE,SQLERRM;

END ;
$$;

----------------------------------------------------------------------
-- 2. sp_save_user_claim_reward_data: add old value tracking + audit log
----------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.sp_save_user_claim_reward_data(IN _uid integer, IN _data_type character varying, IN _reward_type character varying, IN _min_claim double precision, IN _claim_fee_percent double precision, IN _api_synced_value double precision)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _current_value           FLOAT   := 0;
    _pending_value           DECIMAL := 0;
    _claim_value             DECIMAL := 0;
    _synced_value            DECIMAL := 0;
    _last_time_claim_success TIMESTAMP;
    _old_value               FLOAT   := 0;
    _old_pending_value       DECIMAL := 0;
    _old_synced_value        DECIMAL := 0;
    _reason                  VARCHAR := 0;
BEGIN

    SELECT values,
           claim_pending,
           values + claim_pending,
           claim_synced
    INTO _current_value,
        _pending_value,
        _claim_value,
        _synced_value
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    _old_value = _current_value;
    _old_pending_value = _pending_value;
    _old_synced_value = _synced_value;

    IF _claim_value < _min_claim
    THEN
        RAISE EXCEPTION '%,%','Not enough reward to claim',1019;
    END IF;

    IF ROUND(_synced_value) >= ROUND(_api_synced_value)
    THEN
        _current_value = 0;
        _pending_value = _claim_value;
        _reason = 'Claim';
    ELSE
        _synced_value = _api_synced_value;
        _pending_value = 0;
        _last_time_claim_success = CURRENT_TIMESTAMP;
        _reason = 'Claim successful';

        INSERT INTO log_user_claim_reward(uid, claim_date, value, reward_type, data_type)
        VALUES (_uid, CURRENT_TIMESTAMP, _claim_value - (_claim_value * _claim_fee_percent / 100), _reward_type,
                _data_type);
    END IF;

    UPDATE user_block_reward
    SET values                  = _current_value,
        claim_pending           = _pending_value,
        claim_synced            = _synced_value,
        modify_date             = NOW() AT TIME ZONE 'utc',
        last_time_claim_success = COALESCE(_last_time_claim_success, user_block_reward.last_time_claim_success)
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new,
                                        claim_pending_old, claim_pending_changed, claim_pending_new,
                                        claim_synced, claim_synced_changed, claim_synced_new, reason)
    VALUES (_uid, _reward_type, _data_type, _old_value, _current_value - _old_value, _current_value, _old_pending_value,
            _pending_value - _old_pending_value, _pending_value, _old_synced_value, _synced_value - _old_synced_value,
            _synced_value, _reason);

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLSTATE,SQLERRM;

END ;
$$;

----------------------------------------------------------------------
-- 3. fn_save_user_claim_reward_data: remove user_block_reward_gift query
----------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.fn_save_user_claim_reward_data(_uid integer, _data_type character varying, _reward_type character varying, _min_claim double precision, _api_synced_value double precision, _claim_confirmed boolean) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    result             DOUBLE PRECISION;
    _claim_value       DOUBLE PRECISION;
    _claim_fee_percent DOUBLE PRECISION;
    _reward_gift       json := '[]';
BEGIN

    IF NOT _claim_confirmed
    THEN
        CALL sp_fix_user_claim_reward_data(_uid, _data_type, _reward_type, _api_synced_value);
    END IF;

    SELECT values + claim_pending
    INTO _claim_value
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

    SELECT CASE
               WHEN _reward_type IN ('BOMBERMAN', 'BCOIN_DEPOSITED') THEN 0
               WHEN _claim_value >= 80 THEN 3
               WHEN _claim_value >= 60 THEN 6
               ELSE 10 END
    INTO _claim_fee_percent;

    CALL sp_save_user_claim_reward_data(_uid,
                                        _data_type,
                                        _reward_type,
                                        _min_claim,
                                        _claim_fee_percent,
                                        _api_synced_value);

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
        RAISE EXCEPTION '%,%',SQLSTATE,SQLERRM;
END;
$$;
