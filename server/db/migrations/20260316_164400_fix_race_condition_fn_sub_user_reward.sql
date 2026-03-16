-- Migration: Fix race condition (TOCTOU double-spend) in fn_sub_user_reward
-- Date: 2026-03-16
-- Applies to: bombcrypto database
--
-- This adds PERFORM 1 ... FOR UPDATE row-level locks before the balance SELECT
-- in both fn_sub_user_reward overloads, preventing concurrent transactions from
-- reading stale balances and bypassing the sufficiency check.
-- Same fix pattern as fn_sub_user_gem (PR #2).

-- Fix 5-param overload
CREATE OR REPLACE FUNCTION public.fn_sub_user_reward(_uid integer, _networktype character varying, _amount double precision, _rewardtype character varying, _reason character varying) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    rewardAmount FLOAT;
    message      TEXT;
    rewardNew    FLOAT;
BEGIN
    PERFORM 1
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _rewardType
      AND type = _networktype
    FOR UPDATE;

    SELECT COALESCE(SUM(CASE WHEN reward_type = _rewardType THEN "values" ELSE 0 END), 0)
    INTO rewardAmount
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _rewardType
      AND type = _networktype;

    IF _amount > rewardAmount THEN
        message := '1019,Not enough ' || _amount || ' ' || _rewardType;
        RAISE EXCEPTION '%', message;
    END IF;

    WITH reward AS (
        UPDATE user_block_reward
            SET values = values - _amount,
                modify_date = NOW() AT TIME ZONE 'utc'
            WHERE uid = _uid
                AND reward_type = _rewardtype
                AND type = _networktype
            RETURNING values)
    SELECT *
    INTO rewardNew
    FROM reward;

    INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new,
                                                      reason)
    VALUES (_uid, _rewardtype, _networktype, rewardAmount, -_amount, rewardNew, _reason);

    RETURN json_build_object('subAmount', _amount)::text;
EXCEPTION
    WHEN SQLSTATE '45000' THEN
        GET STACKED DIAGNOSTICS message = MESSAGE_TEXT;
        RAISE EXCEPTION '%', message;
END;
$$;

-- Fix 6-param overload (with deposit reward type)
CREATE OR REPLACE FUNCTION public.fn_sub_user_reward(_uid integer, _networktype character varying, _amount double precision, _rewardtype character varying, _depositrewardtype character varying, _reason character varying) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    rewardAmount           FLOAT;
    depositRewardAmount    FLOAT;
    message                TEXT;
    depositSubAmount       FLOAT;
    subAmount              FLOAT;
    newRrewardAmount       FLOAT;
    newDepositRewardAmount FLOAT;
BEGIN
    PERFORM 1
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type IN (_rewardType, _depositRewardType)
      AND type = _networktype
    FOR UPDATE;

    SELECT COALESCE(SUM(CASE WHEN reward_type = _rewardType THEN "values" ELSE 0 END), 0),
           COALESCE(SUM(CASE WHEN reward_type = _depositRewardType THEN "values" ELSE 0 END), 0)
    INTO rewardAmount, depositRewardAmount
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type IN (_rewardType, _depositRewardType)
      AND type = _networktype;

    IF _amount > (rewardAmount + depositRewardAmount) THEN
        message := '1019,Not enough ' || _amount || ' ' || _rewardType;
        RAISE EXCEPTION '%', message;
    END IF;

    depositSubAmount := CASE WHEN _amount <= depositRewardAmount THEN _amount ELSE depositRewardAmount END;
    subAmount := _amount - depositSubAmount;

    IF subAmount != 0 THEN
        WITH reward AS (
            UPDATE user_block_reward
                SET "values" = "values" - subAmount,
                    modify_date = CURRENT_TIMESTAMP
                WHERE uid = _uid
                    AND type = _networktype
                    AND reward_type = _rewardType
                RETURNING values)
        SELECT *
        INTO newRrewardAmount
        FROM reward;

        INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new, reason)
        VALUES (_uid, _rewardType, _networktype, rewardAmount, -subAmount, newRrewardAmount, _reason);
    END IF;

    IF depositSubAmount != 0 THEN
        WITH reward AS (
            UPDATE user_block_reward
                SET "values" = "values" - depositSubAmount,
                    modify_date = CURRENT_TIMESTAMP
                WHERE uid = _uid
                    AND type = _networktype
                    AND reward_type = _depositRewardType
                RETURNING values)
        SELECT *
        INTO newDepositRewardAmount
        FROM reward;

        INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new, reason)
        VALUES (_uid, _depositRewardType, _networktype, depositRewardAmount, -depositSubAmount, newDepositRewardAmount,
                _reason);
    END IF;

    RETURN json_build_object('subAmount', subAmount, 'depositSubAmount', depositSubAmount)::text;
EXCEPTION
    WHEN SQLSTATE '45000' THEN
        GET STACKED DIAGNOSTICS message = MESSAGE_TEXT;
        RAISE EXCEPTION '%', message;
END;
$$;
