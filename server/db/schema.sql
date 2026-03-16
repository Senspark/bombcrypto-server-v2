--
-- PostgreSQL database dump
--

\restrict SrJR32Ye34b8ONLQfcfd5zOvoS8QoXHQikYMmdW6PJGFFahxk0lmYTHfJ8o4IjF

-- Dumped from database version 17.6 (Debian 17.6-2.pgdg13+1)
-- Dumped by pg_dump version 17.7 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: logs; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA logs;


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: hero_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.hero_type AS ENUM (
    'FI',
    'TRIAL',
    'TR'
);


--
-- Name: rewardtype; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.rewardtype AS ENUM (
    'BOOSTER',
    'REWARD'
);


--
-- Name: logs_th_mode_v2_create_partition_from_template(text); Type: FUNCTION; Schema: logs; Owner: -
--

CREATE FUNCTION logs.logs_th_mode_v2_create_partition_from_template(date_part text) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
    partition_name TEXT;
BEGIN
    partition_name := 'logs_th_mode_v2_' || date_part;

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS logs.%I (LIKE logs.logs_th_mode_v2_template INCLUDING ALL)',
                   partition_name
            );

    RETURN;
END;
$$;


--
-- Name: logs_th_mode_v2_insert_trigger(); Type: FUNCTION; Schema: logs; Owner: -
--

CREATE FUNCTION logs.logs_th_mode_v2_insert_trigger() RETURNS trigger
    LANGUAGE plpgsql
    AS $_$
DECLARE
    partition_name TEXT;
    date_part TEXT;
BEGIN
    date_part := to_char(NEW.timestamp, 'YYYY_MM_DD');
    partition_name := 'logs_th_mode_v2_' || date_part;

    PERFORM logs.logs_th_mode_v2_create_partition_from_template(date_part);

    EXECUTE FORMAT('
        INSERT INTO logs.%I (race_id, uid, hero_id, network_id, pool_index, reward_level, timestamp, reward_bcoin, reward_sen, reward_coin)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)',
                   partition_name
            ) USING NEW.race_id, NEW.uid, NEW.hero_id, NEW.network_id, NEW.pool_index, NEW.reward_level, NEW.timestamp, NEW.reward_bcoin, NEW.reward_sen, NEW.reward_coin;

    INSERT INTO logs.logs_th_mode_v2_races(race_id, timestamp, table_name)
    VALUES (NEW.race_id, NEW.timestamp, date_part)
    ON CONFLICT (race_id) DO NOTHING;

    RETURN NULL;
END;
$_$;


--
-- Name: logs_user_block_reward_create_partition_from_template(text); Type: FUNCTION; Schema: logs; Owner: -
--

CREATE FUNCTION logs.logs_user_block_reward_create_partition_from_template(date_part text) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
    partition_name TEXT;
BEGIN
    partition_name := 'logs_user_block_reward_' || date_part;

    EXECUTE format('
                            CREATE TABLE IF NOT EXISTS logs.%I (LIKE logs.logs_user_block_reward_template INCLUDING ALL)',
                   partition_name
            );

    RETURN;
END;
$$;


--
-- Name: logs_user_block_reward_insert_trigger(); Type: FUNCTION; Schema: logs; Owner: -
--

CREATE FUNCTION logs.logs_user_block_reward_insert_trigger() RETURNS trigger
    LANGUAGE plpgsql
    AS $_$
DECLARE
    partition_name TEXT;
    date_part      TEXT;
BEGIN
    date_part := to_char(NOW() AT TIME ZONE 'utc', 'YYYY_MM_DD');
    partition_name := 'logs_user_block_reward_' || date_part;

    PERFORM logs.logs_user_block_reward_create_partition_from_template(date_part);

    EXECUTE format('
        INSERT INTO logs.%I (uid, reward_type, network, values_old, values_changed, values_new, claim_pending_old, claim_pending_changed, claim_pending_new, claim_synced, claim_synced_changed, claim_synced_new, reason, changed_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)',
                   partition_name
            ) USING NEW.uid, NEW.reward_type, NEW.network, NEW.values_old, NEW.values_changed, NEW.values_new, NEW.claim_pending_old, NEW.claim_pending_changed, NEW.claim_pending_new, NEW.claim_synced, NEW.claim_synced_changed, NEW.claim_synced_new, NEW.reason, CURRENT_TIMESTAMP;

    RETURN NULL;
END;
$_$;


--
-- Name: fn_add_user_reward(integer, character varying, double precision, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_add_user_reward(_uid integer, _networktype character varying, _amount double precision, _rewardtype character varying, _reason character varying) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    rewardAmount FLOAT;
    message      TEXT;
    rewardNew    FLOAT;
BEGIN
    SELECT COALESCE(SUM(CASE WHEN reward_type = _rewardType THEN "values" ELSE 0 END), 0)
    INTO rewardAmount
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _rewardType
      AND type = _networktype;

    WITH reward AS (
        INSERT INTO user_block_reward (uid, reward_type, type, "values", total_values, modify_date, last_time_claim_success)
            VALUES (_uid, _rewardType, _networktype, _amount, _amount, NOW() AT TIME ZONE 'utc', NOW() AT TIME ZONE 'utc')
            ON CONFLICT (uid, type, reward_type)
                DO UPDATE SET "values" = user_block_reward."values" + EXCLUDED."values",
                    total_values = user_block_reward.total_values + excluded.total_values,
                    modify_date = NOW() AT TIME ZONE 'utc'
            RETURNING values)
    SELECT *
    INTO rewardNew
    FROM reward;

    INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new, reason)
    VALUES (_uid, _rewardtype, _networktype, rewardAmount, _amount, rewardNew, _reason);

    RETURN json_build_object('addAmount', _amount)::text;
EXCEPTION
    WHEN SQLSTATE '45000' THEN
        GET STACKED DIAGNOSTICS message = MESSAGE_TEXT;
        RAISE EXCEPTION '%', message;
END;
$$;


--
-- Name: fn_calculate_package_auto_price(integer, json); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_calculate_package_auto_price(_uid integer, package_json json) RETURNS TABLE(package character varying, num_days integer, price_percent double precision, min_price double precision, price double precision)
    LANGUAGE plpgsql
    AS $$
DECLARE
    sql_query   TEXT;
    mined_value DECIMAL(10, 4) := 0.0;
    mined_day   DECIMAL(10, 4);
BEGIN
    sql_query := 'SELECT COALESCE(SUM(values_changed), 0) FROM logs.user_block_reward WHERE uid = ' || quote_literal(_uid) ||
                 ' AND reward_type = ''BCOIN'' AND reason = ''Save game''' ||
                 ' AND DATE(changed_at) BETWEEN CURRENT_DATE - INTERVAL ''7 days'' AND CURRENT_DATE - INTERVAL ''1 day''';
    EXECUTE sql_query INTO mined_day;
    mined_value := mined_value + mined_day;

    RETURN QUERY
        SELECT r.package,
               r.num_days,
               r.price_percent::double precision,
               r.min_price::double precision,
               GREATEST(r.min_price, ROUND(mined_value * (r.price_percent / 100)::DECIMAL(10, 4)))::double precision
        FROM json_to_recordset(package_json::json) AS r(package VARCHAR(40), num_days INT,
                                                        price_percent double precision, min_price double precision);
END;
$$;


--
-- Name: fn_create_tournament_match_v2(integer, text, integer, text, timestamp with time zone, timestamp with time zone, integer, text, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_create_tournament_match_v2(p_user1_id integer, p_user1_name text, p_user2_id integer, p_user2_name text, p_from_time timestamp with time zone, p_to_time timestamp with time zone, p_hero_stats integer, p_server text, p_mode integer) RETURNS TABLE(success boolean, fixture_id integer, error_reason text)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_fixture_id integer;
    v_fixture_conflict_exists boolean;
    v_tournament_conflict_exists boolean;
BEGIN

    -- Step 1: Check for existing fixture conflicts
    SELECT EXISTS(
        SELECT 1 FROM pvp_fixture_matches
        WHERE (
            (player_1_uid = p_user1_id OR player_2_uid = p_user1_id OR
             player_1_uid = p_user2_id OR player_2_uid = p_user2_id) AND
            (
                (from_time <= p_to_time AND to_time >= p_from_time) OR
                (from_time <= p_from_time AND to_time >= p_to_time) OR
                (from_time >= p_from_time AND to_time <= p_to_time)
            )
        )
    ) INTO v_fixture_conflict_exists;

    IF v_fixture_conflict_exists THEN
        RETURN QUERY SELECT
            false::boolean,
            NULL::integer,
            'Match already exists in fixture table'::text;
        RETURN;
    END IF;


    -- Step 2: Check for tournament conflicts
    SELECT EXISTS(
        SELECT 1 FROM pvp_tournament
        WHERE (
            (participant_1 = p_user1_name AND participant_2 = p_user2_name) OR
            (participant_1 = p_user2_name AND participant_2 = p_user1_name)
        ) AND (
            (find_begin_time <= p_to_time AND find_end_time >= p_from_time) OR
            (find_begin_time <= p_from_time AND find_end_time >= p_to_time) OR
            (find_begin_time >= p_from_time AND find_end_time <= p_to_time)
        )
    ) INTO v_tournament_conflict_exists;

    IF v_tournament_conflict_exists THEN
        RETURN QUERY SELECT
            false::boolean,
            NULL::integer,
            'Match already exists in tournament table'::text;
        RETURN;
    END IF;

    -- Start transaction
    BEGIN
        -- Step 3: Insert into pvp_fixture_matches
        INSERT INTO pvp_fixture_matches
            (player_1_uid, player_1_username, player_2_uid, player_2_username, hero_profile, fixed_zone, mode, from_time, to_time)
        VALUES
            (p_user1_id, p_user1_name, p_user2_id, p_user2_name, p_hero_stats, p_server, p_mode, p_from_time, p_to_time)
        RETURNING id INTO v_fixture_id;


        -- Step 4: Insert into pvp_tournament
        INSERT INTO pvp_tournament
            (id, mode, participant_1, participant_2, status, find_begin_time, find_end_time)
        VALUES
            (v_fixture_id, p_mode, p_user1_name, p_user2_name, 'PENDING', p_from_time, p_to_time);

        -- Return success
        RETURN QUERY SELECT
            true::boolean,
            v_fixture_id,
            NULL::text;

    EXCEPTION WHEN OTHERS THEN

        RETURN QUERY SELECT
            false::boolean,
            NULL::integer,
            'Database error: ' || SQLERRM::text;
    END;
END;
$$;


--
-- Name: fn_decay_user_pvp_rank(character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_decay_user_pvp_rank(table_rank_ss character varying) RETURNS TABLE(uid integer, point_decay integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
BEGIN
    CREATE TEMP TABLE tmp
    (
        uid         INT,
        point_decay INT
    );
    WITH list_decay_user AS (
        UPDATE table_rank_ss AS u
            SET point =
                    CASE
                        WHEN u.matches_in_current_date > c.min_matches
                            THEN u.point - c.sub_value
                        ELSE u.point
                        END
            FROM config_pvp_ranking AS c
            WHERE u.point BETWEEN c.start_point AND c.end_point
            RETURNING u.uid, CASE
                                 WHEN u.matches_in_current_date > c.min_matches
                                     THEN c.sub_value
                                 ELSE 0
                END AS point_decay)
    INSERT
    INTO tmp (uid, point_decay)
    SELECT *
    FROM list_decay_user
    WHERE point_decay != 0;

    UPDATE table_rank_ss AS u
    SET matches_in_current_date = 0;
    CALL sp_update_user_pvp_rank();
    RETURN QUERY SELECT * FROM tmp;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END
$$;


--
-- Name: fn_delete_tournament_match_v2(integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_delete_tournament_match_v2(p_match_id integer) RETURNS TABLE(success boolean, message text)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_exists BOOLEAN;
BEGIN
    -- Check if the match exists in pvp_tournament table
    SELECT EXISTS(SELECT 1 FROM pvp_tournament WHERE id = p_match_id) INTO v_exists;

    IF NOT v_exists THEN
        RETURN QUERY SELECT FALSE, 'Match not found in tournament table';
        RETURN;
    END IF;

    -- Begin transaction
    BEGIN
        -- Backup the tournament data to pvp_tournament_backup
        INSERT INTO pvp_tournament_backup
        SELECT * FROM pvp_tournament
        WHERE id = p_match_id;

        -- Delete from pvp_fixture_matches
        DELETE FROM pvp_fixture_matches WHERE id = p_match_id;

        -- Delete from pvp_tournament
        DELETE FROM pvp_tournament WHERE id = p_match_id;

        -- Return success
        RETURN QUERY SELECT TRUE, 'Tournament match deleted successfully';
    EXCEPTION WHEN OTHERS THEN
        -- Return error on exception
        RETURN QUERY SELECT FALSE, 'Error deleting tournament match: ' || SQLERRM;
    END;
END;
$$;


--
-- Name: fn_edit_tournament_match_v2(integer, integer, text, integer, text, integer, text, integer, timestamp with time zone, timestamp with time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_edit_tournament_match_v2(p_id integer, p_player_1_uid integer, p_player_1_username text, p_player_2_uid integer, p_player_2_username text, p_hero_profile integer, p_server text, p_mode integer, p_from_datetime timestamp with time zone, p_to_datetime timestamp with time zone) RETURNS TABLE(success boolean, message text)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_match_status TEXT;
BEGIN
    -- Step 1: Check if the match exists and is in 'PENDING' status
    SELECT status INTO v_match_status FROM pvp_tournament WHERE id = p_id;

    IF NOT FOUND THEN
        RETURN QUERY SELECT false, 'Match not found';
        RETURN;
    END IF;

    IF v_match_status IS NULL OR UPPER(v_match_status) <> 'PENDING' THEN
        RETURN QUERY SELECT false, 'Only pending matches can be edited';
        RETURN;
    END IF;

    -- Step 2: Check for schedule conflicts with other matches (excluding current match)
    IF EXISTS (
        SELECT 1 FROM pvp_fixture_matches
        WHERE id != p_id AND (
            (player_1_uid = p_player_1_uid OR player_2_uid = p_player_1_uid OR
             player_1_uid = p_player_2_uid OR player_2_uid = p_player_2_uid) AND
            (
                (from_time <= p_to_datetime AND to_time >= p_from_datetime) OR
                (from_time <= p_from_datetime AND to_time >= p_to_datetime) OR
                (from_time >= p_from_datetime AND to_time <= p_to_datetime)
            )
        )
    ) THEN
        RETURN QUERY SELECT false, 'Schedule conflict with existing match';
        RETURN;
    END IF;

    -- Step 3: Check for tournament conflicts (excluding current match)
    IF EXISTS (
        SELECT 1 FROM pvp_tournament
        WHERE id != p_id AND (
            (participant_1 = p_player_1_username OR participant_2 = p_player_2_username) AND
            (
                (find_begin_time <= p_to_datetime AND find_end_time >= p_from_datetime) OR
                (find_begin_time <= p_from_datetime AND find_end_time >= p_to_datetime) OR
                (find_begin_time >= p_from_datetime AND find_end_time <= p_to_datetime)
            )
        )
    ) THEN
        RETURN QUERY SELECT false, 'Schedule conflict in tournament table';
        RETURN;
    END IF;

    -- Step 4: Update pvp_fixture_matches table
    UPDATE pvp_fixture_matches
    SET player_1_uid = p_player_1_uid,
        player_1_username = p_player_1_username,
        player_2_uid = p_player_2_uid,
        player_2_username = p_player_2_username,
        hero_profile = p_hero_profile,
        fixed_zone = p_server,
        mode = p_mode,
        from_time = p_from_datetime,
        to_time = p_to_datetime
    WHERE id = p_id;

    -- Step 5: Update pvp_tournament table
    UPDATE pvp_tournament
    SET participant_1 = p_player_1_username,
        participant_2 = p_player_2_username,
        mode = p_mode,
        find_begin_time = p_from_datetime,
        find_end_time = p_to_datetime
    WHERE id = p_id;

    -- Return success
    RETURN QUERY SELECT true, 'Match successfully updated';
END;
$$;


--
-- Name: fn_fusion_hero_server(integer, character varying, double precision, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_fusion_hero_server(uid integer, hero_ids character varying, amount_token double precision, _reward_type character varying, _network_type character varying) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    reason_fail TEXT;
    hero_ids_array INTEGER[];
    reason TEXT;
BEGIN
    -- Dynamically set the reason based on the network type
    reason := 'Fusion hero ' || _network_type;

    -- Call fn_sub_user_reward function
    BEGIN
        PERFORM fn_sub_user_reward(uid, _network_type, amount_token, _reward_type, reason);
    EXCEPTION
        WHEN OTHERS THEN
            reason_fail := 'Sub reward fail';
            RETURN reason_fail;
    END;

    -- Convert hero_ids from VARCHAR to INTEGER array
    hero_ids_array := string_to_array(hero_ids, ',')::INTEGER[];

    -- Update user_bomber table
    BEGIN
        UPDATE public.user_bomber
        SET "hasDelete" = 1
        WHERE bomber_id = ANY(hero_ids_array)
          AND data_type = _network_type
          AND "hasDelete" = 0;
    EXCEPTION
        WHEN OTHERS THEN
            reason_fail := 'Delete hero fail';
            RETURN reason_fail;
    END;

    RETURN reason_fail;
END;
$$;


--
-- Name: fn_fusion_hero_ton(integer, character varying, double precision); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_fusion_hero_ton(uid integer, hero_ids character varying, amount_ton double precision) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    reason_fail TEXT := '';
    hero_ids_array INTEGER[];
BEGIN
    -- Convert hero_ids from VARCHAR to INTEGER array and append target
    hero_ids_array := string_to_array(hero_ids, ',')::INTEGER[];

    -- Update user_bomber table
    BEGIN
        UPDATE public.user_bomber
        SET "hasDelete" = 1
        WHERE bomber_id = ANY(hero_ids_array)
          AND data_type = 'TON'
          AND "hasDelete" = 0;
    EXCEPTION
        WHEN OTHERS THEN
            reason_fail := 'Delete hero fail';
            RETURN reason_fail;
    END;

    -- Call fn_sub_user_reward function
    BEGIN
        PERFORM fn_sub_user_reward(uid, 'TON', amount_ton, 'TON_DEPOSITED', 'Fusion hero Ton');
    EXCEPTION
        WHEN OTHERS THEN
            reason_fail := 'Sub ton deposit fail';
            RETURN reason_fail;
    END;

    RETURN reason_fail;
END;
$$;


--
-- Name: fn_get_all_season_coin_ranking(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_get_all_season_coin_ranking() RETURNS TABLE(uid integer, coin double precision, network character varying, name character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
        WITH result AS (SELECT urc.uid, sum(urc.coin) AS coin, urc.network
                        FROM user_ranking_coin urc
                        WHERE urc.season > 0
                        GROUP BY urc.uid, urc.network)
        SELECT r.*,
               CASE
                   WHEN u.name IS NOT NULL THEN u.name
                   WHEN u.second_username IS NOT NULL THEN u.second_username
                   ELSE
                       CASE
                           WHEN LENGTH(u.user_name) > 10 THEN CONCAT(SUBSTRING(u.user_name, 0, 6), '...',
                                                                     SUBSTRING(u.user_name, LENGTH(u.user_name) - 3, 4))
                           ELSE u.user_name END
                   END AS name
        FROM result AS r
                 INNER JOIN "user" AS u ON r.uid = u.id_user;
END;
$$;


--
-- Name: fn_get_coin_ranking_2(integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_get_coin_ranking_2(_season integer) RETURNS TABLE(uid integer, coin double precision, network character varying, season integer, name character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
        WITH result AS (SELECT * FROM user_ranking_coin WHERE user_ranking_coin.season = _season)
        SELECT r.*,
               CASE
                   WHEN u.name IS NOT NULL THEN u.name
                   WHEN u.second_username IS NOT NULL THEN u.second_username
                   ELSE
                       CASE
                           WHEN LENGTH(u.user_name) > 10 THEN CONCAT(SUBSTRING(u.user_name, 0, 6), '...',
                                                                     SUBSTRING(u.user_name, LENGTH(u.user_name) - 3, 4))
                           ELSE u.user_name END
                   END as name
        FROM result AS r
                 INNER JOIN "user" AS u ON r.uid = u.id_user;
END;
$$;


--
-- Name: fn_get_coin_ranking_3(integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_get_coin_ranking_3(_season integer) RETURNS TABLE(uid integer, coin_total double precision, coin_current_season double precision, network character varying, name character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
        WITH result AS (SELECT urc.uid,
                               SUM(urc.coin)                                            AS coin_total,
                               SUM(CASE WHEN urc.season = _season THEN urc.coin ELSE 0 END) AS coin_current_season,
                               urc.network
                        FROM user_ranking_coin urc
                        GROUP BY urc.uid, urc.network)
        SELECT r.*,
               CASE
                   WHEN u.name IS NOT NULL THEN u.name
                   WHEN u.second_username IS NOT NULL THEN u.second_username
                   ELSE
                       CASE
                           WHEN LENGTH(u.user_name) > 10 THEN CONCAT(SUBSTRING(u.user_name, 0, 6), '...',
                                                                     SUBSTRING(u.user_name, LENGTH(u.user_name) - 3, 4))
                           ELSE u.user_name END
                   END AS name
        FROM result AS r
                 INNER JOIN "user" AS u ON r.uid = u.id_user;
END;
$$;


--
-- Name: fn_get_coin_ranking_4(integer, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_get_coin_ranking_4(_season integer, _network character varying) RETURNS TABLE(uid integer, coin_total double precision, coin_current_season double precision, network character varying, name character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
        WITH result AS (SELECT urc.uid,
                               SUM(urc.coin)                                            AS coin_total,
                               SUM(CASE WHEN urc.season = _season THEN urc.coin ELSE 0 END) AS coin_current_season,
                               urc.network
                        FROM user_ranking_coin urc
                        WHERE urc.network = _network
                        GROUP BY urc.uid, urc.network)
        SELECT r.*,
               CASE
                   WHEN u.name IS NOT NULL THEN u.name
                   WHEN u.second_username IS NOT NULL THEN u.second_username
                   ELSE
                       CASE
                           WHEN LENGTH(u.user_name) > 10 THEN CONCAT(SUBSTRING(u.user_name, 0, 6), '...',
                                                                     SUBSTRING(u.user_name, LENGTH(u.user_name) - 3, 4))
                           ELSE u.user_name END
                   END AS name
        FROM result AS r
                 INNER JOIN "user" AS u ON r.uid = u.id_user;
END;
$$;


--
-- Name: fn_get_coin_ranking_5(integer, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_get_coin_ranking_5(_season integer, _network character varying) RETURNS TABLE(uid integer, coin_total double precision, coin_current_season double precision, network character varying, name character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
        WITH result AS (SELECT urc.uid,
                               SUM(urc.coin)                                            AS coin_total,
                               SUM(CASE WHEN urc.season = _season THEN urc.coin ELSE 0 END) AS coin_current_season,
                               urc.network
                        FROM user_ranking_coin urc
                        WHERE urc.network = _network
                        GROUP BY urc.uid, urc.network)
        SELECT r.*,
               CASE
                   WHEN u.name IS NOT NULL THEN u.name
                   WHEN u.second_username IS NOT NULL THEN u.second_username
                   ELSE u.user_name
                   END AS name
        FROM result AS r
                 INNER JOIN "user" AS u ON r.uid = u.id_user;
END;
$$;


--
-- Name: fn_get_heroes_ton(integer, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_get_heroes_ton(_uid integer, _limit integer) RETURNS TABLE(uid integer, active smallint, stage smallint, energy integer, time_rest timestamp without time zone, bomber_id bigint, data_type character varying, power integer, bomb_range integer, stamina integer, speed integer, bomb integer, ability character varying, charactor integer, color integer, rare integer, bomb_skin integer, type integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
BEGIN
    IF _limit <= 0 OR _limit > 500 THEN
        _limit := 500;
    END IF;

    RETURN QUERY
        SELECT ub.uid,
               ub.active,
               ub.stage,
               ub.energy,
               ub.time_rest,
               ub.bomber_id,
               ub.data_type,
               ub.power,
               ub.bomb_range,
               ub.stamina,
               ub.speed,
               ub.bomb,
               ub.ability,
               ub.charactor,
               ub.color,
               ub.rare,
               ub.bomb_skin,
               ub.type
        FROM user_bomber ub
        WHERE ub.uid = _uid
          AND ub.data_type = 'TON'
          AND ub."hasDelete" = 0
        ORDER BY ub.active DESC, ub.rare DESC, ub.power DESC
        LIMIT _limit;
END;
$$;


--
-- Name: fn_insert_new_bomberman(integer, integer, character varying, character varying, integer, integer, integer, integer, integer, integer, integer, character varying, integer, integer, integer, integer, integer, integer, timestamp with time zone, boolean, character varying, character varying, integer, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_insert_new_bomberman(_userid integer, _heroid integer, _datatype character varying, _details character varying, _herotype integer, _level integer, _bombpower integer, _bombrange integer, _stamina integer, _speed integer, _bombcount integer, _ability character varying, _skin integer, _color integer, _rarity integer, _bombskin integer, _energy integer, _stage integer, _timerest timestamp with time zone, _isactive boolean, _shield character varying, _abilitys character varying, _shieldlevel integer, _quantity integer) RETURNS TABLE(uid integer, bid integer, hero_tr_type character varying, old_owner integer, lock_until timestamp with time zone)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_old_owner    int;
    v_is_locked    boolean;
    v_lock_until   timestamp with time zone;
    v_lock_seconds int;
    v_now          timestamp with time zone;
BEGIN

    v_now := NOW()::timestamp;
    v_is_locked := FALSE;
    v_old_owner := NULL;
    v_lock_seconds := 3600 * 3;
    v_lock_until := NULL;

    SELECT ub.uid
    INTO v_old_owner
    FROM user_bomber ub
    WHERE ub.uid <> _userId
      AND ub.bomber_id = _heroid
      AND ub.type = _herotype
      AND ub.data_type = _datatype;

    IF v_old_owner IS NOT NULL THEN
        _isactive := FALSE;
        v_is_locked := TRUE;
        v_lock_until := v_now + (v_lock_seconds || ' seconds')::INTERVAL;

        INSERT INTO user_bomber_lock (bomber_id, hero_type, data_type, lock_since, lock_seconds, reason)
        VALUES (_heroId, _herotype, _dataType, v_now, v_lock_seconds,
                'Transfer from uid ' || v_old_owner::text || ' to ' || _userId::text)
        ON CONFLICT (bomber_id, hero_type, data_type) DO UPDATE SET lock_since   = excluded.lock_since,
                                                                    lock_seconds = excluded.lock_seconds,
                                                                    reason       = excluded.reason;
    END IF;

    RETURN QUERY
        INSERT
            INTO user_bomber AS ub (uid,
                                    gen_id,
                                    "bomber_id",
                                    level,
                                    power,
                                    bomb_range,
                                    stamina,
                                    speed,
                                    bomb,
                                    ability,
                                    charactor,
                                    color,
                                    rare,
                                    bomb_skin,
                                    energy,
                                    stage,
                                    time_rest,
                                    active,
                                    shield,
                                    ability_shield,
                                    shield_level,
                                    "type",
                                    data_type,
                                    hero_tr_type)
                SELECT _userId,
                       _details,
                       _heroId,
                       _level,
                       _bombPower,
                       _bombRange,
                       _stamina,
                       _speed,
                       _bombCount,
                       _ability,
                       _skin,
                       _color,
                       _rarity,
                       _bombSkin,
                       _energy,
                       _stage,
                       _timeRest,
                       CASE WHEN _isActive THEN 1 ELSE 0 END,
                       _shield,
                       _abilityS,
                       _shieldLevel,
                       _heroType,
                       _dataType,
                       'HERO'
                FROM GENERATE_SERIES(1, _quantity)
                ON CONFLICT ("bomber_id", "type", "data_type")
                    DO UPDATE SET "hasDelete" = 0,
                        active = excluded.active,
                        uid = excluded.uid,
                        time_rest = excluded.time_rest,
                        stage = _stage,
                        hero_tr_type = excluded.hero_tr_type
                RETURNING ub.uid, ub.bomber_id::INT AS bid, ub.hero_tr_type, v_old_owner, v_lock_until;

END;
$$;


--
-- Name: fn_insert_new_hero_tr(integer, character varying, character varying, integer, integer, integer, integer, integer, integer, integer, character varying, integer, integer, integer, integer, integer, integer, timestamp with time zone, boolean, character varying, character varying, integer, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_insert_new_hero_tr(_userid integer, _datatype character varying, _details character varying, _herotype integer, _level integer, _bombpower integer, _bombrange integer, _stamina integer, _speed integer, _bombcount integer, _ability character varying, _skin integer, _color integer, _rarity integer, _bombskin integer, _energy integer, _stage integer, _timerest timestamp with time zone, _isactive boolean, _shield character varying, _abilitys character varying, _shieldlevel integer, _quantity integer) RETURNS TABLE(uid integer, bid integer, hero_tr_type character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
BEGIN
    -- has_hero counts the number of existing heroes of the same type
    RETURN QUERY
        WITH has_hero AS (
            SELECT COUNT(*) AS count
            FROM user_bomber ub
            WHERE ub.uid = _userId
              AND ub.charactor = _skin
              AND ub.color = _color
              AND ub.type = 2
              AND ub.hero_tr_type = 'HERO'
        )
        INSERT INTO user_bomber AS ub (
            uid,
            gen_id,
            "bomber_id",
            level,
            power,
            bomb_range,
            stamina,
            speed,
            bomb,
            ability,
            charactor,
            color,
            rare,
            bomb_skin,
            energy,
            stage,
            time_rest,
            active,
            shield,
            ability_shield,
            shield_level,
            "type",
            data_type,
            hero_tr_type
        )
        SELECT
            _userId,
            _details,
            NEXTVAL('user_bomber_id_non_fi_seq'),
            _level,
            _bombPower,
            _bombRange,
            _stamina,
            _speed,
            _bombCount,
            _ability,
            _skin,
            _color,
            _rarity,
            _bombSkin,
            _energy,
            _stage,
            _timeRest,
            CASE WHEN _isActive THEN 1 ELSE 0 END,
            _shield,
            _abilityS,
            _shieldLevel,
            _heroType,
            _dataType,
            CASE
                WHEN (SELECT count FROM has_hero) >= 1 THEN 'SOUL'
                WHEN (SELECT count FROM has_hero) = 0 AND GENERATE_SERIES.row_number = 1 THEN 'HERO'
                ELSE 'SOUL'
            END
        FROM (
            SELECT row_number() OVER () AS row_number
            FROM GENERATE_SERIES(1, _quantity)
        ) AS GENERATE_SERIES
        ON CONFLICT ("bomber_id", "type", "data_type")
            DO UPDATE SET
                "hasDelete" = 0,
                active = excluded.active,
                uid = excluded.uid,
                time_rest = excluded.time_rest,
                stage = _stage,
                hero_tr_type = excluded.hero_tr_type
        RETURNING ub.uid, ub.bomber_id::INT AS bid, ub.hero_tr_type;
END;
$$;


--
-- Name: fn_insert_new_server_hero(integer, integer, integer, integer, integer, integer, integer, character varying, integer, integer, integer, integer, character varying, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_insert_new_server_hero(_uid integer, _level integer, _power integer, _bomb_range integer, _stamina integer, _speed integer, _bomb integer, _ability character varying, _charactor integer, _color integer, _rare integer, _bomb_skin integer, _shield character varying, _network character varying, _ability_shield character varying, _active integer) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    _bomber_id INTEGER;
BEGIN
    _bomber_id := NEXTVAL('user_bomber_id_non_fi_seq');
    INSERT INTO user_bomber AS ub (uid,
                                   level,
                                   power,
                                   bomb_range,
                                   stamina,
                                   speed,
                                   bomb,
                                   ability,
                                   charactor,
                                   color,
                                   rare,
                                   bomb_skin,
                                   shield,
                                   data_type,
                                   ability_shield,
                                   energy,
                                   hero_tr_type,
                                   bomber_id,
                                   type,
                                   active)
    VALUES (_uid,
            _level,
            _power,
            _bomb_range,
            _stamina,
            _speed,
            _bomb,
            _ability,
            _charactor,
            _color,
            _rare,
            _bomb_skin,
            _shield,
            _network,
            _ability_shield,
            _stamina * 50,
            'HERO',
            _bomber_id,
            3,
            _active);

    RETURN _bomber_id;
END;

$$;


--
-- Name: fn_insert_new_server_hero(integer, integer, integer, integer, integer, integer, integer, character varying, integer, integer, integer, integer, character varying, character varying, character varying, integer, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_insert_new_server_hero(_uid integer, _level integer, _power integer, _bomb_range integer, _stamina integer, _speed integer, _bomb integer, _ability character varying, _charactor integer, _color integer, _rare integer, _bomb_skin integer, _shield character varying, _network character varying, _ability_shield character varying, _active integer, _type integer) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    _bomber_id INTEGER;
BEGIN
    IF (_network = 'SOL') THEN
    _bomber_id := NEXTVAL('user_bomber_id_sol_seq');
ELSIF (_network = 'BAS') THEN
    _bomber_id := NEXTVAL('user_bomber_id_bas_seq');
ELSIF (_network = 'VIC') THEN
    _bomber_id := NEXTVAL('user_bomber_id_vic_seq');
ELSIF (_network = 'RON') THEN
    _bomber_id := NEXTVAL('user_bomber_id_ron_seq');
ELSIF (_network = 'TON') THEN
    _bomber_id := NEXTVAL('user_bomber_id_non_fi_seq');
ELSE
    RAISE EXCEPTION 'Unsupported network type: %', _network;
END IF;

    INSERT INTO user_bomber AS ub (uid,
                                   level,
                                   power,
                                   bomb_range,
                                   stamina,
                                   speed,
                                   bomb,
                                   ability,
                                   charactor,
                                   color,
                                   rare,
                                   bomb_skin,
                                   shield,
                                   data_type,
                                   ability_shield,
                                   energy,
                                   hero_tr_type,
                                   bomber_id,
                                   type,
                                   active)
    VALUES (_uid,
            _level,
            _power,
            _bomb_range,
            _stamina,
            _speed,
            _bomb,
            _ability,
            _charactor,
            _color,
            _rare,
            _bomb_skin,
            _shield,
            _network,
            _ability_shield,
            _stamina * 50,
            'HERO',
            _bomber_id,
            _type,
            _active);

    RETURN _bomber_id;
END;
$$;


--
-- Name: fn_pvp_fixture_register_match(integer, integer, timestamp with time zone, timestamp with time zone, smallint, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_pvp_fixture_register_match(_uid1 integer, _uid2 integer, _from_time timestamp with time zone, _to_time timestamp with time zone, _hero_profile smallint, _fixed_zone character varying, _mode integer) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    _username1           varchar;
    _username2           varchar;
    _registered_match_id integer;
BEGIN

    IF _uid1 = _uid2 THEN
        RAISE EXCEPTION 'uid1 and uid2 must be different';
    END IF;

    SELECT user_name
    INTO _username1
    FROM "user"
    WHERE id_user = _uid1;

    IF _username1 IS NULL THEN
        RAISE EXCEPTION 'uid1 does not exist';
    END IF;

    SELECT user_name
    INTO _username2
    FROM "user"
    WHERE id_user = _uid2;

    IF _username2 IS NULL THEN
        RAISE EXCEPTION 'uid2 does not exist';
    END IF;

    INSERT INTO public.pvp_fixture_matches(player_1_uid, player_1_username, player_2_uid, player_2_username,
                                           hero_profile,
                                           fixed_zone, from_time, to_time, mode)
    VALUES (_uid1, _username1, _uid2, _username2, _hero_profile, _fixed_zone, _from_time, _to_time, _mode)
    RETURNING id INTO _registered_match_id;

    RETURN _registered_match_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END;
$$;


--
-- Name: fn_pvp_save_fixture_match_to_log(integer, integer, integer, timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_pvp_save_fixture_match_to_log(userid1 integer, userid2 integer, game_mode integer, from_time timestamp without time zone, to_time timestamp without time zone) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
    user1 varchar;
    user2 varchar;
BEGIN
    SELECT user_name INTO user1 FROM public.user WHERE id_user = userId1;
    SELECT user_name INTO user2 FROM public.user WHERE id_user = userId2;

    INSERT INTO public.pvp_tournament (participant_1, participant_2, mode, status, find_begin_time, find_end_time)
    VALUES (user1, user2, game_mode, 'PENDING', from_time, to_time)
    ON CONFLICT (participant_1, participant_2, mode, status)
    DO NOTHING;

END;
$$;


--
-- Name: fn_save_user_claim_reward_data(integer, character varying, character varying, double precision, double precision, boolean); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_save_user_claim_reward_data(_uid integer, _data_type character varying, _reward_type character varying, _min_claim double precision, _api_synced_value double precision, _claim_confirmed boolean) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    result             DOUBLE PRECISION;
    _claim_value       DOUBLE PRECISION;
    _claim_fee_percent DOUBLE PRECISION;
    _reward_gift       json;
BEGIN

    IF NOT _claim_confirmed
    THEN
        -- Fix trường hợp user đã từng claim trên blockchain rồi nhưng database chưa có ghi nhận
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

    SELECT COALESCE(JSON_AGG(ROW_TO_JSON(g)), '[]'::json)
    INTO _reward_gift
    FROM user_block_reward_gift AS g
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND data_type = _data_type
      AND status = 'PENDING';


    RETURN JSON_BUILD_OBJECT('value', result,
                             'received', _claim_value - (_claim_value * _claim_fee_percent / 100),
                             'gifts', _reward_gift
           )::text;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLSTATE,SQLERRM;
END;
$$;


--
-- Name: fn_select_or_insert_new_user_sol(character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_select_or_insert_new_user_sol(_wallet_address character varying) RETURNS TABLE(uid integer, user_name character varying, create_at timestamp with time zone)
    LANGUAGE plpgsql
    AS $$
BEGIN

    IF EXISTS(SELECT 1 FROM bomberland.users WHERE bomberland.users.username = _wallet_address) THEN
        UPDATE bomberland.users
        SET last_login = NOW()
        WHERE bomberland.users.username = _wallet_address
        RETURNING bomberland.users.id, bomberland.users.username, bomberland.users.create_at
            INTO uid, user_name, create_at;
        RETURN QUERY
            SELECT uid, user_name, create_at;
    ELSE
        INSERT INTO bomberland.users(username, address, create_at, update_at, last_login, type_account)
        VALUES (_wallet_address, _wallet_address, NOW(), NOW(), NOW(), 'SOL')
        RETURNING bomberland.users.id, bomberland.users.username, bomberland.users.create_at
            INTO uid, user_name, create_at;
        RETURN QUERY
            SELECT uid, user_name, create_at;
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END ;
$$;


--
-- Name: fn_sub_user_gem(integer, character varying, double precision, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_sub_user_gem(_uid integer, _datatype character varying, _amount double precision, _reason character varying) RETURNS text
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


--
-- Name: fn_sub_user_reward(integer, character varying, double precision, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_sub_user_reward(_uid integer, _networktype character varying, _amount double precision, _rewardtype character varying, _reason character varying) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    rewardAmount FLOAT;
    message      TEXT;
    rewardNew    FLOAT;
BEGIN
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

    INSERT INTO logs.logs_user_block_reward_template (uid, reward_type, network, values_old, values_changed, values_new,
                                                      reason)
    VALUES (_uid, _rewardtype, _networktype, rewardAmount, -_amount, rewardNew, _reason);

    RETURN json_build_object('subAmount', _amount)::text;
EXCEPTION
    WHEN SQLSTATE '45000' THEN
        GET STACKED DIAGNOSTICS message = MESSAGE_TEXT;
        RAISE EXCEPTION '%', message;
END;
$$;


--
-- Name: fn_sub_user_reward(integer, character varying, double precision, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_sub_user_reward(_uid integer, _networktype character varying, _amount double precision, _rewardtype character varying, _depositrewardtype character varying, _reason character varying) RETURNS text
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


--
-- Name: fn_tournament_create_match(character varying, character varying, integer, smallint, character varying, timestamp with time zone, timestamp with time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_tournament_create_match(_user_name_1 character varying, _user_name_2 character varying, _game_mode integer, _hero_profile smallint, _fixed_zone character varying, _from_time timestamp with time zone, _to_time timestamp with time zone) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    _uid_1               integer;
    _uid_2               integer;
    _registered_match_id integer;
BEGIN
    _user_name_1 := LOWER(_user_name_1);
    _user_name_2 := LOWER(_user_name_2);

    SELECT id_user INTO _uid_1 FROM "user" WHERE user_name = _user_name_1;
    SELECT id_user INTO _uid_2 FROM "user" WHERE user_name = _user_name_2;

    IF _uid_1 IS NULL THEN
        RAISE EXCEPTION 'User % not found', _user_name_1;
    END IF;

    IF _uid_2 IS NULL THEN
        RAISE EXCEPTION 'User % not found', _user_name_2;
    END IF;

    IF _uid_1 = _uid_2 THEN
        RAISE EXCEPTION 'User 1 and User 2 must be different';
    END IF;

    IF EXISTS(SELECT *
              FROM pvp_tournament
              WHERE mode = _game_mode
                AND status = 'PENDING'
                AND ((participant_1 = _user_name_1::varchar AND participant_2 = _user_name_2::varchar)
                  OR (participant_2 = _user_name_1::varchar AND participant_1 = _user_name_2::varchar))) THEN
        RAISE EXCEPTION 'Match already exists';
    ELSE

        INSERT INTO public.pvp_fixture_matches(player_1_uid, player_1_username, player_2_uid, player_2_username,
                                               hero_profile,
                                               fixed_zone, from_time, to_time, mode)
        VALUES (_uid_1, _user_name_1, _uid_2, _user_name_2, _hero_profile, _fixed_zone, _from_time, _to_time, _game_mode)
        RETURNING id INTO _registered_match_id;

        INSERT INTO public.pvp_tournament (id, participant_1, participant_2, mode, status, find_begin_time,
                                           find_end_time)
        VALUES (_registered_match_id, _user_name_1, _user_name_2, _game_mode, 'PENDING', _from_time, _to_time);
        RETURN _registered_match_id;
    END IF;

END;
$$;


--
-- Name: fn_tr_check_user_block_reward_values(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_tr_check_user_block_reward_values() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.values < 0 THEN
        RAISE EXCEPTION 'Column values cannot be negative';
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: fn_update_daily_check_in(character varying, integer, character varying, character varying, timestamp with time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_update_daily_check_in(_wallet character varying, _totalcheckin integer, _tx_hash character varying, _datatype character varying, _timestamp timestamp with time zone) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    _uid integer;
    _reward integer;
BEGIN
    -- 1. Get user ID from public.user
    SELECT id_user INTO _uid
    FROM public.user
    WHERE user_name = _wallet;

    IF _uid IS NULL THEN
        RETURN 'User not found';
    END IF;

    -- 2. Get reward amount from public.daily_check_in_config
    SELECT reward INTO _reward
    FROM public.config_daily_check_in
    WHERE day = _totalCheckIn AND data_type = _dataType;

    IF _reward IS NULL THEN
        RETURN 'Invalid check-in amount';
    END IF;

    -- 3. Insert into public.user_daily_check_in
    INSERT INTO public.user_daily_check_in(uid, total_check_in, reward, tx_hash, data_type, created_at)
    VALUES (_uid, _totalCheckIn, _reward, _tx_hash, _dataType, _timestamp);

    -- 4. Perform reward function
    PERFORM fn_add_user_reward(_uid, _dataType, _reward, 'COIN', 'Daily check-in VIC');
    RETURN _uid;
END;
$$;


--
-- Name: fn_update_user_bas_transaction(integer, double precision, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_update_user_bas_transaction(_id integer, _amount double precision, _tx_hash character varying, _token character varying, _sender character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_tx_hash VARCHAR;
    v_user_name VARCHAR;
    v_user_type VARCHAR;
    _uid integer;
    v_transaction_id integer;
BEGIN
    IF _id >= 0 THEN
        SELECT tx_hash INTO v_tx_hash
        FROM public.user_bas_transactions
        WHERE id = _id;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Transaction not found for user id: %', _id;
        END IF;

        IF v_tx_hash IS NULL THEN
            UPDATE public.user_bas_transactions
            SET amount = _amount,
                tx_hash = _tx_hash,
                token_name = _token,
                transaction_type = 'Deposit'
            WHERE id = _id;

            SELECT uid INTO _uid
            FROM public.user_bas_transactions
            WHERE id = _id;

            SELECT user_name, type INTO v_user_name, v_user_type
            FROM public.user
            WHERE id_user = _uid;

            IF v_user_type = 'FI' AND _token = 'BAS' THEN
                PERFORM fn_add_user_reward(_uid, _token, _amount, 'BAS_DEPOSITED', 'Deposit BAS');
            ELSE
                RAISE EXCEPTION 'User cheat deposit BAS id% ', _id;
            END IF;

            RETURN v_user_name;
        ELSE
            RAISE EXCEPTION 'Transaction hash BAS already exists for user id: %', _id;
        END IF;
    ELSE
        SELECT id_user INTO _uid
        FROM public.user
        WHERE user_name = _sender;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'User not found for sender: %', _sender;
        END IF;

        SELECT id, tx_hash INTO v_transaction_id, v_tx_hash
        FROM public.user_bas_transactions
        WHERE uid = _uid AND tx_hash IS NULL
        LIMIT 1;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Transaction not found for sender: %', _sender;
        END IF;

        IF v_tx_hash IS NULL THEN
            UPDATE public.user_bas_transactions
            SET amount = _amount,
                tx_hash = _tx_hash,
                token_name = _token,
                transaction_type = 'Deposit'
            WHERE id = v_transaction_id;

            SELECT user_name, type INTO v_user_name, v_user_type
            FROM public.user
            WHERE id_user = _uid;

            IF v_user_type = 'FI' AND _token = 'BAS' THEN
                PERFORM fn_add_user_reward(_uid, _token, _amount, 'BAS_DEPOSITED', 'Deposit BAS');
            ELSE
                RAISE EXCEPTION 'User cheat deposit BAS sender %', _sender;
            END IF;

            RETURN v_user_name;
        ELSE
            RAISE EXCEPTION 'Transaction has BAS already exists for sender: %', _sender;
        END IF;
    END IF;
END;
$$;


--
-- Name: fn_update_user_bcoin_sol_transaction(integer, double precision, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_update_user_bcoin_sol_transaction(_id integer, _amount double precision, _tx_hash character varying, _token character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_tx_hash VARCHAR;
    v_user_name VARCHAR;
    _uid integer;
BEGIN
    -- Check if the transaction hash already exists
    PERFORM 1
    FROM public.user_sol_transactions
    WHERE tx_hash = _tx_hash;

    -- If tx_hash is found, throw an exception
    IF FOUND THEN
        RAISE EXCEPTION 'Transaction hash already exists: %', _tx_hash;
    END IF;

    -- Check if the transaction exists for the given id and get the tx_hash
    SELECT tx_hash, uid INTO v_tx_hash, _uid
    FROM public.user_sol_transactions
    WHERE id = _id;

    -- If no row is found, throw an exception
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Transaction not found for user id: %', _id;
    END IF;

    -- If tx_hash is NULL, update row
    IF v_tx_hash IS NULL THEN
        UPDATE public.user_sol_transactions
        SET amount = _amount,
            tx_hash = _tx_hash,
            token_name = _token,
            transaction_type = 'Deposit'
        WHERE id = _id;

        PERFORM fn_add_user_reward(_uid, 'SOL', _amount, 'BCOIN_DEPOSITED', 'Deposite Bcoin');

        -- Select user_name from public.user
        SELECT user_name INTO v_user_name
        FROM public.user
        WHERE id_user = _uid;

        -- Return the user_name
        RETURN v_user_name;
    ELSE
        -- If tx_hash is not NULL, throw an exception
        RAISE EXCEPTION 'Transaction hash already exists for user id: %', _id;
    END IF;
END;
$$;


--
-- Name: fn_update_user_bcoin_transaction(integer, double precision, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_update_user_bcoin_transaction(_id integer, _amount double precision, _tx_hash character varying, _token character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_tx_hash VARCHAR;
    v_user_name VARCHAR;
    _uid integer;
BEGIN
    -- Check if the transaction hash already exists
    PERFORM 1
    FROM public.user_ton_transactions
    WHERE tx_hash = _tx_hash;

    -- If tx_hash is found, throw an exception
    IF FOUND THEN
        RAISE EXCEPTION 'Transaction hash already exists: %', _tx_hash;
    END IF;

    -- Check if the transaction exists for the given id and get the tx_hash
    SELECT tx_hash, uid INTO v_tx_hash, _uid
    FROM public.user_ton_transactions
    WHERE id = _id;

    -- If no row is found, throw an exception
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Transaction not found for user id: %', _id;
    END IF;

    -- If tx_hash is NULL, update row
    IF v_tx_hash IS NULL THEN
        UPDATE public.user_ton_transactions
        SET amount = _amount,
            tx_hash = _tx_hash,
            token_name = _token,
            transaction_type = 'Deposit'
        WHERE id = _id;

        PERFORM fn_add_user_reward(_uid, 'TON', _amount, 'BCOIN_DEPOSITED', 'Deposite Bcoin');

        -- Select user_name from public.user
        SELECT user_name INTO v_user_name
        FROM public.user
        WHERE id_user = _uid;

        -- Return the user_name
        RETURN v_user_name;
    ELSE
        -- If tx_hash is not NULL, throw an exception
        RAISE EXCEPTION 'Transaction hash already exists for user id: %', _id;
    END IF;
END;
$$;


--
-- Name: fn_update_user_ron_transaction(integer, double precision, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_update_user_ron_transaction(_id integer, _amount double precision, _tx_hash character varying, _token character varying, _sender character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_tx_hash VARCHAR;
    v_user_name VARCHAR;
    v_user_type VARCHAR;
    _uid integer;
    v_transaction_id integer;
BEGIN
    IF _id >= 0 THEN
        -- Normal process by _id
        SELECT tx_hash INTO v_tx_hash
        FROM public.user_ron_transactions
        WHERE id = _id;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Transaction not found for user id: %', _id;
        END IF;

        IF v_tx_hash IS NULL THEN
            UPDATE public.user_ron_transactions
            SET amount = _amount,
                tx_hash = _tx_hash,
                token_name = _token,
                transaction_type = 'Deposit'
            WHERE id = _id;

            SELECT uid INTO _uid
            FROM public.user_ron_transactions
            WHERE id = _id;

            SELECT user_name, type INTO v_user_name, v_user_type
            FROM public.user
            WHERE id_user = _uid;

            IF v_user_type = 'FI' AND _token = 'RON' THEN
                PERFORM fn_add_user_reward(_uid, _token, _amount, 'RON_DEPOSITED', 'Deposit RON');
            ELSE
                RAISE EXCEPTION 'User cheat deposit RON id% ', _id;
            END IF;

            RETURN v_user_name;
        ELSE
            RAISE EXCEPTION 'Transaction hash RON already exists for user id: %', _id;
        END IF;
    ELSE
        -- Process by _sender
        SELECT id_user INTO _uid
        FROM public.user
        WHERE user_name = _sender;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'User not found for sender: %', _sender;
        END IF;

        SELECT id, tx_hash INTO v_transaction_id, v_tx_hash
        FROM public.user_ron_transactions
        WHERE uid = _uid AND tx_hash IS NULL
        LIMIT 1;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Transaction not found for sender: %', _sender;
        END IF;

        IF v_tx_hash IS NULL THEN
            UPDATE public.user_ron_transactions
            SET amount = _amount,
                tx_hash = _tx_hash,
                token_name = _token,
                transaction_type = 'Deposit'
            WHERE id = v_transaction_id;

            SELECT user_name, type INTO v_user_name, v_user_type
            FROM public.user
            WHERE id_user = _uid;

            IF v_user_type = 'FI' AND _token = 'RON' THEN
                PERFORM fn_add_user_reward(_uid, _token, _amount, 'RON_DEPOSITED', 'Deposit RON');
            ELSE
                RAISE EXCEPTION 'User cheat deposit RON sender %', _sender;
            END IF;

            RETURN v_user_name;
        ELSE
            RAISE EXCEPTION 'Transaction has RON already exists for sender: %', _sender;
        END IF;
    END IF;
END;
$$;


--
-- Name: fn_update_user_sol_transaction(integer, double precision, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_update_user_sol_transaction(_id integer, _amount double precision, _tx_hash character varying, _token character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_tx_hash VARCHAR;
    v_user_name VARCHAR;
    _uid integer;
BEGIN
    -- Check if the transaction exists
    SELECT tx_hash INTO v_tx_hash
    FROM public.user_sol_transactions
    WHERE id = _id;

    -- If no row is found, throw an exception
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Transaction not found for user id: %', _id;
    END IF;

    -- If tx_hash is NULL, update row
    IF v_tx_hash IS NULL THEN
        UPDATE public.user_sol_transactions
        SET amount = _amount,
            tx_hash = _tx_hash,
            token_name = _token,
            transaction_type = 'Deposit'
        WHERE id = _id;

        SELECT uid INTO _uid
        FROM public.user_sol_transactions
        WHERE id = _id;

        PERFORM fn_add_user_reward(_uid, _token, _amount, 'SOL_DEPOSITED', 'Deposit Sol');

        -- Select user_name from public.user
        SELECT user_name INTO v_user_name
        FROM public.user
        WHERE id_user = _uid;

        -- Return the user_name
        RETURN v_user_name;
    ELSE
        -- If tx_hash is not NULL, throw an exception
        RAISE EXCEPTION 'Transaction hash Sol already exists for user id: %', _id;
    END IF;
END;
$$;


--
-- Name: fn_update_user_ton_transaction(integer, double precision, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_update_user_ton_transaction(_id integer, _amount double precision, _tx_hash character varying, _token character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_tx_hash VARCHAR;
    v_user_name VARCHAR;
    _uid integer;
BEGIN
    -- Check if the transaction exists
    SELECT tx_hash INTO v_tx_hash
    FROM public.user_ton_transactions
    WHERE id = _id;

    -- If no roq is found, throw an exception
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Transaction not found for user id: %', _id;
    END IF;

    -- If tx_hash is NULL, update row
    IF v_tx_hash IS NULL THEN
        UPDATE public.user_ton_transactions
        SET amount = _amount,
            tx_hash = _tx_hash,
            token_name = _token,
            transaction_type = 'Deposit'
        WHERE id = _id;

        SELECT uid INTO _uid
        FROM public.user_ton_transactions
        WHERE id = _id;

        PERFORM fn_add_user_reward(_uid, _token, _amount, 'TON_DEPOSITED', 'Deposite Ton');

        -- Select user_name from public.user
        SELECT user_name INTO v_user_name
        FROM public.user
        WHERE id_user = _uid;

        -- Return the user_name
        RETURN v_user_name;
    ELSE
        -- If tx_hash is not NULL, throw an exception
        RAISE EXCEPTION 'Transaction hash already exists for user id: %', _id;
    END IF;
END;
$$;


--
-- Name: fn_update_user_vic_transaction(integer, double precision, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_update_user_vic_transaction(_id integer, _amount double precision, _tx_hash character varying, _token character varying, _sender character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_tx_hash VARCHAR;
    v_user_name VARCHAR;
    v_user_type VARCHAR;
    _uid integer;
    v_transaction_id integer;
BEGIN
    IF _id >= 0 THEN
        SELECT tx_hash INTO v_tx_hash
        FROM public.user_vic_transactions
        WHERE id = _id;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Transaction not found for user id: %', _id;
        END IF;

        IF v_tx_hash IS NULL THEN
            UPDATE public.user_vic_transactions
            SET amount = _amount,
                tx_hash = _tx_hash,
                token_name = _token,
                transaction_type = 'Deposit'
            WHERE id = _id;

            SELECT uid INTO _uid
            FROM public.user_vic_transactions
            WHERE id = _id;

            SELECT user_name, type INTO v_user_name, v_user_type
            FROM public.user
            WHERE id_user = _uid;

            IF v_user_type = 'FI' AND _token = 'VIC' THEN
                PERFORM fn_add_user_reward(_uid, _token, _amount, 'VIC_DEPOSITED', 'Deposit VIC');
            ELSE
                RAISE EXCEPTION 'User cheat deposit VIC id% ', _id;
            END IF;

            RETURN v_user_name;
        ELSE
            RAISE EXCEPTION 'Transaction hash VIC already exists for user id: %', _id;
        END IF;
    ELSE
        SELECT id_user INTO _uid
        FROM public.user
        WHERE user_name = _sender;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'User not found for sender: %', _sender;
        END IF;

        SELECT id, tx_hash INTO v_transaction_id, v_tx_hash
        FROM public.user_vic_transactions
        WHERE uid = _uid AND tx_hash IS NULL
        LIMIT 1;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Transaction not found for sender: %', _sender;
        END IF;

        IF v_tx_hash IS NULL THEN
            UPDATE public.user_vic_transactions
            SET amount = _amount,
                tx_hash = _tx_hash,
                token_name = _token,
                transaction_type = 'Deposit'
            WHERE id = v_transaction_id;

            SELECT user_name, type INTO v_user_name, v_user_type
            FROM public.user
            WHERE id_user = _uid;

            IF v_user_type = 'FI' AND _token = 'VIC' THEN
                PERFORM fn_add_user_reward(_uid, _token, _amount, 'VIC_DEPOSITED', 'Deposit VIC');
            ELSE
                RAISE EXCEPTION 'User cheat deposit VIC sender %', _sender;
            END IF;

            RETURN v_user_name;
        ELSE
            RAISE EXCEPTION 'Transaction has VIC already exists for sender: %', _sender;
        END IF;
    END IF;
END;
$$;


--
-- Name: logs_th_mode_v2_create_partition_from_template(text); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.logs_th_mode_v2_create_partition_from_template(date_part text) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
    partition_name TEXT;
BEGIN
    partition_name := 'logs_th_mode_v2_' || date_part;

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS logs.%I (LIKE logs.logs_th_mode_v2_template INCLUDING ALL)',
                   partition_name
            );

    RETURN;
END;
$$;


--
-- Name: logs_th_mode_v2_insert_trigger(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.logs_th_mode_v2_insert_trigger() RETURNS trigger
    LANGUAGE plpgsql
    AS $_$
DECLARE
    partition_name TEXT;
    date_part      TEXT;
BEGIN
    date_part := TO_CHAR(NEW.timestamp, 'YYYY_MM_DD');
    partition_name := 'logs_th_mode_v2_' || date_part;

    PERFORM logs.logs_th_mode_v2_create_partition_from_template(date_part);

    EXECUTE FORMAT('
        INSERT INTO logs.%I (race_id, uid, hero_id, network_id, pool_index, reward_level, timestamp, reward_bcoin, reward_sen, reward_coin)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)',
                   partition_name
            ) USING NEW.race_id, NEW.uid, NEW.hero_id, NEW.network_id, NEW.pool_index, NEW.reward_level, NEW.timestamp, NEW.reward_bcoin, NEW.reward_sen, NEW.reward_coin;

    RETURN NULL;
END;
$_$;


--
-- Name: sp_buy_item_market_v3(integer, integer, integer, character varying, integer[], double precision, integer, character varying, integer, double precision); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_buy_item_market_v3(IN _buyer_uid integer, IN _item_id integer, IN _item_type integer, IN _reward_type character varying, IN _list_id integer[], IN _fee double precision, IN _expiration integer, IN _item_name character varying, IN _fixed_amount integer, IN _fixed_price double precision)
    LANGUAGE plpgsql
    AS $$
DECLARE
    i_id INTEGER;
    i_seller_uid INTEGER;
    i_price DOUBLE PRECISION;
    i_total_price DOUBLE PRECISION := 0;
BEGIN
    -- 1. Check if the fee is negative
    IF _fee < 0 THEN
        RAISE EXCEPTION 'Market fee cannot be negative';
    END IF;

    -- 2. Loop through each id in the list
    FOR i_id IN SELECT UNNEST(_list_id) LOOP
        -- Find the seller_uid and price for the current id
        SELECT seller_uid, price
        INTO i_seller_uid, i_price
        FROM user_market_selling_v3
        WHERE id = i_id
        FOR UPDATE;

        -- If no row is found, raise an exception
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Item with id % not found in the market', i_id;
        END IF;

        -- Delete the row from user_market_selling_v3
        DELETE FROM user_market_selling_v3
        WHERE id = i_id;

        -- 3. Log the activity
        INSERT INTO logs.log_user_activity_market_v3 (
            seller_uid, buyer_uid, item_id,
            item_name, type, id, quantity,
            price, reward_type, expiration
        )
        VALUES (
            i_seller_uid, _buyer_uid, _item_id,
            _item_name, _item_type, i_id, 1,
            i_price, _reward_type, _expiration
        );

        -- Calculate the total price for the current item
        i_total_price := i_total_price + i_price;

        -- 4. Add reward to the seller
        PERFORM fn_add_user_reward(i_seller_uid, 'TR', i_price * (1 - _fee), _reward_type, 'Sell Item Marketplace');
    END LOOP;

    -- 5. Update the item ownership
    CALL sp_update_item_market_v3(_buyer_uid, _list_id, _item_id, _item_type);

    -- 6. Log the fixed quantity purchase
    IF _fixed_amount > 0 THEN
        i_total_price := i_total_price + _fixed_amount * _fixed_price;

        INSERT INTO logs.log_user_activity_market_v3 (
            seller_uid, buyer_uid, item_id,
            item_name, type, id, quantity,
            price, reward_type, expiration
        )
        VALUES (
            -1, _buyer_uid, _item_id,
            _item_name, _item_type, 0, _fixed_amount,
            _fixed_price, _reward_type, _expiration
        );
    END IF;

    -- 7. Subtract gems from the buyer
    PERFORM fn_sub_user_gem(_buyer_uid, 'TR', i_total_price, 'Buy Item Marketplace');
END;
$$;


--
-- Name: sp_buy_item_marketplace(integer, integer, integer, double precision, integer, integer, character varying, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_buy_item_marketplace(IN _item_id integer, IN _item_type integer, IN _quantity integer, IN _unit_price double precision, IN _buyer_id integer, IN _reward_type integer, IN _name character varying, IN _expiration_after integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    feeSell         FLOAT DEFAULT 0.2;
    typeReward      VARCHAR(20) DEFAULT 'TR';
    _price          double precision DEFAULT _quantity * _unit_price;
    param           RECORD;
BEGIN
    --     SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

    SELECT value INTO feeSell FROM game_config WHERE key = 'fee_sell';

    CREATE TEMP TABLE _tbl_items_market ON COMMIT DROP AS
    WITH _data AS (SELECT *,
                          SUM(quantity) OVER (ORDER BY round_num) AS total_quantity
                   FROM (SELECT *,
                                ROW_NUMBER() OVER (ORDER BY modify_date) AS round_num
                         FROM user_marketplace
                         WHERE uid_creator != _buyer_id
                           AND item_id = _item_id
                           AND unit_price = _unit_price
                           AND quantity > 0
                           AND status = 0
                           AND expiration_after = _expiration_after) t),
         _max_round AS (SELECT round_num
                        FROM _data
                        WHERE total_quantity >= _quantity
                        LIMIT 1)
    SELECT *
    FROM _data
    WHERE round_num <= (SELECT round_num FROM _max_round);

    CREATE TEMP TABLE _tbl_ids ON COMMIT DROP AS
    SELECT tim.id
    FROM (SELECT item_id, JSON_ARRAY_ELEMENTS(list_id::json)::text::int AS id
          FROM _tbl_items_market
          LIMIT _quantity) AS tim
             INNER JOIN user_item_status AS uis ON tim.id = uis.id AND tim.item_id = uis.item_id
    WHERE uis.status = 2;

    IF (SELECT COUNT(*) FROM _tbl_ids) < _quantity THEN
        RAISE EXCEPTION 'The item is being traded by another transaction, not enough %', _quantity;
    END IF;

--     update item_status
    INSERT INTO user_item_status (uid, id, item_id, status)
    SELECT _buyer_id, t.id, _item_id, 1
    FROM (SELECT *
          FROM _tbl_ids) AS t
    ON CONFLICT (id, item_id)
        DO UPDATE SET uid    = EXCLUDED.uid,
                      status = excluded.status;

    IF (_item_type IN (1, 2, 3, 8)) THEN
--         update skin
        UPDATE user_skin
        SET uid              = _buyer_id,
            status           = 0
        WHERE id IN (SELECT * FROM _tbl_ids);
    ELSIF (_item_type = 4) THEN
--         update heroes
        WITH _current AS (SELECT charactor, color, COUNT(*) AS count
                          FROM user_bomber
                          WHERE uid = _buyer_id
                            AND type = 2
                            AND hero_tr_type = 'HERO'
                          GROUP BY charactor, color),
             _new AS (SELECT *
                      FROM user_bomber
                      WHERE type = 2
                        AND bomber_id IN (SELECT * FROM _tbl_ids)),
             _processed AS (SELECT n.bomber_id,
                                   n.charactor,
                                   n.color,
                                   COALESCE(c.count, 0)                                 AS currnet_count,
                                   ROW_NUMBER() OVER (PARTITION BY n.charactor,n.color) AS index
                            FROM _new AS n
                                     LEFT JOIN _current AS c
                                               ON n.charactor = c.charactor AND n.color = c.color)
        UPDATE user_bomber AS ub
        SET uid          = _buyer_id,
            hero_tr_type = CASE
                               WHEN p.currnet_count = 0 AND p.index = 1
                                   THEN 'HERO'
                               ELSE 'SOUL' END
        FROM _processed AS p
        WHERE ub.type = 2
          AND ub.bomber_id = p.bomber_id;
    ELSIF (_item_type = 5) THEN
--         update booster
        UPDATE user_booster
        SET uid = _buyer_id
        WHERE id IN (SELECT * FROM _tbl_ids);
    END IF;

--     update marketplace
    UPDATE user_marketplace AS um
    SET status      = CASE WHEN _quantity >= tim.total_quantity THEN 2 ELSE um.status END,
        quantity    =um.quantity - (CASE
                                        WHEN _quantity >= tim.total_quantity THEN um.quantity
                                        ELSE _quantity - tim.total_quantity + um.quantity
            END),
        price       = um.price - (CASE
                                      WHEN _quantity >= tim.total_quantity THEN um.quantity
                                      ELSE _quantity - tim.total_quantity + um.quantity
            END) * um.unit_price,
        list_id     = CASE
                          WHEN _quantity >= tim.total_quantity THEN '[]'
                          ELSE (SELECT JSON_AGG(t1.id)
                                FROM (SELECT JSON_ARRAY_ELEMENTS(um.list_id::json)::text::int AS id) AS t1
                                         LEFT JOIN _tbl_ids AS t2 ON t1.id = t2.id
                                WHERE t2 IS NULL)
            END,
        modify_date = NOW()
    FROM _tbl_items_market AS tim
    WHERE um.id = tim.id;

--     update user block reward user buy
    PERFORM fn_sub_user_gem(_buyer_id, 'TR', _price, 'Buy Item Marketplace');

--     #update user block reward user sell
    FOR param IN
        WITH _rewad_add AS (SELECT uid_creator,
                                   SUM((CASE
                                            WHEN _quantity >= total_quantity THEN quantity
                                            ELSE quantity - (total_quantity - _quantity)
                                       END) * _unit_price * (1 - feeSell)) AS value
                            FROM _tbl_items_market
                            GROUP BY uid_creator)
        SELECT *
        FROM _rewad_add
        LOOP
            PERFORM fn_add_user_reward(param.uid_creator, typeReward, param.value, 'GEM', 'Sell Item Marketplace');
        END LOOP;

--     #update activity buy
    INSERT INTO user_activity_marketplace(uid, instant_id, action, item_name, source, type, item_id, price, reward_type,
                                          unit_price)
    SELECT _buyer_id,
           _buyer_id,
           0,
           _name,
           uid_creator::text,
           _item_type,
           _item_id,
           (CASE
                WHEN _quantity >= total_quantity THEN quantity
                ELSE quantity - (total_quantity - _quantity)
               END) * _unit_price,
           _reward_type,
           _unit_price
    FROM _tbl_items_market;
--     #update activity sell
    INSERT INTO user_activity_marketplace(uid, instant_id, action, item_name, source, type, item_id, price, reward_type,
                                          unit_price)
    SELECT uid_creator,
           _buyer_id,
           1,
           _name,
           _buyer_id::text,
           _item_type,
           _item_id,
           (CASE
                WHEN _quantity >= total_quantity THEN quantity
                ELSE quantity - (total_quantity - _quantity)
               END) * _unit_price * (1 - feeSell),
           _reward_type,
           _unit_price
    FROM _tbl_items_market;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%, %',SQLSTATE, SQLERRM;
END
$$;


--
-- Name: sp_cancel_item_market_v3(integer, integer, integer, double precision); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_cancel_item_market_v3(IN _seller_uid integer, IN _item_id integer, IN _expiration integer, IN _price double precision)
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- lấy ra các id cần cancel
    WITH items_to_cancel AS (
        SELECT id
        FROM user_market_selling_v3
        WHERE seller_uid = _seller_uid
          AND item_id = _item_id
          AND expiration_after = _expiration
          AND price = _price
    )
    -- Update item status
    INSERT INTO user_item_status (uid, id, item_id, status)
    SELECT _seller_uid, id, _item_id, 0
    FROM items_to_cancel
    ON CONFLICT (id, item_id) DO UPDATE
    SET uid = EXCLUDED.uid,
        status = CASE
                    WHEN user_item_status.status = 2 THEN EXCLUDED.status
                    ELSE user_item_status.status
                 END
    WHERE user_item_status.status = 2;

    -- Xoá các item đã cancel
    DELETE FROM user_market_selling_v3
    WHERE seller_uid = _seller_uid
      AND item_id = _item_id
      AND expiration_after = _expiration
      AND price = _price;
END;
$$;


--
-- Name: sp_cancel_item_marketplace(integer, integer, integer, double precision, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_cancel_item_marketplace(IN _uid integer, IN _item_id integer, IN _quantity integer, IN _unit_price double precision, IN _expiration_after integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _row_updated INT DEFAULT 0;
BEGIN

    CREATE TEMP TABLE _tbl_items_market ON COMMIT DROP AS
    SELECT id,
           item_id,
           JSON_ARRAY_ELEMENTS_TEXT(list_id::json)::int AS instance_id
    FROM user_marketplace
    WHERE uid_creator = _uid
      AND item_id = _item_id
      AND unit_price = _unit_price
      AND status = 0
      AND quantity > 0
      AND expiration_after = _expiration_after;

    IF ((SELECT COUNT(*) FROM _tbl_items_market) != _quantity) THEN
        RAISE EXCEPTION 'The item is being traded by another transaction';
    END IF;

    UPDATE user_item_status
    SET status = 0
    WHERE id IN (SELECT instance_id FROM _tbl_items_market)
      AND item_id = _item_id
      AND status = 2;
    GET DIAGNOSTICS _row_updated = ROW_COUNT;

    IF (_row_updated != _quantity) THEN
        RAISE EXCEPTION 'The item is being traded by another transaction 1';
    END IF;

    UPDATE user_marketplace
    SET status      = 1,
        modify_date = NOW()
    WHERE uid_creator = _uid
      AND id IN (SELECT id FROM _tbl_items_market);

    GET DIAGNOSTICS _row_updated = ROW_COUNT;

    IF (_row_updated != (SELECT COUNT(DISTINCT id) FROM _tbl_items_market)) THEN
        RAISE EXCEPTION 'The item is being traded by another transaction %,%',_row_updated,(SELECT COUNT(DISTINCT id) FROM _tbl_items_market);
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%, %', SQLSTATE, SQLERRM;
END
$$;


--
-- Name: sp_check_user_achievement(integer, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_check_user_achievement(IN _uid integer, IN _current_reward_season integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _now                 TIMESTAMP DEFAULT NOW() AT TIME ZONE 'utc';
    _pvp_win_count       INT DEFAULT 0;
    _pvp_win_streaks     INT DEFAULT 0;
    _pvp_rank            INT DEFAULT 0;
    _max_adventure_level INT DEFAULT 0;
BEGIN

    CREATE TEMP TABLE _tbl_config_mission ON COMMIT DROP AS
    SELECT *
    FROM config_mission
    WHERE type = 'ACHIEVEMENT'
      AND active;


    SELECT COALESCE((misc_configs_json::jsonb ->> 'win_count')::INT, 0),
           COALESCE((misc_configs_json::jsonb ->> 'win_streaks')::INT, 0)
    INTO _pvp_win_count,
        _pvp_win_streaks
    FROM user_config
    WHERE uid = _uid;

    EXECUTE FORMAT('SELECT rank
               FROM %I
               WHERE uid = %s', FORMAT('user_pvp_rank_ss_%s', _current_reward_season), _uid)
        INTO _pvp_rank;

    SELECT COALESCE((max_stage - 1) * 5 + max_level, 0)
    INTO _max_adventure_level
    FROM user_adventure_mode
    WHERE uid = _uid;


    IF _pvp_win_count > 0 THEN
        -- xử lý trường hợp thắng pvp, action = 'WIN_PVP';
        INSERT INTO user_mission(uid,
                                 type,
                                 date,
                                 mission_code,
                                 number_mission,
                                 completed_mission,
                                 is_received_reward,
                                 modify_date,
                                 is_daily_mission)
        SELECT _uid,
               type,
               _now,
               code,
               number_mission,
               number_mission,
               0,
               _now,
               FALSE
        FROM _tbl_config_mission
        WHERE action = 'WIN_PVP'
        ON CONFLICT (uid,mission_code)
            DO UPDATE SET completed_mission = _pvp_win_count;
    END IF;

    IF _pvp_win_streaks > 0 THEN
        -- xử lý trường hợp win streaks pvp, action = WIN_STREAKS_PVP
        INSERT INTO user_mission(uid,
                                 type,
                                 date,
                                 mission_code,
                                 number_mission,
                                 completed_mission,
                                 is_received_reward,
                                 modify_date,
                                 is_daily_mission)
        SELECT _uid,
               type,
               _now,
               code,
               number_mission,
               number_mission,
               0,
               _now,
               FALSE
        FROM _tbl_config_mission
        WHERE action = 'WIN_STREAKS_PVP'
        ON CONFLICT (uid,mission_code)
            DO UPDATE SET completed_mission = CASE
                                                  WHEN user_mission.completed_mission < _pvp_win_streaks
                                                      THEN _pvp_win_streaks
                                                  ELSE user_mission.completed_mission END;
    END IF;

    IF _pvp_rank > 0 THEN
        -- xử lý trường hợp pvp rank kết thúc mùa, action = PVP_RANK_TARGET
        INSERT INTO user_mission(uid,
                                 type,
                                 date,
                                 mission_code,
                                 number_mission,
                                 completed_mission,
                                 is_received_reward,
                                 modify_date,
                                 is_daily_mission)
        SELECT _uid,
               type,
               _now,
               code,
               number_mission,
               _pvp_rank,
               0,
               _now,
               FALSE
        FROM _tbl_config_mission
        WHERE action = 'PVP_RANK_TARGET'
          AND _pvp_rank BETWEEN number_mission AND number_mission_max
        ON CONFLICT (uid,mission_code)
            DO UPDATE SET completed_mission = _pvp_rank;
    END IF;

    IF _max_adventure_level > 0 THEN
        -- xử lý trường hợp hoàn thành adventure map, action = COMPLETE_ADVENTURE
        INSERT INTO user_mission(uid,
                                 type,
                                 date,
                                 mission_code,
                                 number_mission,
                                 completed_mission,
                                 is_received_reward,
                                 modify_date,
                                 is_daily_mission)
        SELECT _uid,
               type,
               _now,
               code,
               1,
               1,
               0,
               _now,
               FALSE
        FROM _tbl_config_mission
        WHERE action = 'COMPLETE_ADVENTURE'
          AND number_mission <= _max_adventure_level
        ON CONFLICT (uid,mission_code) DO NOTHING;
    END IF;


EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%, %',SQLSTATE, SQLERRM;
END
$$;


--
-- Name: sp_claim_ton_tasks(integer, integer, double precision); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_claim_ton_tasks(IN _uid integer, IN _task_id integer, IN _reward double precision)
    LANGUAGE plpgsql
    AS $$
DECLARE
    isAlreadyClaimed      BOOLEAN;
    message      TEXT;
BEGIN
    SELECT count(*) > 0
    FROM "user_ton_completed_tasks"
    WHERE "uid" = _uid
      AND "task_id" = _task_id
      AND "claimed" = 1
    INTO isAlreadyClaimed;

    IF isAlreadyClaimed THEN
        RAISE EXCEPTION 'Already claim this task';
    END IF;

    UPDATE "user_ton_completed_tasks"
    SET "claimed" = 1,
        "claim_time" = CURRENT_TIMESTAMP
    WHERE "uid" = _uid
      AND "task_id" = _task_id;

    PERFORM fn_add_user_reward(_uid, 'TR', _reward, 'COIN', 'Claim task TON');

EXCEPTION
    WHEN SQLSTATE '45000' THEN
        GET STACKED DIAGNOSTICS message = MESSAGE_TEXT;
        RAISE EXCEPTION '%', message;
END;
$$;


--
-- Name: sp_edit_item_market_v3(integer, integer, double precision, double precision, integer[], integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_edit_item_market_v3(IN _seller_uid integer, IN _item_id integer, IN _new_price double precision, IN _old_price double precision, IN _list_id integer[], IN _expiration integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    i_type integer;
    i_reward_type varchar;
BEGIN
    IF _new_price <= 0 THEN
        RAISE EXCEPTION 'Price must be greater than 0';
    END IF;

    -- Get type and reward_type from any old row (they should be the same for all)
    SELECT type, reward_type INTO i_type, i_reward_type
    FROM user_market_selling_v3
    WHERE seller_uid = _seller_uid
      AND item_id = _item_id
      AND expiration_after = _expiration
      AND price = _old_price
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No matching record found in user_market_selling_v3 for seller_uid %, item_id %, expiration %, price %', _seller_uid, _item_id, _expiration, _old_price;
    END IF;

    -- Update all items with old price to status = 0 before delete
    UPDATE user_item_status
    SET status = 0
    WHERE id IN (
        SELECT id FROM user_market_selling_v3
        WHERE seller_uid = _seller_uid
          AND item_id = _item_id
          AND expiration_after = _expiration
          AND price = _old_price
    )
    AND item_id = _item_id
    AND status = 2;

    -- Delete all old rows for this seller, item, expiration, old price
    DELETE FROM user_market_selling_v3
    WHERE seller_uid = _seller_uid
      AND item_id = _item_id
      AND expiration_after = _expiration
      AND price = _old_price;

    -- Insert into user_market_selling_v3
    INSERT INTO user_market_selling_v3 (
        seller_uid, item_id, type, price, reward_type, id, expiration_after, modify_date
    )
    SELECT
        _seller_uid, _item_id, i_type, _new_price, i_reward_type, unnest(_list_id), _expiration, now()
    ON CONFLICT (id) DO NOTHING;

    -- Insert into user_item_status
    INSERT INTO user_item_status (uid, id, item_id, status)
    SELECT
        _seller_uid, unnest(_list_id), _item_id, 2
    ON CONFLICT (id, item_id) DO UPDATE
    SET uid = EXCLUDED.uid,
        status = CASE
                   WHEN user_item_status.status = 0 THEN EXCLUDED.status
                   ELSE user_item_status.status
                 END
    WHERE user_item_status.status = 0;
END;
$$;


--
-- Name: sp_edit_item_marketplace(integer, integer, integer, double precision, integer, double precision, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_edit_item_marketplace(IN _uid integer, IN _item_id integer, IN _old_quantity integer, IN _old_unit_price double precision, IN _new_quantity integer, IN _new_unit_price double precision, IN _expiration_after integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _row_updated INT DEFAULT 0;
BEGIN

    IF _new_quantity <= 0 THEN
        RAISE EXCEPTION 'Quantity must be greater than 0';
    END IF;

    IF _new_unit_price <= 0 THEN
        RAISE EXCEPTION 'Price must be greater than 0';
    END IF;

    CREATE TEMP TABLE _tbl_items_market_item ON COMMIT DROP AS
    SELECT id,
           item_id,
           list_id
    FROM user_marketplace
    WHERE uid_creator = _uid
      AND item_id = _item_id
      AND unit_price = _old_unit_price
      AND status = 0
      AND quantity > 0
      AND expiration_after = _expiration_after;

    CREATE TEMP TABLE _tbl_items_market ON COMMIT DROP AS
    SELECT item_id,
           JSON_ARRAY_ELEMENTS_TEXT(list_id::json)::int AS instance_id
    FROM _tbl_items_market_item;

    IF ((SELECT COUNT(*) FROM _tbl_items_market) != _old_quantity) THEN
        RAISE EXCEPTION 'The item is being traded by another transaction 1';
    END IF;

    CREATE TEMP TABLE _tbl_items_new ON COMMIT DROP AS
    SELECT item_id,
           instance_id
    FROM _tbl_items_market
    LIMIT _new_quantity;

    CREATE TEMP TABLE _tbl_items_remain ON COMMIT DROP AS
    SELECT item_id,
           instance_id
    FROM _tbl_items_market
    WHERE instance_id NOT IN (SELECT instance_id FROM _tbl_items_new);

    --  cơ chế edit: xoá toàn bộ row đang tồn tại, gom về 1 row duy nhất, cập nhật lại giá
--  và số lượng

-- xoá toàn bộ dòng hiện tại
    UPDATE user_marketplace
    SET status      = 1,
        modify_date = NOW()
    WHERE uid_creator = _uid
      AND item_id = _item_id
      AND unit_price = _old_unit_price
      AND status = 0
      AND quantity > 0
      AND expiration_after = _expiration_after;

    GET DIAGNOSTICS _row_updated = ROW_COUNT;

    IF (_row_updated != (SELECT COUNT(*) FROM _tbl_items_market_item)) THEN
        RAISE EXCEPTION 'The item is being traded by another transaction 2';
    END IF;

--  gom lại thành 1 dòng mới
    UPDATE user_marketplace
    SET list_id    = (SELECT JSON_AGG(instance_id) FROM _tbl_items_new),
        price      = _new_unit_price * (SELECT COUNT(*) FROM _tbl_items_new),
        unit_price = _new_unit_price,
        quantity   = (SELECT COUNT(*) FROM _tbl_items_new),
        status     = 0,
        expiration_after = _expiration_after
    WHERE id = (SELECT id FROM _tbl_items_market_item LIMIT 1)
      AND uid_creator = _uid;

--  set lại status normal  cho item dư
    UPDATE user_item_status
    SET status = 0
    WHERE id IN (SELECT instance_id FROM _tbl_items_remain)
      AND item_id = _item_id
      AND status = 2;
    GET DIAGNOSTICS _row_updated = ROW_COUNT;

    IF (_row_updated != (SELECT COUNT(*) FROM _tbl_items_remain)) THEN
        RAISE EXCEPTION 'The item is being traded by another transaction 3';
    END IF;


EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%, %',SQLSTATE, SQLERRM;
END
$$;


--
-- Name: sp_fix_user_claim_reward_data(integer, character varying, character varying, double precision); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_fix_user_claim_reward_data(IN _uid integer, IN _data_type character varying, IN _reward_type character varying, IN _api_synced_value double precision)
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

    -- Sở dĩ phải làm như vầy là vì blockchain lưu lại lịch sử claim thông qua _api_synced_value
    -- Nhưng database này chưa hề có dữ liệu ở cột claim_synced cho nên sẽ gây ra lỗi

    UPDATE user_block_reward
    SET claim_synced = _api_synced_value,
        modify_date  = now()
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLSTATE,SQLERRM;

END ;
$$;


--
-- Name: sp_insert_new_ton_hero(integer, integer, integer, integer, integer, integer, integer, character varying, integer, integer, integer, integer, character varying, character varying, character varying, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_insert_new_ton_hero(IN _uid integer, IN _level integer, IN _power integer, IN _bomb_range integer, IN _stamina integer, IN _speed integer, IN _bomb integer, IN _ability character varying, IN _charactor integer, IN _color integer, IN _rare integer, IN _bomb_skin integer, IN _shield character varying, IN _network character varying, IN _ability_shield character varying, IN _active integer)
    LANGUAGE plpgsql
    AS $$
BEGIN
    INSERT INTO user_bomber AS ub (uid,
                                   level,
                                   power,
                                   bomb_range,
                                   stamina,
                                   speed,
                                   bomb,
                                   ability,
                                   charactor,
                                   color,
                                   rare,
                                   bomb_skin,
                                   shield,
                                   data_type,
                                   ability_shield,
                                   energy,
                                   hero_tr_type,
                                   bomber_id,
                                   type,
                                   active)
    VALUES (_uid,
            _level,
            _power,
            _bomb_range,
            _stamina,
            _speed,
            _bomb,
            _ability,
            _charactor,
            _color,
            _rare,
            _bomb_skin,
            _shield,
            _network,
            _ability_shield,
            _stamina * 50,
            'HERO',
            NEXTVAL('user_bomber_id_non_fi_seq'),
            3,
            _active);
END;
$$;


--
-- Name: sp_modify_rock_from_user_wallet(character varying, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_modify_rock_from_user_wallet(IN _wallet character varying, IN _amount integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _uid INT;
    _current_value NUMERIC;
BEGIN
    -- Get user id
    SELECT id_user INTO _uid FROM public.user WHERE user_name = _wallet;

    -- If user exists
    IF _uid IS NOT NULL THEN
        -- Get current rock value
        SELECT COALESCE((SELECT values FROM user_block_reward WHERE uid = _uid AND reward_type = 'ROCK'), 0) INTO _current_value;
        -- If the new value is not negative
        IF _current_value + _amount >= 0 THEN
            -- Insert new rock reward or update if it already exists
            INSERT INTO user_block_reward (uid, type, reward_type, values, total_values, modify_date)
            VALUES (_uid, 'TR', 'ROCK', GREATEST(_amount, 0), GREATEST(_amount, 0), CURRENT_TIMESTAMP)
            ON CONFLICT (uid, type, reward_type) DO UPDATE
            SET values = user_block_reward.values + _amount,
                total_values = CASE WHEN _amount > 0 THEN user_block_reward.total_values + _amount ELSE user_block_reward.total_values END,
                modify_date = CURRENT_TIMESTAMP;
        ELSE
            -- Throw exception if new value is negative
            RAISE EXCEPTION 'Not enough rock to perform this operation';
        END IF;

        -- Lưu lại lịch sử
        INSERT INTO bombcrypto2.logs.logs_user_buy_rock_pack (uid, time_stamp, package_name, rock_amount, price, token_name, network)
        VALUES (_uid, CURRENT_TIMESTAMP, 'ADD_DEFAULT', _amount, 0, 'ROCK', 'TR');
    END IF;
END;
$$;


--
-- Name: sp_pvp_tournament_finish(character varying, character varying, integer, integer, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_pvp_tournament_finish(IN _user_name_1 character varying, IN _user_name_2 character varying, IN _mode integer, IN _score_1 integer, IN _score_2 integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _match_id      integer;
    _participant_1 varchar;
    _participant_2 varchar;
    _user_1_score  integer;
    _user_2_score  integer;
BEGIN
    SELECT id, participant_1, participant_2
    INTO _match_id, _participant_1, _participant_2
    FROM pvp_tournament
    WHERE mode = _mode
        AND (participant_1 = _user_name_1 AND participant_2 = _user_name_2)
       OR (participant_1 = _user_name_2 AND participant_2 = _user_name_1)
        AND status = 'PENDING'
    LIMIT 1;

    IF _match_id IS NULL THEN
        RAISE EXCEPTION 'Match not found';
    END IF;

    IF _participant_1 = _user_name_1 THEN
        _user_1_score = _score_1;
        _user_2_score = _score_2;
    ELSE
        _user_1_score = _score_2;
        _user_2_score = _score_1;
    END IF;

    UPDATE pvp_tournament
    SET finish_time=NOW(),
        status='COMPLETED',
        user_1_score=_user_1_score,
        user_2_score=_user_2_score
    WHERE id = _match_id::integer;

END;
$$;


--
-- Name: sp_repair_hero_shield(character varying, integer, integer, double precision, character varying, character varying, integer, text); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_repair_hero_shield(IN _data_type character varying, IN _uid integer, IN _hero_id integer, IN _price double precision, IN _reward_type character varying, IN _deposit_reward_type character varying, IN _remain_shield integer, IN _new_shield text)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _tmp json;
BEGIN

    SELECT fn_sub_user_reward(_uid, _data_type, _price, _reward_type, _deposit_reward_type)
    INTO _tmp;

    INSERT INTO log_repair_shield(username,
                                  repair_time,
                                  bomber_id,
                                  remain_shield,
                                  type)
    VALUES ((SELECT user_name FROM "user" WHERE id_user = _uid),
            NOW() AT TIME ZONE 'utc',
            _hero_id,
            _remain_shield,
            _data_type);

    UPDATE user_bomber
    SET shield = _new_shield
    WHERE bomber_id = _hero_id
      AND uid = _uid;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END;
$$;


--
-- Name: sp_repair_hero_shield_with_rock(integer, character varying, integer, double precision, character varying, integer, text); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_repair_hero_shield_with_rock(IN _uid integer, IN _network character varying, IN _hero_id integer, IN _price double precision, IN _reward_type character varying, IN _remain_shield integer, IN _new_shield text)
    LANGUAGE plpgsql
    AS $$
DECLARE
    rewardAmount FLOAT;
    message TEXT;
BEGIN
    SELECT COALESCE(SUM(CASE WHEN reward_type = _reward_type THEN "values" ELSE 0 END), 0)
    INTO rewardAmount
    FROM user_block_reward
    WHERE uid = _uid
      AND reward_type = _reward_type;



    IF _price > rewardAmount THEN
        message := '1019,Not enough ' || _price || ' ' || _reward_type;
        RAISE EXCEPTION '%', message;
    END IF;

    UPDATE user_block_reward
    SET "values"    = "values" - _price,
        modify_date = CURRENT_TIMESTAMP
    WHERE uid = _uid
      AND reward_type = _reward_type;

    INSERT INTO log_repair_shield(username,
                                  repair_time,
                                  bomber_id,
                                  remain_shield,
                                  type,
                                  uid,
                                  reward_type)
    VALUES ((SELECT user_name FROM "user" WHERE id_user = _uid),
            NOW() AT TIME ZONE 'utc',
            _hero_id,
            _remain_shield,
            _network,
            _uid,
            _reward_type);

    UPDATE user_bomber
    SET shield = _new_shield
    WHERE bomber_id = _hero_id
      AND uid = _uid;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END;
$$;


--
-- Name: sp_save_simulate_data(integer, integer, integer, integer, integer, double precision, integer, integer, integer, text, text); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_save_simulate_data(IN _uid integer, IN _hero_id integer, IN _speed integer, IN _bomb integer, IN _total_minutes integer, IN _bomb_per_minutes double precision, IN _shield_lost_per_minutes integer, IN _total_bomb integer, IN _total_shield_lost integer, IN _bomb_to_stop text, IN _shield_lost_to_stop text)
    LANGUAGE plpgsql
    AS $$
DECLARE
    existing_record RECORD;
BEGIN
    SELECT * INTO existing_record 
    FROM logs.logs_simulate_th_mode_v2 
    WHERE uid = _uid AND hero_id = _hero_id AND speed = _speed AND bomb = _bomb;

    IF existing_record IS NULL THEN
        INSERT INTO logs.logs_simulate_th_mode_v2 (
            uid, 
            hero_id, 
            speed, 
            bomb, 
            total_minutes, 
            bomb_per_minutes, 
            shield_lost_per_minutes, 
            total_bomb, 
            total_shield_lost, 
            bomb_to_stop, 
            shield_lost_to_stop
        ) VALUES (
            _uid, 
            _hero_id, 
            _speed, 
            _bomb, 
            _total_minutes, 
            _bomb_per_minutes, 
            _shield_lost_per_minutes, 
            _total_bomb, 
            _total_shield_lost, 
            _bomb_to_stop, 
            _shield_lost_to_stop
        );
    ELSE
        IF existing_record.total_minutes >= _total_minutes AND _bomb_to_stop != '' AND _shield_lost_to_stop != '' THEN
            RETURN;
        ELSE
            UPDATE logs.logs_simulate_th_mode_v2 
            SET
                total_minutes = _total_minutes, 
                bomb_per_minutes = _bomb_per_minutes, 
                shield_lost_per_minutes = _shield_lost_per_minutes, 
                total_bomb = _total_bomb, 
                total_shield_lost = _total_shield_lost, 
                bomb_to_stop = COALESCE(NULLIF(_bomb_to_stop, ''), existing_record.bomb_to_stop), 
                shield_lost_to_stop = COALESCE(NULLIF(_shield_lost_to_stop, ''), existing_record.shield_lost_to_stop)
            WHERE uid = _uid AND hero_id = _hero_id AND speed = _speed AND bomb = _bomb;
        END IF;
    END IF;
END;
$$;


--
-- Name: sp_save_simulate_data(integer, integer, integer, integer, integer, integer, double precision, integer, integer, character varying, character varying); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_save_simulate_data(IN _uid integer, IN _hero_id integer, IN _speed integer, IN _bomb integer, IN _total_minutes integer, IN _bomb_per_minutes integer, IN _shield_lost_per_minutes double precision, IN _total_bomb integer, IN _total_shield_lost integer, IN _bomb_to_stop character varying, IN _shield_lost_to_stop character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    existing_record RECORD;
BEGIN
    SELECT * INTO existing_record
    FROM logs.logs_simulate_th_mode_v2
    WHERE uid = _uid AND hero_id = _hero_id AND speed = _speed AND bomb = _bomb;

    IF existing_record IS NULL THEN
        INSERT INTO logs.logs_simulate_th_mode_v2 (
            uid,
            hero_id,
            speed,
            bomb,
            total_minutes,
            bomb_per_minutes,
            shield_lost_per_minutes,
            total_bomb,
            total_shield_lost,
            bomb_to_stop,
            shield_lost_to_stop
        ) VALUES (
            _uid,
            _hero_id,
            _speed,
            _bomb,
            _total_minutes,
            _bomb_per_minutes,
            _shield_lost_per_minutes,
            _total_bomb,
            _total_shield_lost,
            _bomb_to_stop,
            _shield_lost_to_stop
        );
    ELSE
        IF existing_record.total_minutes >= _total_minutes AND _bomb_to_stop != '' AND _shield_lost_to_stop != '' THEN
            RETURN;
        ELSE
            UPDATE logs.logs_simulate_th_mode_v2
            SET
                total_minutes = _total_minutes,
                bomb_per_minutes = _bomb_per_minutes,
                shield_lost_per_minutes = _shield_lost_per_minutes,
                total_bomb = _total_bomb,
                total_shield_lost = _total_shield_lost,
                bomb_to_stop = COALESCE(NULLIF(_bomb_to_stop, ''), existing_record.bomb_to_stop),
                shield_lost_to_stop = COALESCE(NULLIF(_shield_lost_to_stop, ''), existing_record.shield_lost_to_stop)
            WHERE uid = _uid AND hero_id = _hero_id AND speed = _speed AND bomb = _bomb;
        END IF;
    END IF;
END;
$$;


--
-- Name: sp_save_user_claim_reward_data(integer, character varying, character varying, double precision, double precision, double precision); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_save_user_claim_reward_data(IN _uid integer, IN _data_type character varying, IN _reward_type character varying, IN _min_claim double precision, IN _claim_fee_percent double precision, IN _api_synced_value double precision)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _current_value           FLOAT;
    _pending_value           DECIMAL;
    _claim_value             DECIMAL;
    _synced_value            DECIMAL;
    _last_time_claim_success TIMESTAMP;
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


    IF _claim_value < _min_claim
    THEN
        RAISE EXCEPTION '%,%','Not enough reward to claim',1019;
    END IF;


    IF ROUND(_synced_value) >= ROUND(_api_synced_value)
    THEN
        _current_value = 0;
        _pending_value = _claim_value;

-- set status là pending(dã claim mà chưa thành công
        UPDATE user_block_reward_gift
        SET status = 'PENDING'
        WHERE uid = _uid
          AND status = 'WAITING';
    ELSE
        _synced_value = _api_synced_value;
        _pending_value = 0;
        _current_value = 0;
        _last_time_claim_success = CURRENT_TIMESTAMP;

--         Set trạng thái claimed (đã claim thành công)
        UPDATE user_block_reward_gift
        SET status = 'CLAIMED'
        WHERE uid = _uid
          AND status = 'PENDING';

        INSERT INTO log_user_claim_reward(uid, claim_date, value, reward_type, data_type)
        VALUES (_uid, CURRENT_TIMESTAMP, _claim_value - (_claim_value * _claim_fee_percent / 100), _reward_type,
                _data_type);
    END IF;

--     trừ reward
    UPDATE user_block_reward
    SET values                  = _current_value,
        claim_pending           = _pending_value,
        claim_synced            = _synced_value,
        modify_date             = CURRENT_TIMESTAMP,
        last_time_claim_success = COALESCE(_last_time_claim_success, user_block_reward.last_time_claim_success)
    WHERE uid = _uid
      AND reward_type = _reward_type
      AND type = _data_type;
--

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLSTATE,SQLERRM;

END ;
$$;


--
-- Name: sp_sell_item_market_v3(integer, integer, integer, double precision, character varying, integer[], integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_sell_item_market_v3(IN _seller_uid integer, IN _type integer, IN _item_id integer, IN _price double precision, IN _reward_type character varying, IN _list_id integer[], IN _expiration integer)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF _price <= 0 THEN
        RAISE EXCEPTION 'Price must be greater than 0';
    END IF;

    -- Các id cần update
    WITH item_ids AS (
        SELECT unnest(_list_id) AS id
    )
    -- Update trạng thái thành đang bán
    INSERT INTO user_item_status (uid, id, item_id, status)
    SELECT _seller_uid, id, _item_id, 2
    FROM item_ids
    ON CONFLICT (id, item_id) DO UPDATE
    SET uid = EXCLUDED.uid,
        status = CASE
                    WHEN user_item_status.status = 0 THEN EXCLUDED.status
                    ELSE user_item_status.status
                 END
    WHERE user_item_status.status = 0;

    -- Cập nhật lên market
    INSERT INTO user_market_selling_v3 (
        seller_uid, type, item_id, price, reward_type, id, expiration_after, modify_date
    )
    SELECT
        _seller_uid, _type, _item_id, _price, _reward_type, id, _expiration, now()
    FROM (SELECT unnest(_list_id) AS id) AS ids
    ON CONFLICT (id) DO NOTHING;
END;
$$;


--
-- Name: sp_sell_item_marketplace(json, integer, integer, integer, double precision, integer, integer, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_sell_item_marketplace(IN _listid json, IN _type integer, IN _itemid integer, IN _quantity integer, IN _price double precision, IN _uid integer, IN _rewardtype integer, IN _expiration_after integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    countUpdate INT DEFAULT 0;
BEGIN

    IF _quantity <= 0 THEN
        RAISE EXCEPTION 'Quantity must be greater than 0';
    END IF;

    IF _price <= 0 THEN
        RAISE EXCEPTION 'Price must be greater than 0';
    END IF;

    PERFORM *
    FROM user_item_status
    WHERE id IN (SELECT t::text::int4
                 FROM JSON_ARRAY_ELEMENTS(_listId) AS t)
      AND item_id = _itemId FOR UPDATE;
--      update user_item_status
    WITH _count
             AS (INSERT INTO user_item_status (uid, id, item_id, status)
            SELECT _uid, t::text::int4, _itemId, 2
            FROM JSON_ARRAY_ELEMENTS(_listId) AS t
            ON CONFLICT (id, item_id) DO UPDATE SET uid = EXCLUDED.uid,
                status = CASE
                             WHEN user_item_status.status = 0 THEN EXCLUDED.status
                             WHEN user_item_status.status = 1 THEN 1000
                             ELSE 999 END
            RETURNING status)
    SELECT MAX(status)
    INTO countUpdate
    FROM _count;
    IF (countUpdate = 999) THEN
        RAISE EXCEPTION 'The item is being traded by another transaction';
    ELSIF (countUpdate = 1000) THEN
        RAISE EXCEPTION 'Can not sell item locked';
    END IF;

--     update marketplace
    INSERT INTO user_marketplace (type,
                                  item_id,
                                  list_id,
                                  price,
                                  unit_price,
                                  quantity,
                                  reward_type,
                                  status,
                                  uid_creator,
                                  expiration_after)
    VALUES (_type,
            _itemId,
            _listId,
            _price,
            _price / _quantity,
            _quantity,
            _rewardType,
            0,
            _uid,
            CASE WHEN _expiration_after IS NULL THEN -1 ELSE _expiration_after END);
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%, %',SQLSTATE, SQLERRM;
END
$$;


--
-- Name: sp_setup_next_pvp_season(); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_setup_next_pvp_season()
    LANGUAGE plpgsql
    AS $_$
DECLARE
    _current_season       INT;
    _is_calculated_reward bool;
    _next_season          INT;
    _current_season_ended BOOLEAN;
BEGIN

    SELECT ps.id             AS current_season,
           ps.is_calculated_reward,
           ns.id             AS next_season,
           CASE
               WHEN NOW() AT TIME ZONE 'utc' BETWEEN ps.start_date AND ps.end_date
                   THEN FALSE
               ELSE TRUE END AS current_season_ended
    INTO _current_season,
        _is_calculated_reward,
        _next_season,
        _current_season_ended
    FROM config_ranking_season AS ps
             LEFT JOIN config_ranking_season AS ns ON ps.id = ns.id - 1
    WHERE NOW() AT TIME ZONE 'utc' BETWEEN ps.start_date AND ps.end_date
       OR NOW() AT TIME ZONE 'utc' BETWEEN ps.end_date AND ns.start_date
       OR NOW() AT TIME ZONE 'utc' > ps.end_date
    ORDER BY ps.end_date DESC
    LIMIT 1;

    -- tạo bảng ranking cho mùa hiện tại nếu chưa có
    EXECUTE FORMAT('CREATE SEQUENCE if NOT EXISTS user_pvp_rank_ss_%1$s_seq START 1;', _current_season);
    EXECUTE FORMAT('CREATE TABLE IF NOT EXISTS user_pvp_rank_ss_%1$s
                   (
                        LIKE user_pvp_rank_template INCLUDING ALL
                   )', _current_season);

-- calculate reward khi kết thúc mùa
    IF _current_season_ended AND _is_calculated_reward = FALSE THEN
        EXECUTE FORMAT('CREATE TABLE IF NOT EXISTS user_pvp_rank_reward_ss_%1$s
                   (
                        LIKE user_pvp_rank_reward_template INCLUDING ALL
                   )', _current_season);
        EXECUTE FORMAT('TRUNCATE TABLE user_pvp_rank_reward_ss_%1$s;', _current_season);

        EXECUTE FORMAT('INSERT INTO user_pvp_rank_reward_ss_%1$s (uid,
                                                              rank,
                                                              reward,
                                                              total_match)
                                      SELECT r.uid,
                                             r.rank,
                                             bsr.reward,
                                             r.total_match
                                      FROM user_pvp_rank_ss_%2$s AS r
                                               LEFT JOIN config_pvp_ranking_reward bsr
                                                         ON r.rank >= bsr.rank_min AND r.rank <= bsr.rank_max AND r.total_match >= 30
                                      WHERE r.rank IS NOT NULL', _current_season, _current_season
                );

        UPDATE config_ranking_season
        SET is_calculated_reward = TRUE
        WHERE id = _current_season;

    END IF;

--     nếu có mùa tiếp theo thì tạo mùa tiếp theo
    IF _next_season IS NOT NULL AND _current_season_ended THEN
        EXECUTE FORMAT('CREATE SEQUENCE if NOT EXISTS user_pvp_rank_ss_%1$s_seq START 1;', _next_season);
        EXECUTE FORMAT('CREATE TABLE IF NOT EXISTS user_pvp_rank_ss_%1$s
                   (
                        LIKE user_pvp_rank_template INCLUDING ALL
                   )', _next_season);
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END
$_$;


--
-- Name: sp_sync_user_deposit(integer, character varying, double precision, double precision); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_sync_user_deposit(IN _uid integer, IN _type character varying, IN _total_bcoin_deposited double precision, IN _total_sen_deposited double precision)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _timeStamp                TIMESTAMP DEFAULT NOW();
    _current_bcoin_deposited  FLOAT := 0;
    _current_sen_deposited    FLOAT := 0;
    _addition_bcoin_deposited FLOAT := 0;
    _addition_sen_deposited   FLOAT := 0;
BEGIN
    SELECT bcoin_deposited, sen_deposited
    INTO _current_bcoin_deposited, _current_sen_deposited
    FROM user_total_bcoin_deposited
    WHERE uid = _uid and type = _type;

    SELECT COALESCE(_current_bcoin_deposited, 0), COALESCE(_current_sen_deposited, 0)
    INTO _current_bcoin_deposited, _current_sen_deposited;

    --     RAISE NOTICE 'Current bcoin deposited: % - Total bcoin deposited %', _current_bcoin_deposited, _total_bcoin_deposited;

    -- Nhập số tổng vào user_total_bcoin_deposited

    IF _total_bcoin_deposited > _current_bcoin_deposited THEN
        INSERT INTO user_total_bcoin_deposited(uid, bcoin_deposited, modify_date, type)
        VALUES (_uid, _total_bcoin_deposited, _timeStamp, _type)
        ON CONFLICT (uid, type) DO UPDATE SET bcoin_deposited = EXCLUDED.bcoin_deposited,
                                              modify_date     = EXCLUDED.modify_date;

        _addition_bcoin_deposited := _total_bcoin_deposited - _current_bcoin_deposited;
    END IF;

    IF _total_sen_deposited > _current_sen_deposited THEN
        INSERT INTO user_total_bcoin_deposited(uid, sen_deposited, modify_date, type)
        VALUES (_uid, _total_sen_deposited, _timeStamp, _type)
        ON CONFLICT (uid, type) DO UPDATE SET sen_deposited = EXCLUDED.sen_deposited,
                                              modify_date   = EXCLUDED.modify_date;

        _addition_sen_deposited := _total_sen_deposited - _current_sen_deposited;
    END IF;

    -- Nhập addition vào table user_block_reward

    IF _addition_bcoin_deposited > 0 THEN
        INSERT INTO user_block_reward(uid, reward_type, type, values, total_values, modify_date)
        VALUES (_uid, 'BCOIN_DEPOSITED', _type, _addition_bcoin_deposited, _addition_bcoin_deposited, _timeStamp)
        ON CONFLICT (uid, reward_type, type) DO UPDATE SET values       = user_block_reward.values + EXCLUDED.values,
                                                           total_values = user_block_reward.total_values + EXCLUDED.values,
                                                           modify_date  = EXCLUDED.modify_date;
    END IF;

    IF _addition_sen_deposited > 0 THEN
        INSERT INTO user_block_reward(uid, reward_type, type, values, total_values, modify_date)
        VALUES (_uid, 'SENSPARK_DEPOSITED', _type, _addition_sen_deposited, _addition_sen_deposited, _timeStamp)
        ON CONFLICT (uid, reward_type, type) DO UPDATE SET values       = user_block_reward.values + EXCLUDED.values,
                                                           total_values = user_block_reward.total_values + EXCLUDED.values,
                                                           modify_date  = EXCLUDED.modify_date;
    END IF;

END;
$$;


--
-- Name: sp_update_item_market_v3(integer, integer[], integer, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_update_item_market_v3(IN buyer_uid integer, IN list_id integer[], IN _item_id integer, IN _item_type integer)
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Update user_item_status
    INSERT INTO user_item_status (uid, id, item_id, status)
    SELECT buyer_uid, UNNEST(list_id), _item_id, 1
    ON CONFLICT (id, item_id)
        DO UPDATE SET uid = EXCLUDED.uid,
                      status = EXCLUDED.status;

    -- Update based on item type (bomb, wing, explode, trail)
    IF (_item_type IN (1, 2, 3, 8)) THEN
        -- Update skin
        UPDATE user_skin
        SET uid = buyer_uid,
            status = 0
        WHERE id = ANY(list_id);
    ELSIF (_item_type = 4) THEN
        -- Update heroes
        WITH _current AS (
            SELECT charactor, color, COUNT(*) AS count
            FROM user_bomber
            WHERE uid = buyer_uid
              AND type = 2
              AND hero_tr_type = 'HERO'
            GROUP BY charactor, color
        ),
        _new AS (
            SELECT *
            FROM user_bomber
            WHERE type = 2
              AND bomber_id = ANY(list_id)
        ),
        _processed AS (
            SELECT n.bomber_id,
                   n.charactor,
                   n.color,
                   COALESCE(c.count, 0) AS current_count,
                   ROW_NUMBER() OVER (PARTITION BY n.charactor, n.color) AS index
            FROM _new AS n
            LEFT JOIN _current AS c
            ON n.charactor = c.charactor AND n.color = c.color
        )
        UPDATE user_bomber AS ub
        SET uid = buyer_uid,
            hero_tr_type = CASE
                               WHEN p.current_count = 0 AND p.index = 1 THEN 'HERO'
                               ELSE 'SOUL'
                           END
        FROM _processed AS p
        WHERE ub.type = 2
          AND ub.bomber_id = p.bomber_id;
    ELSIF (_item_type = 5) THEN
        -- Update booster
        UPDATE user_booster
        SET uid = buyer_uid
        WHERE id = ANY(list_id);
    END IF;
END;
$$;


--
-- Name: sp_update_user_on_boarding(integer, integer, integer, integer, integer, character varying, character varying); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_update_user_on_boarding(IN p_uid integer, IN p_new_step integer, IN p_new_claimed integer, IN p_cur_step integer, IN p_cur_claimed integer, IN p_network character varying, IN p_rewardtype character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Update progress mới
    INSERT INTO user_on_boarding (uid, step, claimed)
    SELECT p_uid, p_new_step, p_new_claimed
    WHERE p_cur_step < p_new_step and p_cur_claimed <= p_new_claimed
    ON CONFLICT (uid) DO UPDATE SET step = p_new_step, claimed = p_new_claimed;

    -- Kiểm tra xem progess mới này có đc nhận thưởng ko
    IF p_new_claimed > p_cur_claimed THEN
        -- Cộng reward
        PERFORM fn_add_user_reward(
            p_uid,
            p_network,
            (SELECT reward FROM config_on_boarding WHERE step = p_new_claimed),
            p_rewardType,
            'Claim on boarding reward'
        );
    END IF;
END;
$$;


--
-- Name: sp_update_user_pvp_rank(); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_update_user_pvp_rank()
    LANGUAGE plpgsql
    AS $_$
DECLARE
    current_season     INT DEFAULT 1;
    currentSeasonTable VARCHAR(22) DEFAULT 'user_pvp_rank_ss_1';
BEGIN
        SELECT ps.id AS current_season
        INTO current_season
        FROM config_ranking_season AS ps
                 LEFT JOIN config_ranking_season AS ns ON ps.id = ns.id - 1
        WHERE NOW() AT TIME ZONE 'utc' BETWEEN ps.start_date AND ps.end_date
           OR NOW() AT TIME ZONE 'utc' BETWEEN ps.end_date AND ns.start_date
           OR NOW() AT TIME ZONE 'utc' > ps.end_date
        ORDER BY ps.end_date DESC
        LIMIT 1;

        currentSeasonTable = FORMAT('user_pvp_rank_ss_%1$s', current_season);

        EXECUTE FORMAT('UPDATE %s SET rank = NULL
                    WHERE rank IS NOT NULL', currentSeasonTable);
        EXECUTE FORMAT('WITH _rank AS (SELECT uid,
                      row_number() over (ORDER BY point DESC, CASE WHEN total_match > 0 THEN win_match / total_match ELSE 0 END DESC) AS rnk
               FROM %s
               )
               UPDATE %s as cr
                   SET rank = nr.rnk
                   FROM _rank nr
               WHERE cr.uid = nr.uid;', currentSeasonTable, currentSeasonTable);
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END
$_$;


--
-- Name: sp_user_buy_auto_mine(integer, character varying, character varying, json); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_user_buy_auto_mine(IN _uid integer, IN _reward_type character varying, IN _data_type character varying, IN _package_json json)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _time_stamp      timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _last_start_time timestamp DEFAULT NULL;
    _last_end_time   timestamp DEFAULT NULL;
    _start_time      timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _end_time        timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _price           float;
    _num_days        int;
BEGIN

    SELECT start_time,
           end_time
    INTO _last_start_time,
        _last_end_time
    FROM user_auto_mine
    WHERE uid = _uid
      AND type = _data_type;

    IF _last_end_time IS NOT NULL
    THEN
        IF EXTRACT(DAY FROM
                   (_time_stamp::timestamp - _last_end_time::timestamp)) < -2
        THEN
            RAISE EXCEPTION 'You can only renew your package on the last 2 days';

        END IF;
    END IF;

    SELECT _package_json ->> 'min_price',
           _package_json ->> 'num_days'
    INTO _price,
        _num_days;

--     tính thời gian auto mine
    IF _last_end_time IS NOT NULL AND _start_time < _last_end_time THEN
        _start_time = _last_start_time;
        _end_time = _last_end_time + (_num_days || ' DAY')::INTERVAL;
    ELSE
        _end_time = _end_time + (_num_days || ' DAY')::INTERVAL;
    END IF;

--     trừ reward
    PERFORM fn_sub_user_reward(_uid,
                              _data_type,
                              _price,
                              _reward_type,
                              'Buy auto mine');

--     update lại thời gian hết hạn auto mine
    INSERT INTO user_auto_mine(uid, start_time, end_time, type)
    VALUES (_uid, _start_time, _end_time, _data_type)
    ON CONFLICT (uid,type) DO UPDATE SET start_time  = excluded.start_time,
                                         end_time    = excluded.end_time,
                                         modify_time = _time_stamp;

--     ghi log
    INSERT INTO user_auto_mine_buy_logs(uid, time, num_day, price, deposit_amount, modify_time, type)
    VALUES (_uid, _time_stamp, _num_days, _price, 0, _time_stamp,
            _data_type);

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;

END;
$$;


--
-- Name: sp_user_buy_auto_mine(integer, character varying, character varying, character varying, json); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_user_buy_auto_mine(IN _uid integer, IN _first_reward_type character varying, IN _second_reward_type character varying, IN _data_type character varying, IN _package_json json)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _time_stamp      timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _last_start_time timestamp DEFAULT NULL;
    _last_end_time   timestamp DEFAULT NULL;
    _start_time      timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _end_time        timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _sub_reward      json;
    _price           float;
    _num_days        int;
BEGIN

    SELECT start_time,
           end_time
    INTO _last_start_time,
        _last_end_time
    FROM user_auto_mine
    WHERE uid = _uid
      AND type = _data_type;

    IF _last_end_time IS NOT NULL
    THEN
        IF EXTRACT(DAY FROM
                   (_time_stamp::timestamp - _last_end_time::timestamp)) < -2
        THEN
            RAISE EXCEPTION 'You can only renew your package on the last 2 days';
        END IF;
    ELSE
        IF EXTRACT(DAY FROM
                   (_time_stamp::timestamp - (SELECT datecreate FROM "user" WHERE id_user = _uid)::timestamp)) < -2
        THEN
            RAISE EXCEPTION 'You can only renew your package on the last 2 days';
        END IF;
    END IF;

    SELECT price,
           num_days
    INTO _price,
        _num_days
    FROM fn_calculate_package_auto_price(_uid, _package_json);

    IF (SELECT COUNT(*) FROM user_auto_mine WHERE uid = _uid AND type = _data_type) != 0
    THEN
        SELECT CASE WHEN _start_time < end_time THEN start_time ELSE _start_time END,
               CASE
                   WHEN _end_time < end_time
                       THEN end_time + (_num_days || ' DAY')::INTERVAL
                   ELSE _end_time + (_num_days || ' DAY')::INTERVAL
                   END
        INTO _start_time,
            _end_time
        FROM user_auto_mine
        WHERE uid = _uid
          AND type = _data_type;

    ELSE
        _end_time = _end_time + (_num_days || ' DAY')::INTERVAL;
    END IF;

--     trừ reward
    SELECT fn_sub_user_reward(_uid,
                              _data_type,
                              _price,
                              _first_reward_type,
                              _second_reward_type,
                              'Buy auto mine')::json
    INTO _sub_reward;

--     update lại thời gian hết hạn auto mine
    INSERT INTO user_auto_mine(uid, start_time, end_time, type)
    VALUES (_uid, _start_time, _end_time, _data_type)
    ON CONFLICT (uid,type) DO UPDATE SET start_time  = excluded.start_time,
                                         end_time    = excluded.end_time,
                                         modify_time = _time_stamp;

--     ghi log
    INSERT INTO user_auto_mine_buy_logs(uid, time, num_day, price, deposit_amount, modify_time, type)
    VALUES (_uid, _time_stamp, _num_days, _price, (_sub_reward ->> 'depositSubAmount')::double precision, _time_stamp,
            _data_type);

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;

END;
$$;


--
-- Name: sp_user_buy_gacha_chest_slot(integer, character varying, integer, integer, character varying); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_user_buy_gacha_chest_slot(IN _uid integer, IN _data_type character varying, IN _price integer, IN _slot_id integer, IN _gacha_chest_slots character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _time_stamp timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _receipt    json;
BEGIN
    --     trừ reward
    SELECT fn_sub_user_gem(_uid,
                           _data_type,
                           _price,
                           'Buy gacha chest slot')::json
    INTO _receipt;

    -- Update into user_config where uid = _uid
    UPDATE user_config
    SET gacha_chest_slots = _gacha_chest_slots
    WHERE uid = _uid;

--     ghi log
    INSERT INTO user_gacha_chest_slot_buy_logs(uid, time, slot_id, price, type, receipt)
    VALUES (_uid, _time_stamp, _slot_id, _price, _data_type, _receipt);

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;

END;
$$;


--
-- Name: sp_user_buy_rock_pack(integer, character varying, character varying, character varying, character varying); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_user_buy_rock_pack(IN _uid integer, IN _pack_name character varying, IN _network character varying, IN _reward_type character varying, IN _second_reward_type character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _value NUMERIC;
    _second_value NUMERIC;
    _price NUMERIC;
    _remaining_price NUMERIC;
    _rock_amount INT;
BEGIN
    SELECT
        COALESCE(SUM(CASE WHEN reward_type = _reward_type THEN values ELSE 0 END), 0) AS _value,
        COALESCE(SUM(CASE WHEN reward_type = _second_reward_type THEN values ELSE 0 END), 0) AS _second_value
    INTO _value, _second_value
    FROM user_block_reward
    WHERE type = _network AND uid = _uid AND (reward_type = _reward_type OR reward_type = _second_reward_type);

    IF _second_reward_type = 'SENSPARK' THEN
        SELECT sen_price, rock_amount INTO _price, _rock_amount FROM config_rock_pack WHERE pack_name = _pack_name;
    ELSE
        SELECT bcoin_price, rock_amount INTO _price, _rock_amount FROM config_rock_pack WHERE pack_name = _pack_name;
    END IF;

    IF _value + _second_value < _price THEN
        RAISE EXCEPTION 'Not enough % and %', _reward_type, _second_reward_type;
    END IF;

    --Trừ token deposit
    UPDATE user_block_reward SET values = values - LEAST(values, _price), modify_date = CURRENT_TIMESTAMP WHERE type = _network AND uid = _uid AND reward_type = _reward_type;
    _remaining_price := _price - _value;
    --Trừ token reward nếu còn thiếu
    IF _value < _price THEN
        UPDATE user_block_reward SET values = values - LEAST(values, _remaining_price), modify_date = CURRENT_TIMESTAMP WHERE type = _network AND uid = _uid AND reward_type = _second_reward_type;
    END IF;

    --Cộng đá
    INSERT INTO user_block_reward (uid, type, reward_type, values, total_values, modify_date)
    VALUES (_uid, 'TR', 'ROCK', _rock_amount, _rock_amount, CURRENT_TIMESTAMP)
    ON CONFLICT (uid, type, reward_type) DO UPDATE SET values = user_block_reward.values + EXCLUDED.values, total_values = user_block_reward.total_values + EXCLUDED.values, modify_date = CURRENT_TIMESTAMP;

    -- Lưu lại lịch sử
    INSERT INTO bombcrypto2.logs.logs_user_buy_rock_pack (uid, time_stamp, package_name, rock_amount, price, token_name, network)
    VALUES (_uid, CURRENT_TIMESTAMP, _pack_name, _rock_amount, _price, _reward_type, 'TR');

END;
$$;


--
-- Name: summary_pvp_ranking_reward(); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.summary_pvp_ranking_reward()
    LANGUAGE plpgsql
    AS $$
DECLARE
    i_season         INT;
    i_last_update    timestamp with time zone;
    i_interval_value INT;
BEGIN
    -- Check if interval + last_update < now
    SELECT last_update,
           COALESCE(interval_update, 300)
    INTO i_last_update,
        i_interval_value
    FROM schedule_status
    WHERE schedule = 'PVP';

    IF (i_last_update + (i_interval_value * interval '1 second')) < NOW() THEN
        UPDATE schedule_status
        SET last_update = NOW()
        WHERE schedule = 'PVP';

        SELECT ps.id
        INTO i_season
        FROM config_ranking_season AS ps
                 LEFT JOIN config_ranking_season AS ns ON ps.id = ns.id - 1
        WHERE NOW() AT TIME ZONE 'utc' BETWEEN ps.start_date AND ps.end_date
           OR NOW() AT TIME ZONE 'utc' BETWEEN ps.end_date AND ns.start_date
           OR NOW() AT TIME ZONE 'utc' > ps.end_date
        ORDER BY ps.end_date DESC
        LIMIT 1;

        INSERT INTO schedule_status(schedule, status, season)
        VALUES ('PVP', 1, i_season)
        ON CONFLICT (schedule) DO UPDATE SET status = 1, season = i_season;
        CALL sp_update_user_pvp_rank();
        CALL sp_setup_next_pvp_season();
        UPDATE schedule_status SET status = 0 WHERE schedule = 'PVP';
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: log_daily_task; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_daily_task (
    today_task character varying(20) NOT NULL,
    date timestamp with time zone
);


--
-- Name: log_user_activity_market_v3; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_activity_market_v3 (
    seller_uid integer NOT NULL,
    buyer_uid integer NOT NULL,
    item_id smallint NOT NULL,
    item_name character varying(50),
    type smallint NOT NULL,
    id integer,
    quantity integer,
    price double precision NOT NULL,
    reward_type character varying(30) NOT NULL,
    expiration integer,
    "time" timestamp without time zone DEFAULT (now() AT TIME ZONE 'utc'::text) NOT NULL
);


--
-- Name: log_user_bas_buy_activity; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_bas_buy_activity (
    uid integer,
    item_ids character varying,
    price real,
    type character varying(20),
    date timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: log_user_bas_fusion; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_bas_fusion (
    uid integer,
    hero_ids character varying,
    fee double precision,
    result character varying,
    reason_fail character varying(50),
    create_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: log_user_claim_referral; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_claim_referral (
    uid integer,
    amount double precision,
    claimed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: log_user_reactive_house; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_reactive_house (
    uid integer,
    house_id integer,
    data_type character varying(20),
    reactive_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: log_user_receive_offline_reward; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_receive_offline_reward (
    uid integer,
    log_in timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    log_out timestamp with time zone,
    time_offline double precision,
    reward double precision,
    network character varying(10)
);


--
-- Name: log_user_ron_buy_activity; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_ron_buy_activity (
    uid integer,
    item_ids character varying,
    price real,
    type character varying(20),
    date timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: log_user_ron_fusion; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_ron_fusion (
    uid integer,
    hero_ids character varying,
    fee double precision,
    result character varying,
    reason_fail character varying(50),
    create_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: log_user_sol_fusion; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_sol_fusion (
    uid integer NOT NULL,
    hero_ids character varying NOT NULL,
    fee double precision,
    result character varying,
    reason_fail character varying(50),
    create_at timestamp with time zone
);


--
-- Name: log_user_sol_fusion_v2; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_sol_fusion_v2 (
    uid integer NOT NULL,
    hero_ids character varying NOT NULL,
    fee double precision,
    result character varying,
    reason_fail character varying(50),
    create_at timestamp with time zone
);


--
-- Name: log_user_ton_buy_activity; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_ton_buy_activity (
    uid integer,
    item_ids character varying,
    price real,
    type character varying(20),
    date timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: log_user_ton_fusion; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_ton_fusion (
    uid integer NOT NULL,
    hero_ids character varying(100) NOT NULL,
    fee double precision,
    result character varying,
    reason_fail character varying(50),
    create_at timestamp with time zone
);


--
-- Name: COLUMN log_user_ton_fusion.hero_ids; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.log_user_ton_fusion.hero_ids IS 'danh sách các hero đc fusion';


--
-- Name: COLUMN log_user_ton_fusion.fee; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.log_user_ton_fusion.fee IS 'Phí ton deposit cho lần fusion này';


--
-- Name: COLUMN log_user_ton_fusion.result; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.log_user_ton_fusion.result IS 'id hero được fusion thành công';


--
-- Name: COLUMN log_user_ton_fusion.reason_fail; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.log_user_ton_fusion.reason_fail IS 'Lý do nếu fusion thất bại';


--
-- Name: log_user_ton_fusion_v2; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_ton_fusion_v2 (
    uid integer NOT NULL,
    hero_ids character varying NOT NULL,
    fee double precision,
    result character varying,
    reason_fail character varying(50),
    create_at timestamp with time zone
);


--
-- Name: log_user_vic_buy_activity; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_vic_buy_activity (
    uid integer,
    item_ids character varying,
    price real,
    type character varying(20),
    date timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: log_user_vic_fusion; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.log_user_vic_fusion (
    uid integer,
    hero_ids character varying,
    fee double precision,
    result character varying,
    reason_fail character varying(50),
    create_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_simulate_th_mode_v2; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_simulate_th_mode_v2 (
    uid integer NOT NULL,
    hero_id integer NOT NULL,
    speed integer NOT NULL,
    bomb integer NOT NULL,
    total_minutes integer NOT NULL,
    bomb_per_minutes integer NOT NULL,
    shield_lost_per_minutes double precision NOT NULL,
    total_bomb integer NOT NULL,
    total_shield_lost integer NOT NULL,
    bomb_to_stop character varying NOT NULL,
    shield_lost_to_stop character varying NOT NULL
);


--
-- Name: COLUMN logs_simulate_th_mode_v2.bomb; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_simulate_th_mode_v2.bomb IS 'Số bom hero này có thể đặt';


--
-- Name: COLUMN logs_simulate_th_mode_v2.total_minutes; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_simulate_th_mode_v2.total_minutes IS 'Tổng thời gian đã chạy (phút)';


--
-- Name: COLUMN logs_simulate_th_mode_v2.total_bomb; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_simulate_th_mode_v2.total_bomb IS 'Tổng bom đã đặt';


--
-- Name: COLUMN logs_simulate_th_mode_v2.total_shield_lost; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_simulate_th_mode_v2.total_shield_lost IS 'Tổng shield đã mất';


--
-- Name: COLUMN logs_simulate_th_mode_v2.bomb_to_stop; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_simulate_th_mode_v2.bomb_to_stop IS 'Tại số bom này dừng lại và trả về tổng số bom đặt và kết quả trung bình';


--
-- Name: COLUMN logs_simulate_th_mode_v2.shield_lost_to_stop; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_simulate_th_mode_v2.shield_lost_to_stop IS 'Tại số shield bị mất như này thì dừng lại và trả về tổng số shield mất và shield trung bình';



--
-- Name: logs_th_mode_v2_races; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_th_mode_v2_races (
    race_id integer NOT NULL,
    "timestamp" timestamp with time zone NOT NULL,
    table_name character varying(10)
);


--
-- Name: TABLE logs_th_mode_v2_races; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON TABLE logs.logs_th_mode_v2_races IS 'Danh sách các race';


--
-- Name: COLUMN logs_th_mode_v2_races.table_name; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_th_mode_v2_races.table_name IS 'ví dụ: 2024_05_31';


--
-- Name: logs_th_mode_v2_races_race_id_seq; Type: SEQUENCE; Schema: logs; Owner: -
--

CREATE SEQUENCE logs.logs_th_mode_v2_races_race_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: logs_th_mode_v2_races_race_id_seq; Type: SEQUENCE OWNED BY; Schema: logs; Owner: -
--

ALTER SEQUENCE logs.logs_th_mode_v2_races_race_id_seq OWNED BY logs.logs_th_mode_v2_races.race_id;


--
-- Name: logs_th_mode_v2_seq_race_id; Type: SEQUENCE; Schema: logs; Owner: -
--

CREATE SEQUENCE logs.logs_th_mode_v2_seq_race_id
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: logs_th_mode_v2_summary; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_th_mode_v2_summary (
    uid integer NOT NULL,
    hero_id integer NOT NULL,
    network smallint NOT NULL,
    date date NOT NULL,
    reward_bcoin double precision,
    reward_sen double precision,
    reward_coin double precision
);


--
-- Name: logs_th_mode_v2_template; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_th_mode_v2_template (
    race_id integer NOT NULL,
    uid integer NOT NULL,
    hero_id integer NOT NULL,
    reward_level smallint NOT NULL,
    "timestamp" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reward_bcoin double precision,
    reward_sen double precision,
    reward_coin double precision,
    pool_index integer,
    network_id smallint
);


--
-- Name: COLUMN logs_th_mode_v2_template.race_id; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_th_mode_v2_template.race_id IS 'id hiệp đấu';


--
-- Name: COLUMN logs_th_mode_v2_template.uid; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_th_mode_v2_template.uid IS 'user id';


--
-- Name: COLUMN logs_th_mode_v2_template.reward_level; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_th_mode_v2_template.reward_level IS 'user thuộc top level mấy ?';


--
-- Name: COLUMN logs_th_mode_v2_template.reward_bcoin; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_th_mode_v2_template.reward_bcoin IS 'bcoin | bomb';


--
-- Name: COLUMN logs_th_mode_v2_template.reward_sen; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_th_mode_v2_template.reward_sen IS 'senspark';


--
-- Name: COLUMN logs_th_mode_v2_template.reward_coin; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_th_mode_v2_template.reward_coin IS 'star coin';


--
-- Name: COLUMN logs_th_mode_v2_template.pool_index; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_th_mode_v2_template.pool_index IS 'hero thuộc pool số mấy';


--
-- Name: COLUMN logs_th_mode_v2_template.network_id; Type: COMMENT; Schema: logs; Owner: -
--

COMMENT ON COLUMN logs.logs_th_mode_v2_template.network_id IS 'BSC: 0, POLYGON: 1';


--
-- Name: logs_user_block_reward_2024_08_12; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_12 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_13; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_13 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_14; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_14 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_16; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_16 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_19; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_19 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_20; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_20 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_21; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_21 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_22; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_22 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_23; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_23 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_26; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_26 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_27; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_27 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_29; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_29 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_08_30; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_08_30 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_05; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_05 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_06; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_06 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_09; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_09 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_10; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_10 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_17; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_17 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_20; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_20 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_23; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_23 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_24; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_24 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_26; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_26 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_27; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_27 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_09_30; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_09_30 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_10_02; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_10_02 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_10_03; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_10_03 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_10_04; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_10_04 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_10_08; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_10_08 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_10_09; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_10_09 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_10_10; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_10_10 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_10_11; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_10_11 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_11_19; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_11_19 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_11_20; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_11_20 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_11_21; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_11_21 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_11_26; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_11_26 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_11_27; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_11_27 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_11_28; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_11_28 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_04; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_04 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_05; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_05 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_06; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_06 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_09; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_09 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_10; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_10 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_11; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_11 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_12; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_12 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_13; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_13 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_16; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_16 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_17; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_17 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_28; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_28 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_29; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_29 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_30; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_30 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2024_12_31; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2024_12_31 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_03; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_03 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_06; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_06 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_07; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_07 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_09; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_09 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_10; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_10 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_13; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_13 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_14; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_14 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_15; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_15 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_21; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_21 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_01_22; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_01_22 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_02_03; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_02_03 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_02_06; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_02_06 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_02_07; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_02_07 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_02_11; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_02_11 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_02_19; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_02_19 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_02_25; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_02_25 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_02_26; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_02_26 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_02_27; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_02_27 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_02_28; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_02_28 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_03; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_03 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_04; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_04 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_05; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_05 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_11; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_11 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_18; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_18 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_19; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_19 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_20; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_20 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_25; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_25 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_26; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_26 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_27; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_27 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_28; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_28 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_03_31; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_03_31 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_02; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_02 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_03; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_03 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_04; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_04 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_08; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_08 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_09; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_09 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_10; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_10 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_11; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_11 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_14; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_14 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_15; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_15 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_16; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_16 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_17; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_17 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_18; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_18 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_21; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_21 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_22; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_22 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_23; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_23 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_24; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_24 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_25; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_25 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_28; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_28 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_04_29; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_04_29 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_05; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_05 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_06; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_06 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_07; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_07 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_08; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_08 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_09; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_09 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_13; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_13 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_14; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_14 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_16; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_16 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_19; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_19 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_20; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_20 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_21; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_21 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_22; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_22 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_23; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_23 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_26; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_26 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_05_28; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_05_28 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_02; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_02 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_03; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_03 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_04; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_04 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_09; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_09 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_10; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_10 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_11; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_11 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_12; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_12 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_13; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_13 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_16; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_16 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_17; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_17 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_18; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_18 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_19; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_19 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_20; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_20 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_23; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_23 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_24; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_24 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_25; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_25 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_26; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_26 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_27; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_27 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_06_30; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_06_30 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_01; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_01 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_02; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_02 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_03; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_03 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_04; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_04 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_08; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_08 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_09; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_09 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_10; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_10 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_11; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_11 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_16; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_16 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_18; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_18 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_21; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_21 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_22; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_22 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_23; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_23 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_24; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_24 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_25; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_25 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_28; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_28 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_29; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_29 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_30; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_30 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_07_31; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_07_31 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_01; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_01 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_14; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_14 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_15; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_15 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_18; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_18 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_19; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_19 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_20; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_20 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_21; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_21 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_22; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_22 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_26; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_26 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_27; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_27 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_28; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_28 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_08_29; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_08_29 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_2025_09_03; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_2025_09_03 (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_block_reward_template; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_block_reward_template (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: logs_user_buy_rock_pack; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.logs_user_buy_rock_pack (
    uid integer NOT NULL,
    time_stamp timestamp with time zone,
    package_name character varying(20) NOT NULL,
    rock_amount integer NOT NULL,
    price double precision NOT NULL,
    token_name character varying(30) NOT NULL,
    network character varying(20) NOT NULL
);


--
-- Name: th_mode_v2; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.th_mode_v2 (
    race_id integer NOT NULL,
    uid integer NOT NULL,
    hero_id integer NOT NULL,
    reward_level smallint NOT NULL,
    "timestamp" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reward_bcoin double precision,
    reward_sen double precision,
    reward_coin double precision,
    pool_index integer,
    network_id smallint
);


--
-- Name: user_block_reward; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.user_block_reward (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT (now() AT TIME ZONE 'utc'::text)
);


--
-- Name: user_log_reward; Type: TABLE; Schema: logs; Owner: -
--

CREATE TABLE logs.user_log_reward (
    uid integer NOT NULL,
    reward_type character varying(20) NOT NULL,
    network character varying(10) NOT NULL,
    values_old double precision,
    values_changed double precision,
    values_new double precision,
    claim_pending_old double precision,
    claim_pending_changed double precision,
    claim_pending_new double precision,
    claim_synced double precision,
    claim_synced_changed double precision,
    claim_synced_new double precision,
    reason character varying(100),
    changed_at timestamp with time zone DEFAULT (now() AT TIME ZONE 'utc'::text)
);


--
-- Name: banned_country; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.banned_country (
    country_code character(42) NOT NULL
);


--
-- Name: bombcrypto_hero_traditional_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bombcrypto_hero_traditional_config (
    item_id integer NOT NULL,
    skin integer NOT NULL,
    color integer NOT NULL,
    name character varying(20) NOT NULL,
    speed integer NOT NULL,
    range integer NOT NULL,
    bomb integer NOT NULL,
    max_speed integer NOT NULL,
    max_range integer NOT NULL,
    max_bomb integer NOT NULL
);


--
-- Name: bombcrypto_lucky_ticket_daily_mission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bombcrypto_lucky_ticket_daily_mission (
    mission character varying(50) NOT NULL,
    times integer NOT NULL,
    ticket_reward_numb integer,
    active integer DEFAULT 1 NOT NULL,
    modify_date timestamp(0) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    sort integer DEFAULT 1 NOT NULL
);


--
-- Name: bombcrypto_lucky_wheel_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bombcrypto_lucky_wheel_reward (
    type character varying(30),
    reward_quantity integer,
    drop_rate integer,
    active integer,
    min_round_to_open integer,
    reward_pool integer,
    reward_claimed integer,
    max_reward_per_user integer,
    modify_date timestamp without time zone
);


--
-- Name: bombcrypto_marketplace_product; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bombcrypto_marketplace_product (
    id integer NOT NULL,
    type smallint NOT NULL,
    name character varying(50) NOT NULL,
    ability character varying(100) NOT NULL,
    modify_date timestamp without time zone DEFAULT (now() AT TIME ZONE 'utc'::text) NOT NULL
);


--
-- Name: bombcrypto_stake_vip_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bombcrypto_stake_vip_reward (
    id integer NOT NULL,
    level integer DEFAULT 1 NOT NULL,
    stake_amount numeric(8,0) NOT NULL,
    reward_type public.rewardtype NOT NULL,
    type character varying(30) NOT NULL,
    quantity integer NOT NULL,
    dates integer NOT NULL,
    modify_date timestamp(0) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: bombcrypto_stake_vip_reward_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.bombcrypto_stake_vip_reward ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.bombcrypto_stake_vip_reward_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config (
    key character varying(50) NOT NULL,
    value character varying(50)
);


--
-- Name: config_adventure_mode_entity_creator; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_adventure_mode_entity_creator (
    id integer NOT NULL,
    entity_id integer NOT NULL,
    health integer NOT NULL,
    damage integer NOT NULL,
    speed_move integer NOT NULL,
    follow smallint NOT NULL,
    through smallint NOT NULL,
    gold_reward_first_time integer NOT NULL,
    gold_reward_other_time integer NOT NULL,
    range integer NOT NULL
);


--
-- Name: config_adventure_mode_entity_creator_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_adventure_mode_entity_creator_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_adventure_mode_entity_creator_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_adventure_mode_entity_creator_id_seq OWNED BY public.config_adventure_mode_entity_creator.id;


--
-- Name: config_adventure_mode_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_adventure_mode_items (
    id integer NOT NULL,
    type character varying(50) NOT NULL,
    drop_rate integer NOT NULL,
    reward_min integer NOT NULL,
    reward_max integer NOT NULL,
    reward_type character varying(50)
);


--
-- Name: config_adventure_mode_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_adventure_mode_items_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_adventure_mode_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_adventure_mode_items_id_seq OWNED BY public.config_adventure_mode_items.id;


--
-- Name: config_adventure_mode_level_strategy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_adventure_mode_level_strategy (
    id integer NOT NULL,
    stage integer NOT NULL,
    level integer NOT NULL,
    enemies text NOT NULL,
    enemies_num text NOT NULL,
    "row" integer NOT NULL,
    col integer NOT NULL,
    row_v1 integer NOT NULL,
    col_v1 integer NOT NULL,
    density double precision NOT NULL,
    enemies_door text NOT NULL,
    enemies_door_first_num integer NOT NULL,
    enemies_door_then_num integer NOT NULL,
    blocks text,
    player_spawn text,
    door text,
    enemies_v2 text,
    is_free_revive_hero boolean NOT NULL,
    max_gold_reward integer NOT NULL
);


--
-- Name: config_adventure_mode_level_strategy_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_adventure_mode_level_strategy_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_adventure_mode_level_strategy_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_adventure_mode_level_strategy_id_seq OWNED BY public.config_adventure_mode_level_strategy.id;


--
-- Name: config_bid_club_package; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_bid_club_package (
    package_id integer,
    bid_quantity integer
);


--
-- Name: config_block; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_block (
    id integer NOT NULL,
    type character varying(20) NOT NULL,
    hp integer NOT NULL,
    id_reward integer NOT NULL
);


--
-- Name: config_block_drop_by_day; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_block_drop_by_day (
    id integer NOT NULL,
    type character varying(20) NOT NULL,
    days integer NOT NULL,
    datas character varying(255) NOT NULL
);


--
-- Name: config_block_drop_by_day_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_block_drop_by_day_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_block_drop_by_day_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_block_drop_by_day_id_seq OWNED BY public.config_block_drop_by_day.id;


--
-- Name: config_block_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_block_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_block_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_block_id_seq OWNED BY public.config_block.id;


--
-- Name: config_block_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_block_reward (
    data_type character varying(20) NOT NULL,
    block_id integer NOT NULL,
    type character varying(50) NOT NULL,
    min_reward double precision NOT NULL,
    max_reward double precision NOT NULL,
    reward_weight integer DEFAULT 0
);


--
-- Name: COLUMN config_block_reward.reward_weight; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_block_reward.reward_weight IS 'Tỉ lệ rơi ra phần thưởng này trong cùng 1 group block_id';


--
-- Name: config_bomber_ability; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_bomber_ability (
    ability integer NOT NULL,
    "values" double precision
);


--
-- Name: config_burn_hero; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_burn_hero (
    rarity integer NOT NULL,
    hero_s_rock double precision NOT NULL,
    hero_l_rock double precision NOT NULL
);


--
-- Name: config_coin_leaderboard; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_coin_leaderboard (
    id integer,
    name character varying(20),
    up_rank_point_user integer,
    up_rank_point_club bigint
);


--
-- Name: config_coin_ranking_season; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_coin_ranking_season (
    id integer NOT NULL,
    start_date timestamp with time zone,
    end_date timestamp with time zone,
    modify_date timestamp with time zone
);


--
-- Name: config_daily_check_in; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_daily_check_in (
    day integer,
    reward integer,
    data_type character varying(20)
);


--
-- Name: COLUMN config_daily_check_in.reward; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_daily_check_in.reward IS 'reward bằng star core';


--
-- Name: config_daily_mission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_daily_mission (
    code character varying(30) NOT NULL,
    action character varying(30) NOT NULL,
    number_mission integer DEFAULT 1 NOT NULL,
    rewards json DEFAULT '[]'::json NOT NULL,
    sort smallint DEFAULT 1 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    message_code character varying(255) NOT NULL,
    cool_down integer DEFAULT 0 NOT NULL,
    next_mission_code character varying(30),
    previous_mission_code character varying(30),
    type character varying(30) NOT NULL,
    description text
);


--
-- Name: config_drop_rate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_drop_rate (
    name character varying(20) NOT NULL,
    drop_rate character varying(255) NOT NULL
);


--
-- Name: config_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_event (
    id integer NOT NULL,
    name_event text,
    start_date timestamp without time zone,
    end_date timestamp without time zone
);


--
-- Name: config_event_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_event_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_event_id_seq OWNED BY public.config_event.id;


--
-- Name: config_free_reward_by_ads; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_free_reward_by_ads (
    reward_type character varying(10) NOT NULL,
    quantity_per_view integer NOT NULL,
    interval_in_minutes integer DEFAULT 240 NOT NULL
);


--
-- Name: config_gacha_chest; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_gacha_chest (
    type integer NOT NULL,
    open_time_in_minute integer,
    items_quantity integer,
    skip_open_time_gem_require integer,
    gold_price integer,
    gem_price integer,
    is_sellable boolean DEFAULT true NOT NULL,
    coin_price integer,
    is_has_discount boolean DEFAULT false NOT NULL,
    "desc" character varying(30)
);


--
-- Name: config_gacha_chest_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_gacha_chest_items (
    item_id integer NOT NULL,
    chest_type integer NOT NULL,
    min integer NOT NULL,
    max integer NOT NULL,
    drop_rate double precision NOT NULL,
    no integer,
    is_lock boolean DEFAULT false,
    expiration_after bigint
);


--
-- Name: config_gacha_chest_slot; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_gacha_chest_slot (
    id integer NOT NULL,
    slot integer NOT NULL,
    type character varying(50) NOT NULL,
    price integer NOT NULL
);


--
-- Name: config_gacha_chest_slot_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_gacha_chest_slot_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_gacha_chest_slot_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_gacha_chest_slot_id_seq OWNED BY public.config_gacha_chest_slot.id;


--
-- Name: config_grind_hero; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_grind_hero (
    item_kind character varying(30) NOT NULL,
    drop_items text NOT NULL,
    price integer NOT NULL
);


--
-- Name: config_hero_repair_shield; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_hero_repair_shield (
    id integer NOT NULL,
    rarity integer NOT NULL,
    shield_level integer NOT NULL,
    price double precision NOT NULL,
    price_rock double precision
);


--
-- Name: COLUMN config_hero_repair_shield.price_rock; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_hero_repair_shield.price_rock IS 'Giá sửa khiên bằng rock';


--
-- Name: config_hero_repair_shield_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_hero_repair_shield_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_hero_repair_shield_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_hero_repair_shield_id_seq OWNED BY public.config_hero_repair_shield.id;


--
-- Name: config_hero_traditional_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_hero_traditional_config (
    item_id integer NOT NULL,
    name character varying(255),
    skin integer NOT NULL,
    color integer NOT NULL,
    speed integer NOT NULL,
    range integer NOT NULL,
    bomb integer NOT NULL,
    hp integer NOT NULL,
    dmg integer NOT NULL,
    max_speed integer NOT NULL,
    max_range integer NOT NULL,
    max_bomb integer NOT NULL,
    max_dmg integer NOT NULL,
    max_hp integer NOT NULL,
    tutorial integer NOT NULL,
    can_be_bot integer NOT NULL,
    max_upgrade_speed integer NOT NULL,
    max_upgrade_range integer NOT NULL,
    max_upgrade_bomb integer NOT NULL,
    max_upgrade_hp integer NOT NULL,
    max_upgrade_dmg integer NOT NULL
);


--
-- Name: config_hero_trial_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_hero_trial_config (
    power character varying(255) NOT NULL,
    bomb_range character varying(255) NOT NULL,
    stamina character varying(255) NOT NULL,
    speed character varying(255) NOT NULL,
    bomb character varying(255) NOT NULL,
    ability character varying(255) NOT NULL,
    charactor character varying(255) NOT NULL,
    color character varying(255) NOT NULL,
    rare integer NOT NULL,
    number integer NOT NULL,
    bomb_skin character varying(255) NOT NULL,
    ability_shield character varying(255) NOT NULL
);


--
-- Name: config_hero_upgrade_level; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_hero_upgrade_level (
    level smallint NOT NULL,
    hero smallint NOT NULL,
    gold smallint NOT NULL
);


--
-- Name: config_hero_upgrade_power; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_hero_upgrade_power (
    rare integer NOT NULL,
    datas text
);


--
-- Name: config_hero_upgrade_shield; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_hero_upgrade_shield (
    rarity integer,
    data text,
    price character varying(100)
);


--
-- Name: COLUMN config_hero_upgrade_shield.price; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_hero_upgrade_shield.price IS 'Giá nâng cấp level shield (level 0 không cần nâng cấp nên bằng 0)';


--
-- Name: config_iap_gold_shop; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_iap_gold_shop (
    item_id integer NOT NULL,
    name character varying(255),
    gem_price integer,
    golds_receive integer,
    bonus_first_time integer DEFAULT 0 NOT NULL
);


--
-- Name: config_iap_shop; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_iap_shop (
    product_id character varying NOT NULL,
    type character varying NOT NULL,
    name character varying NOT NULL,
    items json DEFAULT '[]'::json NOT NULL,
    limit_per_user integer,
    bonus_type integer DEFAULT 0 NOT NULL,
    items_bonus json DEFAULT '[]'::json NOT NULL,
    is_stater_pack boolean DEFAULT false NOT NULL,
    is_remove_ads boolean DEFAULT false NOT NULL,
    buy_step integer DEFAULT 1 NOT NULL,
    purchase_time_limit bigint
);


--
-- Name: config_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_item (
    id integer NOT NULL,
    type smallint NOT NULL,
    name character varying(50) NOT NULL,
    ability character varying(100) DEFAULT '[]'::character varying NOT NULL,
    modify_date timestamp without time zone DEFAULT (now() AT TIME ZONE 'utc'::text) NOT NULL,
    description_en character varying(100),
    kind character varying(30) DEFAULT 'NORMAL'::character varying NOT NULL,
    gold_price_7_days integer,
    gem_price_7_days integer,
    gem_price_30_days integer,
    gold_price integer,
    gem_price integer,
    active boolean,
    is_sellable boolean,
    is_default boolean DEFAULT false NOT NULL,
    tag character varying(20),
    sale_start_date date,
    sale_end_date date
);


--
-- Name: config_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.config_item ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.config_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: config_item_market_v3; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_item_market_v3 (
    id integer NOT NULL,
    type integer NOT NULL,
    name character varying(50) NOT NULL,
    kind character varying(30) NOT NULL,
    min_price double precision,
    max_price double precision,
    is_have_expiration integer DEFAULT 1 NOT NULL,
    max_price_7_days double precision,
    max_price_30_days double precision,
    active integer DEFAULT 1 NOT NULL,
    fixed_amount integer DEFAULT 30 NOT NULL,
    reward_type character varying(30) DEFAULT 'GEM'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: config_lucky_wheel_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_lucky_wheel_reward (
    code character varying(30) NOT NULL,
    item_id integer,
    quantity integer,
    active boolean DEFAULT true NOT NULL,
    sort integer DEFAULT 1,
    weight integer DEFAULT 1 NOT NULL
);


--
-- Name: config_message; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_message (
    code character varying(255) NOT NULL,
    vn character varying(255),
    en character varying(255)
);


--
-- Name: config_min_stake_hero; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_min_stake_hero (
    rarity integer NOT NULL,
    min_stake_amount integer
);


--
-- Name: config_mission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_mission (
    code character varying(36) DEFAULT public.uuid_generate_v4() NOT NULL,
    action character varying(30) NOT NULL,
    type character varying(30) NOT NULL,
    number_mission integer DEFAULT 1 NOT NULL,
    rewards json DEFAULT '[]'::json NOT NULL,
    sort smallint DEFAULT 1 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    message_code character varying(255) NOT NULL,
    cool_down integer DEFAULT 0 NOT NULL,
    next_mission_code character varying(30),
    previous_mission_code character varying(30),
    name character varying(255),
    number_mission_max integer DEFAULT 1 NOT NULL,
    note text
);


--
-- Name: config_mystery_box_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_mystery_box_item (
    item_id integer NOT NULL,
    weight integer NOT NULL,
    expiration_after integer,
    quantity integer DEFAULT 1 NOT NULL
);


--
-- Name: config_new_user_gift; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_new_user_gift (
    item_id integer NOT NULL,
    quantity integer NOT NULL,
    item_name character varying(50),
    active boolean NOT NULL,
    expiration_after bigint DEFAULT 0,
    step integer DEFAULT 0 NOT NULL
);


--
-- Name: config_offline_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_offline_reward (
    offline_hours integer NOT NULL,
    rewards_json text NOT NULL
);


--
-- Name: config_offline_reward_th_mode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_offline_reward_th_mode (
    key character varying(20),
    value character varying(50),
    network character varying(10)
);


--
-- Name: config_on_boarding; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_on_boarding (
    step integer NOT NULL,
    reward double precision,
    description character varying(100)
);


--
-- Name: COLUMN config_on_boarding.reward; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_on_boarding.reward IS 'reward bằng star core';


--
-- Name: COLUMN config_on_boarding.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_on_boarding.description IS 'Chỉ để xem ở database';


--
-- Name: config_package_auto_mine; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_package_auto_mine (
    id integer NOT NULL,
    package_name character varying(30),
    num_day integer,
    min_price real,
    price_percent real,
    data_type character varying(20)
);


--
-- Name: config_package_auto_mine_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_package_auto_mine_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_package_auto_mine_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_package_auto_mine_id_seq OWNED BY public.config_package_auto_mine.id;


--
-- Name: config_package_rent_house_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_package_rent_house_v2 (
    rarity integer,
    num_days integer,
    price double precision DEFAULT 0.0,
    data_type character varying(20)
);


--
-- Name: config_params_referral; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_params_referral (
    name character varying(20),
    id integer
);


--
-- Name: config_pvp_ranking; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_pvp_ranking (
    bomb_rank integer NOT NULL,
    start_point integer,
    end_point integer,
    name character varying(20),
    win_point integer,
    loose_point integer,
    min_matches integer,
    decay_point integer
);


--
-- Name: config_pvp_ranking_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_pvp_ranking_reward (
    rank_min integer,
    rank_max integer,
    reward character varying(50)
);


--
-- Name: config_ranking_season; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_ranking_season (
    id integer NOT NULL,
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    modify_date timestamp without time zone,
    is_calculated_reward boolean DEFAULT false NOT NULL
);


--
-- Name: config_referral_params; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_referral_params (
    name character varying(30),
    id integer
);


--
-- Name: config_reset_shield_balancer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_reset_shield_balancer (
    rare integer NOT NULL,
    final_damage integer
);


--
-- Name: config_revive_hero_cost; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_revive_hero_cost (
    times integer NOT NULL,
    allow_ads smallint NOT NULL,
    gem_amount integer NOT NULL,
    modify_date timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: config_reward_level_th_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_reward_level_th_v2 (
    level integer NOT NULL,
    num_users integer,
    bcoin real,
    sen real,
    coin real
);


--
-- Name: config_reward_pool_th_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_reward_pool_th_v2 (
    pool_id integer NOT NULL,
    remaining_reward double precision NOT NULL,
    type character varying(20) NOT NULL,
    max_reward integer
);


--
-- Name: config_rock_pack; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_rock_pack (
    pack_name character varying(20) NOT NULL,
    rock_amount integer NOT NULL,
    sen_price double precision NOT NULL,
    bcoin_price double precision NOT NULL
);


--
-- Name: COLUMN config_rock_pack.pack_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_rock_pack.pack_name IS 'tên gói';


--
-- Name: COLUMN config_rock_pack.rock_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_rock_pack.rock_amount IS 'Số rock nhận được';


--
-- Name: COLUMN config_rock_pack.sen_price; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_rock_pack.sen_price IS 'Giá mua bằng sen';


--
-- Name: COLUMN config_rock_pack.bcoin_price; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_rock_pack.bcoin_price IS 'Giá mua bằng bcoin';


--
-- Name: config_server; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_server (
    server_name character varying(50) NOT NULL,
    id character varying(50),
    server_address character varying(50) NOT NULL,
    editor_address character varying(50),
    zone character varying(5)
);


--
-- Name: COLUMN config_server.editor_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_server.editor_address IS 'Địa chỉ dùng cho editor';


--
-- Name: COLUMN config_server.zone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_server.zone IS 'Ký hiệu zome để tìm server phù hợp';


--
-- Name: config_skin_chest_drop_rate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_skin_chest_drop_rate (
    value character varying(50)
);


--
-- Name: config_stake_vip_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_stake_vip_reward (
    id integer NOT NULL,
    level integer NOT NULL,
    stake_amount double precision NOT NULL,
    reward_type character varying(50) NOT NULL,
    type character varying(50) NOT NULL,
    quantity integer NOT NULL,
    dates integer NOT NULL
);


--
-- Name: config_stake_vip_reward_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_stake_vip_reward_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_stake_vip_reward_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_stake_vip_reward_id_seq OWNED BY public.config_stake_vip_reward.id;


--
-- Name: config_subscription; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_subscription (
    id character varying(30),
    description text,
    no_ads boolean DEFAULT true NOT NULL,
    random_items text NOT NULL,
    offline_reward_bonus integer NOT NULL,
    pvp_penalty boolean NOT NULL,
    bonus_pvp_point_rate real NOT NULL,
    adventure_bonus_item_rate real NOT NULL,
    gem_package_bonus real NOT NULL
);


--
-- Name: config_swap_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_swap_token (
    from_type character varying(20) NOT NULL,
    from_network character varying(10) NOT NULL,
    to_type character varying(20) NOT NULL,
    to_network character varying(10) NOT NULL,
    ratio real DEFAULT 0 NOT NULL
);


--
-- Name: TABLE config_swap_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.config_swap_token IS 'Bảng tỉ lệ chuyển đổi giữa các token';


--
-- Name: COLUMN config_swap_token.ratio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_swap_token.ratio IS '1 _from đổi được bao nhiêu _to?';


--
-- Name: config_swap_token_realtime; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_swap_token_realtime (
    key character varying(50) NOT NULL,
    value character varying
);


--
-- Name: config_th_mode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_th_mode (
    key character varying(30) NOT NULL,
    value character varying(512),
    network character varying(10) NOT NULL
);


--
-- Name: config_th_mode_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_th_mode_v2 (
    key character varying(20) NOT NULL,
    value character varying(100),
    date_updated timestamp(0) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    type character varying(10) NOT NULL
);


--
-- Name: config_ton_tasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_ton_tasks (
    id integer NOT NULL,
    name character varying(100),
    reward integer,
    type integer,
    deleted integer DEFAULT 0 NOT NULL
);


--
-- Name: config_ton_tasks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_ton_tasks_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_ton_tasks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_ton_tasks_id_seq OWNED BY public.config_ton_tasks.id;


--
-- Name: config_upgrade_crystal; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_upgrade_crystal (
    source_item_id integer NOT NULL,
    target_item_id integer NOT NULL,
    gold_fee integer,
    gem_fee integer
);


--
-- Name: config_upgrade_hero_tr; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_upgrade_hero_tr (
    index integer NOT NULL,
    type character varying(30) NOT NULL,
    gold_fee integer NOT NULL,
    gem_fee integer NOT NULL,
    items text DEFAULT '[]'::text NOT NULL
);


--
-- Name: contract; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contract (
    active boolean NOT NULL,
    contract character varying(45) NOT NULL,
    network_type integer NOT NULL,
    token_type integer NOT NULL
);


--
-- Name: daily_task_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.daily_task_config (
    id integer NOT NULL,
    completed integer NOT NULL,
    reward character varying,
    is_random boolean,
    is_default boolean,
    expired bigint,
    is_deleted boolean,
    description character varying(100)
);


--
-- Name: COLUMN daily_task_config.completed; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.daily_task_config.completed IS 'Bước cuối cùng để hoàn thành task';


--
-- Name: COLUMN daily_task_config.reward; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.daily_task_config.reward IS 'Danh sách các reward có dạng [item_id : amount?]';


--
-- Name: COLUMN daily_task_config.is_random; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.daily_task_config.is_random IS 'Random 1 trong các reward';


--
-- Name: COLUMN daily_task_config.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.daily_task_config.description IS 'Chỉ để xem ở database';


--
-- Name: deleted_config_gacha_chest_drop_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.deleted_config_gacha_chest_drop_items (
    item_id integer,
    name character varying,
    description character varying
);


--
-- Name: game_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_config (
    key character varying(50) NOT NULL,
    value character varying(300) NOT NULL,
    date_updated timestamp(0) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: game_disable_feature_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_disable_feature_config (
    version integer NOT NULL,
    disable_feature_ids integer[] NOT NULL
);


--
-- Name: log_block_receive_reward_summary; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_block_receive_reward_summary (
    user_name character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    date date NOT NULL,
    value numeric(10,4) NOT NULL
);


--
-- Name: log_block_recieve_reward_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_block_recieve_reward_template (
    id bigint NOT NULL,
    user_name character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    value double precision NOT NULL,
    log_date timestamp(0) without time zone NOT NULL,
    mode character varying(10) DEFAULT 'PVE'::character varying NOT NULL,
    data_type character varying(10),
    uid integer
);


--
-- Name: log_block_recieve_reward_template_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.log_block_recieve_reward_template ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.log_block_recieve_reward_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: log_buy_chest_gacha; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_buy_chest_gacha (
    id integer NOT NULL,
    id_user integer NOT NULL,
    chest_name character varying(255) NOT NULL,
    value integer NOT NULL,
    buy_time timestamp with time zone NOT NULL
);


--
-- Name: log_buy_chest_gacha_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.log_buy_chest_gacha_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: log_buy_chest_gacha_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.log_buy_chest_gacha_id_seq OWNED BY public.log_buy_chest_gacha.id;


--
-- Name: log_hack; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_hack (
    user_name character varying(100) NOT NULL,
    hack_type integer NOT NULL,
    data character varying,
    log_date timestamp with time zone NOT NULL
);


--
-- Name: log_play_pve; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_play_pve (
    user_id integer,
    level integer,
    stage integer,
    match_time timestamp with time zone,
    match_result text,
    total_time bigint,
    boosters jsonb,
    hero_id integer
);


--
-- Name: log_play_pvp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_play_pvp (
    id_match character varying(50) NOT NULL,
    server_id character varying(20) NOT NULL,
    match_result character varying(42) NOT NULL,
    play_date timestamp without time zone NOT NULL,
    time_match integer NOT NULL,
    user_name_1 character varying(42) NOT NULL,
    user_name_2 character varying(42) NOT NULL,
    bomber_id_1 integer NOT NULL,
    bomber_id_2 integer NOT NULL,
    item_collect_1 integer NOT NULL,
    item_collect_2 integer NOT NULL,
    reason_lose_1 character varying(42) NOT NULL,
    reason_lose_2 character varying(42) NOT NULL,
    point_1 integer NOT NULL,
    point_2 integer NOT NULL,
    latency integer[] NOT NULL,
    time_delta integer[] NOT NULL,
    loss_rate double precision[] NOT NULL,
    country_code text[] NOT NULL
);


--
-- Name: log_pvp_booster; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_pvp_booster (
    date timestamp without time zone NOT NULL,
    uid integer NOT NULL,
    item_id integer NOT NULL,
    type character varying(10) NOT NULL,
    booster_name character varying(30) NOT NULL,
    fee_amount double precision
);


--
-- Name: log_repair_shield; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_repair_shield (
    username character varying(255) NOT NULL,
    repair_time timestamp with time zone NOT NULL,
    bomber_id integer NOT NULL,
    remain_shield integer NOT NULL,
    type character varying(255) NOT NULL,
    uid integer,
    reward_type character varying(20)
);


--
-- Name: COLUMN log_repair_shield.reward_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.log_repair_shield.reward_type IS 'Nguyên liệu dùng để sửa khiên';


--
-- Name: log_user_claim_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_user_claim_reward (
    uid integer,
    claim_date timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP(6),
    value double precision,
    reward_type character varying(20),
    data_type character varying(10)
);


--
-- Name: log_user_grind_hero; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_user_grind_hero (
    uid integer NOT NULL,
    date timestamp(6) without time zone NOT NULL,
    hero_count integer NOT NULL,
    hero_ids text NOT NULL
);


--
-- Name: log_user_lucky_wheel_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_user_lucky_wheel_reward (
    uid integer,
    reward_type character varying(30),
    "time" timestamp without time zone,
    reward_quantity integer
);


--
-- Name: log_user_sol_buy_activity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_user_sol_buy_activity (
    uid integer,
    item_ids character varying,
    price real,
    type character varying(20),
    date timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: pvp_fixture_matches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pvp_fixture_matches (
    id integer NOT NULL,
    player_1_uid integer NOT NULL,
    player_1_username character varying(100) NOT NULL,
    player_2_uid integer NOT NULL,
    player_2_username character varying(100) NOT NULL,
    hero_profile smallint DEFAULT 0,
    fixed_zone character varying(5),
    from_time timestamp with time zone NOT NULL,
    to_time timestamp with time zone NOT NULL,
    mode integer
);


--
-- Name: TABLE pvp_fixture_matches; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.pvp_fixture_matches IS 'Danh sách các trận đấu cố định';


--
-- Name: COLUMN pvp_fixture_matches.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pvp_fixture_matches.id IS 'id đăng ký';


--
-- Name: COLUMN pvp_fixture_matches.hero_profile; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pvp_fixture_matches.hero_profile IS 'Preset các chỉ số của hero (0) là tự do';


--
-- Name: COLUMN pvp_fixture_matches.fixed_zone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pvp_fixture_matches.fixed_zone IS 'Cố định zone';


--
-- Name: COLUMN pvp_fixture_matches.from_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pvp_fixture_matches.from_time IS 'Bắt đầu từ thời điểm';


--
-- Name: COLUMN pvp_fixture_matches.to_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pvp_fixture_matches.to_time IS 'Kết thúc tại thời điểm';


--
-- Name: pvp_fixture_matches_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.pvp_fixture_matches ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pvp_fixture_matches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pvp_tournament; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pvp_tournament (
    id integer NOT NULL,
    mode integer,
    participant_1 character varying(255),
    participant_2 character varying(255),
    status character varying(30),
    find_begin_time timestamp with time zone,
    find_end_time timestamp with time zone,
    finish_time timestamp with time zone,
    user_1_score integer,
    user_2_score integer
);


--
-- Name: COLUMN pvp_tournament.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pvp_tournament.status IS 'PENDING or COMPLETE or ABORTED';


--
-- Name: pvp_tournament_backup; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pvp_tournament_backup (
    id integer NOT NULL,
    mode integer,
    participant_1 character varying(255),
    participant_2 character varying(255),
    status character varying(30),
    find_begin_time timestamp with time zone,
    find_end_time timestamp with time zone,
    finish_time timestamp with time zone,
    user_1_score integer,
    user_2_score integer
);


--
-- Name: schedule_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schedule_status (
    schedule character varying(20),
    status integer,
    season integer,
    interval_update integer DEFAULT 300,
    last_update timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: COLUMN schedule_status.interval_update; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.schedule_status.interval_update IS 'Mặc định là 5 phút';


--
-- Name: user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."user" (
    id_user integer NOT NULL,
    user_name character varying(100) NOT NULL,
    datecreate timestamp(0) without time zone DEFAULT CURRENT_TIMESTAMP,
    lastlogin timestamp(0) without time zone DEFAULT CURRENT_TIMESTAMP,
    lastlogout timestamp(0) without time zone,
    is_ban smallint DEFAULT 0 NOT NULL,
    hash character varying(50) DEFAULT ''::character varying NOT NULL,
    is_review smallint DEFAULT 0 NOT NULL,
    reward_percent double precision DEFAULT 100 NOT NULL,
    mining_token character varying(20) DEFAULT 'BCOIN'::character varying NOT NULL,
    default_mining_token character varying(20) DEFAULT 'BCOIN'::character varying NOT NULL,
    password character varying(100),
    second_username character varying(100),
    activated smallint DEFAULT 0 NOT NULL,
    email character varying(320),
    verify_code character varying(6),
    email_verified_at timestamp(6) with time zone,
    name character varying(255),
    mode character varying(20) DEFAULT 'NON_TRIAL'::character varying NOT NULL,
    type character varying(20) DEFAULT 'FI'::character varying,
    lastlogin_mobile timestamp without time zone,
    lastlogout_mobile timestamp without time zone,
    first_logout timestamp without time zone,
    privilege smallint,
    ban_reason character varying(100),
    ban_at timestamp with time zone,
    ban_expired_at timestamp with time zone
);


--
-- Name: COLUMN "user".privilege; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public."user".privilege IS 'Phân quyền';


--
-- Name: user_activity_marketplace; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_activity_marketplace (
    id integer NOT NULL,
    uid integer NOT NULL,
    "time" timestamp without time zone DEFAULT (now() AT TIME ZONE 'utc'::text) NOT NULL,
    action smallint NOT NULL,
    item_name character varying(50),
    source character varying(42),
    instant_id integer NOT NULL,
    type smallint NOT NULL,
    item_id smallint NOT NULL,
    price double precision NOT NULL,
    reward_type smallint NOT NULL,
    unit_price integer
);


--
-- Name: user_activity_marketplace_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_activity_marketplace_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_activity_marketplace_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_activity_marketplace_id_seq OWNED BY public.user_activity_marketplace.id;


--
-- Name: user_adventure_mode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_adventure_mode (
    uid integer NOT NULL,
    current_level integer,
    max_level integer,
    current_stage integer,
    max_stage integer,
    hero_id integer
);


--
-- Name: user_auto_mine; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_auto_mine (
    uid integer NOT NULL,
    start_time timestamp(6) without time zone NOT NULL,
    end_time timestamp(6) without time zone NOT NULL,
    modify_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    type character varying(20) NOT NULL
);


--
-- Name: user_auto_mine_buy_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_auto_mine_buy_logs (
    uid integer NOT NULL,
    "time" timestamp(6) without time zone NOT NULL,
    num_day integer NOT NULL,
    price double precision,
    modify_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deposit_amount double precision,
    type character varying(10)
);


--
-- Name: user_bas_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_bas_transactions (
    id integer NOT NULL,
    uid integer NOT NULL,
    transaction_type character varying(30),
    amount double precision,
    token_name character varying(20),
    tx_hash character varying(255),
    created_at timestamp with time zone
);


--
-- Name: user_bas_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_bas_transactions ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_bas_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_bcoin_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_bcoin_transactions (
    id integer NOT NULL,
    uid integer NOT NULL,
    transaction_type character varying(30),
    amount double precision,
    token_name character varying(20),
    tx_hash character varying(255),
    created_at timestamp with time zone
);


--
-- Name: user_bcoin_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_bcoin_transactions ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_bcoin_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_block_map_pve; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_block_map_pve (
    uid integer NOT NULL,
    mode integer NOT NULL,
    type character varying(255) NOT NULL,
    block_map text,
    tileset integer,
    updated_date timestamp without time zone,
    created_date timestamp without time zone
);


--
-- Name: user_block_reward; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_block_reward (
    uid integer NOT NULL,
    reward_type character varying(50) NOT NULL,
    "values" double precision DEFAULT 0 NOT NULL,
    total_values double precision DEFAULT 0.0000 NOT NULL,
    modify_date timestamp(0) without time zone,
    last_time_claim_success timestamp(0) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    claim_pending double precision DEFAULT 0 NOT NULL,
    claim_synced double precision DEFAULT 0 NOT NULL,
    type character varying(20) DEFAULT 'BSC'::character varying NOT NULL
);


--
-- Name: user_block_reward_gift; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_block_reward_gift (
    uid integer NOT NULL,
    reward_type character varying NOT NULL,
    count integer NOT NULL,
    rarity integer NOT NULL,
    category integer NOT NULL,
    is_hero_s integer DEFAULT 1 NOT NULL,
    skin integer NOT NULL,
    modify_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status character varying DEFAULT 'WAITING'::character varying NOT NULL,
    data_type character varying(10) NOT NULL
);


--
-- Name: COLUMN user_block_reward_gift.data_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_block_reward_gift.data_type IS 'network type';


--
-- Name: user_bomber; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_bomber (
    uid integer NOT NULL,
    gen_id character varying(255),
    bomber_id bigint NOT NULL,
    name character varying(255),
    level integer NOT NULL,
    power integer NOT NULL,
    bomb_range integer NOT NULL,
    stamina integer NOT NULL,
    speed integer NOT NULL,
    bomb integer NOT NULL,
    ability character varying(255) NOT NULL,
    charactor integer NOT NULL,
    color integer NOT NULL,
    rare integer NOT NULL,
    bomb_skin integer NOT NULL,
    energy integer DEFAULT 0 NOT NULL,
    stage smallint DEFAULT 0 NOT NULL,
    time_rest timestamp(0) without time zone,
    active smallint DEFAULT 1 NOT NULL,
    "hasDelete" smallint DEFAULT 0 NOT NULL,
    random integer DEFAULT 0 NOT NULL,
    shield character varying(255) DEFAULT NULL::character varying,
    ability_shield character varying(255) DEFAULT NULL::character varying,
    is_reset integer DEFAULT 0 NOT NULL,
    shield_level integer DEFAULT 0 NOT NULL,
    type integer DEFAULT 0 NOT NULL,
    data_type character varying(10) DEFAULT 'BSC'::character varying NOT NULL,
    hero_tr_type character varying(10) DEFAULT 'SOUL'::character varying NOT NULL,
    stake_amount double precision DEFAULT 0.0 NOT NULL,
    stake_sen double precision DEFAULT 0.0 NOT NULL,
    create_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: COLUMN user_bomber.stake_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_bomber.stake_amount IS 'stake_bcoin';


--
-- Name: user_bomber_id_bas_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_bomber_id_bas_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_bomber_id_non_fi_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_bomber_id_non_fi_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_bomber_id_ron_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_bomber_id_ron_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_bomber_id_server_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_bomber_id_server_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_bomber_id_sol_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_bomber_id_sol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_bomber_id_vic_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_bomber_id_vic_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_bomber_lock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_bomber_lock (
    bomber_id integer NOT NULL,
    hero_type integer NOT NULL,
    data_type character varying(10) NOT NULL,
    lock_since timestamp with time zone,
    lock_seconds integer,
    reason character varying(255)
);


--
-- Name: TABLE user_bomber_lock; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_bomber_lock IS 'Khoá user bomber';


--
-- Name: COLUMN user_bomber_lock.lock_since; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_bomber_lock.lock_since IS 'Thời điểm bắt đầu khoá';


--
-- Name: COLUMN user_bomber_lock.lock_seconds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_bomber_lock.lock_seconds IS 'Thời gian khoá';


--
-- Name: user_bomber_old_season; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_bomber_old_season (
    uid integer NOT NULL,
    gen_id character varying(255),
    bomber_id bigint NOT NULL,
    name character varying(255),
    level integer NOT NULL,
    power integer NOT NULL,
    bomb_range integer NOT NULL,
    stamina integer NOT NULL,
    speed integer NOT NULL,
    bomb integer NOT NULL,
    ability character varying(255) NOT NULL,
    charactor integer NOT NULL,
    color integer NOT NULL,
    rare integer NOT NULL,
    bomb_skin integer NOT NULL,
    energy integer DEFAULT 0 NOT NULL,
    stage smallint DEFAULT 0 NOT NULL,
    time_rest timestamp(0) without time zone,
    active smallint DEFAULT 1 NOT NULL,
    "hasDelete" smallint DEFAULT 0 NOT NULL,
    random integer DEFAULT 0 NOT NULL,
    shield character varying(255) DEFAULT NULL::character varying,
    ability_shield character varying(255) DEFAULT NULL::character varying,
    is_reset integer DEFAULT 0 NOT NULL,
    shield_level integer DEFAULT 0 NOT NULL,
    type integer DEFAULT 0 NOT NULL,
    data_type character varying(10) DEFAULT 'BSC'::character varying NOT NULL,
    hero_tr_type character varying(10) DEFAULT 'SOUL'::character varying NOT NULL,
    stake_amount double precision DEFAULT 0.0 NOT NULL,
    stake_sen double precision DEFAULT 0.0 NOT NULL,
    create_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: user_bomber_upgraded; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_bomber_upgraded (
    uid integer NOT NULL,
    bomber_id integer NOT NULL,
    hp integer DEFAULT 0 NOT NULL,
    dmg integer DEFAULT 0 NOT NULL,
    speed integer DEFAULT 0 NOT NULL,
    range integer DEFAULT 0 NOT NULL,
    bomb integer DEFAULT 0 NOT NULL
);


--
-- Name: user_booster; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_booster (
    id integer NOT NULL,
    uid integer NOT NULL,
    type smallint NOT NULL,
    item_id smallint,
    create_date timestamp without time zone DEFAULT (now() AT TIME ZONE 'utc'::text),
    status integer
);


--
-- Name: user_booster_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_booster_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_booster_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_booster_id_seq OWNED BY public.user_booster.id;


--
-- Name: user_buy_gem_transaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_buy_gem_transaction (
    bill_token character varying(255) NOT NULL,
    uid integer NOT NULL,
    product_id character varying(255) NOT NULL,
    is_special_offer boolean DEFAULT false,
    date date DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gems_receive integer,
    is_test boolean,
    region character varying(10)
);


--
-- Name: user_club; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_club (
    id bigint NOT NULL,
    name character varying(50),
    link character varying(50),
    channel_id character varying(20),
    avatar_name character varying(20),
    create_at timestamp with time zone
);


--
-- Name: user_club_bid_date; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_club_bid_date (
    club_id bigint NOT NULL,
    bid_point integer,
    date date DEFAULT CURRENT_DATE NOT NULL
);


--
-- Name: user_club_bid_date_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_club_bid_date_v2 (
    club_id integer NOT NULL,
    bid_point integer,
    date date DEFAULT CURRENT_DATE NOT NULL
);


--
-- Name: user_club_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_club_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_club_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_club_id_seq OWNED BY public.user_club.id;


--
-- Name: user_club_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_club_members (
    uid integer NOT NULL,
    club_id bigint NOT NULL,
    point double precision DEFAULT 0.0,
    is_leave integer DEFAULT 0,
    season integer NOT NULL
);


--
-- Name: user_club_members_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_club_members_v2 (
    uid integer NOT NULL,
    club_id integer NOT NULL,
    point double precision DEFAULT 0.0,
    is_leave integer DEFAULT 0,
    season integer NOT NULL
);


--
-- Name: user_club_point; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_club_point (
    club_id bigint,
    season integer,
    point double precision DEFAULT 0.0
);


--
-- Name: user_club_point_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_club_point_v2 (
    club_id integer NOT NULL,
    season integer NOT NULL,
    point double precision DEFAULT 0.0
);


--
-- Name: user_club_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_club_v2 (
    id integer NOT NULL,
    id_telegram bigint,
    name character varying(50) NOT NULL,
    type character varying(20) NOT NULL,
    link character varying(50),
    avatar_name character varying(20),
    create_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: user_club_v2_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_club_v2 ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_club_v2_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_config (
    uid integer NOT NULL,
    gacha_chest_slots text,
    free_reward_config text,
    misc_configs_json text,
    last_claim_subscription timestamp without time zone,
    is_no_ads boolean,
    is_received_first_chest_skip_time boolean,
    total_costume_preset_slot integer DEFAULT 0,
    is_received_tutorial_reward boolean DEFAULT false
);


--
-- Name: user_costume_preset; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_costume_preset (
    id integer NOT NULL,
    uid integer,
    name character varying(30),
    original_name character varying(20),
    bomber_id integer,
    skin_ids text DEFAULT '[]'::text
);


--
-- Name: user_costume_preset_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_costume_preset_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_costume_preset_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_costume_preset_id_seq OWNED BY public.user_costume_preset.id;


--
-- Name: user_create_rock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_create_rock (
    uid integer,
    tx character varying(20),
    heroes jsonb,
    rock_amount real,
    network character varying(10),
    "timestamp" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status character varying(10)
);


--
-- Name: user_daily_check_in; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_daily_check_in (
    uid integer NOT NULL,
    total_check_in integer NOT NULL,
    reward double precision NOT NULL,
    tx_hash character varying NOT NULL,
    data_type character varying(20) NOT NULL,
    created_at timestamp with time zone
);


--
-- Name: user_daily_task; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_daily_task (
    uid integer NOT NULL,
    task_id character varying(20),
    progress character varying(20),
    claimed character varying(50),
    final_reward_claimed integer,
    reward character varying(100),
    date date NOT NULL
);


--
-- Name: COLUMN user_daily_task.reward; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_daily_task.reward IS 'Phần thưởng có dạng item_id: quantity: expiration_time';


--
-- Name: user_energy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_energy (
    user_id integer NOT NULL,
    balance integer DEFAULT 0 NOT NULL,
    last_sync_timestamp timestamp without time zone
);


--
-- Name: user_energy_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_energy_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_energy_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_energy_user_id_seq OWNED BY public.user_energy.user_id;


--
-- Name: user_gacha_chest; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_gacha_chest (
    chest_id integer NOT NULL,
    uid integer,
    chest_type integer,
    open_time bigint,
    deleted integer DEFAULT 0 NOT NULL
);


--
-- Name: user_gacha_chest_chest_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_gacha_chest ALTER COLUMN chest_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_gacha_chest_chest_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_gacha_chest_slot_buy_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_gacha_chest_slot_buy_logs (
    uid integer NOT NULL,
    "time" timestamp(6) without time zone NOT NULL,
    slot_id integer,
    price double precision,
    type character varying(10),
    receipt json
);


--
-- Name: user_hash; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_hash (
    user_id integer NOT NULL,
    hash character varying(50)
);


--
-- Name: user_hash_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_hash_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_hash_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_hash_user_id_seq OWNED BY public.user_hash.user_id;


--
-- Name: user_hero_house_rent; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_hero_house_rent (
    hero_id integer NOT NULL,
    house_id integer NOT NULL,
    is_rest boolean
);


--
-- Name: user_house; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_house (
    uid integer,
    gen_house_id character varying(255),
    house_id integer NOT NULL,
    rarity integer,
    recovery integer,
    max_bomber integer,
    active integer,
    sync_date timestamp without time zone,
    type character varying(255) NOT NULL,
    create_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    end_time_rent timestamp with time zone
);


--
-- Name: user_house_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_house_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_house_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_house_id_seq OWNED BY public.user_house.house_id;


--
-- Name: user_house_old_season; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_house_old_season (
    uid integer,
    gen_house_id character varying(255),
    house_id integer NOT NULL,
    rarity integer,
    recovery integer,
    max_bomber integer,
    active integer,
    sync_date timestamp without time zone,
    type character varying(255) NOT NULL,
    create_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: user_iap_pack; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_iap_pack (
    uid integer,
    product_id character varying(255),
    sale_end_date timestamp without time zone
);


--
-- Name: user_id_user_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_id_user_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_id_user_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_id_user_seq OWNED BY public."user".id_user;


--
-- Name: user_item_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_item_status (
    uid integer NOT NULL,
    id integer NOT NULL,
    item_id smallint NOT NULL,
    status smallint NOT NULL,
    expiry_date timestamp without time zone,
    source character varying
);


--
-- Name: COLUMN user_item_status.source; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_item_status.source IS 'Nguồn gốc của item';


--
-- Name: user_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_link (
    user_id integer,
    linked_user_id integer
);


--
-- Name: user_mail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_mail (
    id integer NOT NULL,
    uid integer,
    is_deleted boolean DEFAULT false,
    is_read boolean DEFAULT false,
    is_claim boolean DEFAULT false,
    create_date timestamp without time zone,
    has_attach boolean DEFAULT false
);


--
-- Name: user_mail_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_mail_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_mail_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_mail_id_seq OWNED BY public.user_mail.id;


--
-- Name: user_mail_item_attach; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_mail_item_attach (
    mail_id integer,
    item_id character varying(255),
    quantity integer,
    expiration_after timestamp without time zone,
    is_lock boolean DEFAULT false
);


--
-- Name: user_market_selling_v3; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_market_selling_v3 (
    id integer NOT NULL,
    seller_uid integer NOT NULL,
    type smallint NOT NULL,
    item_id smallint NOT NULL,
    price double precision NOT NULL,
    reward_type character varying(30) NOT NULL,
    expiration_after integer DEFAULT 0,
    modify_date timestamp with time zone DEFAULT (now() AT TIME ZONE 'utc'::text) NOT NULL
);


--
-- Name: user_marketplace; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_marketplace (
    id integer NOT NULL,
    type smallint NOT NULL,
    item_id smallint NOT NULL,
    stats smallint DEFAULT 0 NOT NULL,
    price double precision NOT NULL,
    reward_type smallint NOT NULL,
    status smallint DEFAULT 0 NOT NULL,
    uid_creator integer NOT NULL,
    modify_date timestamp without time zone DEFAULT (now() AT TIME ZONE 'utc'::text) NOT NULL,
    unit_price double precision,
    quantity integer,
    list_id text DEFAULT '[]'::text NOT NULL,
    expiration_after integer
);


--
-- Name: COLUMN user_marketplace.expiration_after; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_marketplace.expiration_after IS 'tính theo second (-1 cho những item vĩnh viễn)';


--
-- Name: user_marketplace_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_marketplace ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_marketplace_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_marketplace_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_marketplace_status (
    uid integer NOT NULL,
    id integer NOT NULL,
    type smallint NOT NULL,
    item_id smallint NOT NULL,
    reward_type smallint,
    price double precision,
    status smallint NOT NULL
);


--
-- Name: user_material; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_material (
    uid integer NOT NULL,
    item_id integer NOT NULL,
    quantity integer,
    modify_date timestamp without time zone
);


--
-- Name: user_mission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_mission (
    uid integer NOT NULL,
    date date,
    type character varying(30),
    is_daily_mission boolean DEFAULT true,
    mission_code character varying(36) NOT NULL,
    number_mission integer,
    completed_mission integer,
    is_received_reward integer,
    modify_date timestamp without time zone,
    rewards_received json
);


--
-- Name: user_network_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_network_mapping (
    id_user integer,
    user_name text,
    id_user_ron integer,
    user_name_ron character varying,
    id_user_bas integer,
    user_name_bas character varying,
    id_user_vic integer,
    user_name_vic character varying
);


--
-- Name: user_not_have_bsc_account; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_not_have_bsc_account (
    user_name text
);


--
-- Name: user_not_have_bsc_account2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_not_have_bsc_account2 (
    username character varying(255),
    id integer NOT NULL
);


--
-- Name: user_not_have_bsc_account2_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_not_have_bsc_account2_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_not_have_bsc_account2_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_not_have_bsc_account2_id_seq OWNED BY public.user_not_have_bsc_account2.id;


--
-- Name: user_old_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_old_item (
    uid integer NOT NULL,
    item_ids json
);


--
-- Name: user_on_boarding; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_on_boarding (
    uid integer NOT NULL,
    step integer,
    claimed integer
);


--
-- Name: COLUMN user_on_boarding.step; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_on_boarding.step IS 'Tiến trình hiện tại của user';


--
-- Name: COLUMN user_on_boarding.claimed; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_on_boarding.claimed IS 'Phần thưởng đã claim';


--
-- Name: user_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_permissions (
    create_room boolean,
    reset_data boolean,
    story_immortal boolean,
    story_one_hit boolean,
    view_pvp_dashboard boolean,
    user_id integer
);


--
-- Name: user_pvp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_pvp (
    uid integer NOT NULL,
    last_played_hero_id bigint DEFAULT (- (1)::bigint),
    last_bet integer DEFAULT 0
);


--
-- Name: user_pvp_hero_energy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_pvp_hero_energy (
    uid integer NOT NULL,
    heroes text
);

--
-- Name: user_pvp_rank_reward_ss_1; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_pvp_rank_reward_ss_1 (
    username character(42),
    rank integer,
    is_claim smallint DEFAULT 0 NOT NULL,
    bcoin double precision,
    box_hero smallint,
    booster_items json,
    total_match integer DEFAULT 0 NOT NULL,
    uid integer,
    reward text
);


--
-- Name: user_pvp_rank_reward_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_pvp_rank_reward_template (
    username character(42),
    rank integer,
    is_claim smallint DEFAULT 0 NOT NULL,
    bcoin double precision,
    box_hero smallint,
    booster_items json,
    total_match integer DEFAULT 0 NOT NULL,
    uid integer,
    reward text
);

--
-- Name: user_pvp_rank_ss_0; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_pvp_rank_ss_0 (
    rank integer,
    point integer DEFAULT 0 NOT NULL,
    total_match integer DEFAULT 0 NOT NULL,
    win_match integer DEFAULT 0 NOT NULL,
    uid integer NOT NULL,
    user_type integer,
    matches_in_current_date integer DEFAULT 0 NOT NULL
);

--
-- Name: user_pvp_rank_ss_0_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_pvp_rank_ss_0_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: user_pvp_rank_ss_1; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_pvp_rank_ss_1 (
    rank integer,
    point integer DEFAULT 0 NOT NULL,
    total_match integer DEFAULT 0 NOT NULL,
    win_match integer DEFAULT 0 NOT NULL,
    uid integer NOT NULL,
    user_type integer,
    matches_in_current_date integer DEFAULT 0 NOT NULL
);

--
-- Name: user_pvp_rank_ss_1_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_pvp_rank_ss_1_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: user_pvp_rank_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_pvp_rank_template (
    rank integer,
    point integer DEFAULT 0 NOT NULL,
    total_match integer DEFAULT 0 NOT NULL,
    win_match integer DEFAULT 0 NOT NULL,
    uid integer NOT NULL,
    user_type integer,
    matches_in_current_date integer DEFAULT 0 NOT NULL
);


--
-- Name: user_ranking_coin; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_ranking_coin (
    uid integer NOT NULL,
    coin double precision,
    network character varying(10) NOT NULL,
    season integer NOT NULL
);


--
-- Name: user_ranking_coin_backup; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_ranking_coin_backup (
    uid integer NOT NULL,
    coin double precision NOT NULL,
    network character varying(10) NOT NULL,
    season integer NOT NULL
);


--
-- Name: user_ron_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_ron_transactions (
    id integer NOT NULL,
    uid integer NOT NULL,
    transaction_type character varying(30),
    amount double precision,
    token_name character varying(20),
    tx_hash character varying(255),
    created_at timestamp with time zone
);


--
-- Name: user_ron_transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_ron_transactions ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_ron_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_skin; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_skin (
    id integer NOT NULL,
    uid integer NOT NULL,
    type smallint NOT NULL,
    status smallint DEFAULT 0 NOT NULL,
    item_id smallint,
    create_date timestamp without time zone DEFAULT (now() AT TIME ZONE 'utc'::text),
    expiration_after bigint
);


--
-- Name: user_skin_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_skin_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_skin_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_skin_id_seq OWNED BY public.user_skin.id;


--
-- Name: user_sol_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_sol_transactions (
    id integer NOT NULL,
    uid integer NOT NULL,
    transaction_type character varying(30),
    amount double precision,
    token_name character varying(20),
    tx_hash character varying(255),
    created_at timestamp with time zone
);


--
-- Name: user_sol_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_sol_transactions ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_sol_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_subscription; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_subscription (
    uid integer NOT NULL,
    product_id text,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    last_modify timestamp without time zone,
    token text,
    state text
);


--
-- Name: user_swap_gem; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_swap_gem (
    uid integer,
    token_swap character varying(20),
    amount real,
    "time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    network character varying(20),
    unit_price real
);


--
-- Name: user_ton_completed_tasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_ton_completed_tasks (
    uid integer NOT NULL,
    task_id integer NOT NULL,
    claimed integer DEFAULT 0,
    claim_time timestamp with time zone,
    complete_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: user_ton_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_ton_transactions (
    id integer NOT NULL,
    uid integer NOT NULL,
    transaction_type character varying(30),
    amount double precision,
    token_name character varying(20),
    tx_hash character varying(255),
    created_at timestamp with time zone
);


--
-- Name: COLUMN user_ton_transactions.transaction_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_ton_transactions.transaction_type IS 'Deposit';


--
-- Name: COLUMN user_ton_transactions.token_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_ton_transactions.token_name IS 'BCOIN';


--
-- Name: user_ton_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_ton_transactions ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_ton_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_total_bcoin_deposited; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_total_bcoin_deposited (
    uid integer NOT NULL,
    bcoin_deposited double precision DEFAULT 0 NOT NULL,
    sen_deposited double precision DEFAULT 0 NOT NULL,
    modify_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    type character varying(20) NOT NULL
);


--
-- Name: COLUMN user_total_bcoin_deposited.type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_total_bcoin_deposited.type IS 'network type';


--
-- Name: user_vic_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_vic_transactions (
    id integer NOT NULL,
    uid integer NOT NULL,
    transaction_type character varying(30),
    amount double precision,
    token_name character varying(20),
    tx_hash character varying(255),
    created_at timestamp with time zone
);


--
-- Name: user_vic_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_vic_transactions ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_vic_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_white_list; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_white_list (
    user_name character varying(255),
    active smallint DEFAULT 1
);


--
-- Name: whitelist; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.whitelist (
    user_name character varying(50) NOT NULL,
    active boolean DEFAULT true NOT NULL
);


--
-- Name: logs_th_mode_v2_races race_id; Type: DEFAULT; Schema: logs; Owner: -
--

ALTER TABLE ONLY logs.logs_th_mode_v2_races ALTER COLUMN race_id SET DEFAULT nextval('logs.logs_th_mode_v2_races_race_id_seq'::regclass);


--
-- Name: config_adventure_mode_entity_creator id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_adventure_mode_entity_creator ALTER COLUMN id SET DEFAULT nextval('public.config_adventure_mode_entity_creator_id_seq'::regclass);


--
-- Name: config_adventure_mode_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_adventure_mode_items ALTER COLUMN id SET DEFAULT nextval('public.config_adventure_mode_items_id_seq'::regclass);


--
-- Name: config_adventure_mode_level_strategy id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_adventure_mode_level_strategy ALTER COLUMN id SET DEFAULT nextval('public.config_adventure_mode_level_strategy_id_seq'::regclass);


--
-- Name: config_block id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_block ALTER COLUMN id SET DEFAULT nextval('public.config_block_id_seq'::regclass);


--
-- Name: config_block_drop_by_day id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_block_drop_by_day ALTER COLUMN id SET DEFAULT nextval('public.config_block_drop_by_day_id_seq'::regclass);


--
-- Name: config_event id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_event ALTER COLUMN id SET DEFAULT nextval('public.config_event_id_seq'::regclass);


--
-- Name: config_gacha_chest_slot id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_gacha_chest_slot ALTER COLUMN id SET DEFAULT nextval('public.config_gacha_chest_slot_id_seq'::regclass);


--
-- Name: config_hero_repair_shield id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_hero_repair_shield ALTER COLUMN id SET DEFAULT nextval('public.config_hero_repair_shield_id_seq'::regclass);


--
-- Name: config_package_auto_mine id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_package_auto_mine ALTER COLUMN id SET DEFAULT nextval('public.config_package_auto_mine_id_seq'::regclass);


--
-- Name: config_stake_vip_reward id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_stake_vip_reward ALTER COLUMN id SET DEFAULT nextval('public.config_stake_vip_reward_id_seq'::regclass);


--
-- Name: config_ton_tasks id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_ton_tasks ALTER COLUMN id SET DEFAULT nextval('public.config_ton_tasks_id_seq'::regclass);


--
-- Name: log_buy_chest_gacha id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.log_buy_chest_gacha ALTER COLUMN id SET DEFAULT nextval('public.log_buy_chest_gacha_id_seq'::regclass);


--
-- Name: user id_user; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."user" ALTER COLUMN id_user SET DEFAULT nextval('public.user_id_user_seq'::regclass);


--
-- Name: user_activity_marketplace id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_activity_marketplace ALTER COLUMN id SET DEFAULT nextval('public.user_activity_marketplace_id_seq'::regclass);


--
-- Name: user_booster id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_booster ALTER COLUMN id SET DEFAULT nextval('public.user_booster_id_seq'::regclass);


--
-- Name: user_club id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_club ALTER COLUMN id SET DEFAULT nextval('public.user_club_id_seq'::regclass);


--
-- Name: user_costume_preset id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_costume_preset ALTER COLUMN id SET DEFAULT nextval('public.user_costume_preset_id_seq'::regclass);


--
-- Name: user_energy user_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_energy ALTER COLUMN user_id SET DEFAULT nextval('public.user_energy_user_id_seq'::regclass);


--
-- Name: user_hash user_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_hash ALTER COLUMN user_id SET DEFAULT nextval('public.user_hash_user_id_seq'::regclass);


--
-- Name: user_mail id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_mail ALTER COLUMN id SET DEFAULT nextval('public.user_mail_id_seq'::regclass);


--
-- Name: user_not_have_bsc_account2 id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_not_have_bsc_account2 ALTER COLUMN id SET DEFAULT nextval('public.user_not_have_bsc_account2_id_seq'::regclass);


--
-- Name: user_skin id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_skin ALTER COLUMN id SET DEFAULT nextval('public.user_skin_id_seq'::regclass);



--
-- Name: logs_th_mode_v2_races logs_th_mode_v2_races_pk; Type: CONSTRAINT; Schema: logs; Owner: -
--

ALTER TABLE ONLY logs.logs_th_mode_v2_races
    ADD CONSTRAINT logs_th_mode_v2_races_pk PRIMARY KEY (race_id);


--
-- Name: logs_th_mode_v2_template logs_th_mode_v2_template_pk; Type: CONSTRAINT; Schema: logs; Owner: -
--

ALTER TABLE ONLY logs.logs_th_mode_v2_template
    ADD CONSTRAINT logs_th_mode_v2_template_pk PRIMARY KEY (race_id, uid, hero_id);


--
-- Name: bombcrypto_hero_traditional_config bombcrypto_hero_traditional_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bombcrypto_hero_traditional_config
    ADD CONSTRAINT bombcrypto_hero_traditional_config_pkey PRIMARY KEY (item_id);


--
-- Name: bombcrypto_lucky_ticket_daily_mission bombcrypto_lucky_ticket_daily_mission_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bombcrypto_lucky_ticket_daily_mission
    ADD CONSTRAINT bombcrypto_lucky_ticket_daily_mission_pk UNIQUE (mission, times);


--
-- Name: bombcrypto_marketplace_product bombcrypto_marketplace_product_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bombcrypto_marketplace_product
    ADD CONSTRAINT bombcrypto_marketplace_product_pkey PRIMARY KEY (id);


--
-- Name: bombcrypto_stake_vip_reward bombcrypto_stake_vip_reward_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bombcrypto_stake_vip_reward
    ADD CONSTRAINT bombcrypto_stake_vip_reward_pkey PRIMARY KEY (id);


--
-- Name: config_adventure_mode_entity_creator config_adventure_mode_entity_creator_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_adventure_mode_entity_creator
    ADD CONSTRAINT config_adventure_mode_entity_creator_pkey PRIMARY KEY (id);


--
-- Name: config_adventure_mode_items config_adventure_mode_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_adventure_mode_items
    ADD CONSTRAINT config_adventure_mode_items_pkey PRIMARY KEY (id);


--
-- Name: config_adventure_mode_level_strategy config_adventure_mode_level_strategy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_adventure_mode_level_strategy
    ADD CONSTRAINT config_adventure_mode_level_strategy_pkey PRIMARY KEY (id);


--
-- Name: config_block_drop_by_day config_block_drop_by_day_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_block_drop_by_day
    ADD CONSTRAINT config_block_drop_by_day_pkey PRIMARY KEY (id);


--
-- Name: config_block config_block_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_block
    ADD CONSTRAINT config_block_pk PRIMARY KEY (id, type);


--
-- Name: config_bomber_ability config_bomber_ability_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_bomber_ability
    ADD CONSTRAINT config_bomber_ability_pkey PRIMARY KEY (ability);


--
-- Name: config_coin_ranking_season config_coin_ranking_season_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_coin_ranking_season
    ADD CONSTRAINT config_coin_ranking_season_pkey PRIMARY KEY (id);


--
-- Name: config_daily_mission config_daily_mission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_daily_mission
    ADD CONSTRAINT config_daily_mission_pkey PRIMARY KEY (code);


--
-- Name: config_event config_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_event
    ADD CONSTRAINT config_event_pkey PRIMARY KEY (id);


--
-- Name: config_free_reward_by_ads config_free_reward_by_ads_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_free_reward_by_ads
    ADD CONSTRAINT config_free_reward_by_ads_pk PRIMARY KEY (reward_type);


--
-- Name: config_gacha_chest_items config_gacha_chest_items_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_gacha_chest_items
    ADD CONSTRAINT config_gacha_chest_items_pk PRIMARY KEY (item_id, chest_type, min);


--
-- Name: config_gacha_chest config_gacha_chest_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_gacha_chest
    ADD CONSTRAINT config_gacha_chest_pk PRIMARY KEY (type);


--
-- Name: config_gacha_chest_slot config_gacha_chest_slot_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_gacha_chest_slot
    ADD CONSTRAINT config_gacha_chest_slot_pkey PRIMARY KEY (id);


--
-- Name: config_hero_repair_shield config_hero_repair_shield_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_hero_repair_shield
    ADD CONSTRAINT config_hero_repair_shield_pkey PRIMARY KEY (id);


--
-- Name: config_hero_upgrade_power config_hero_upgrade_power_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_hero_upgrade_power
    ADD CONSTRAINT config_hero_upgrade_power_pkey PRIMARY KEY (rare);


--
-- Name: config_iap_gold_shop config_iap_gold_shop_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_iap_gold_shop
    ADD CONSTRAINT config_iap_gold_shop_pkey PRIMARY KEY (item_id);


--
-- Name: config_iap_shop config_iap_shop_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_iap_shop
    ADD CONSTRAINT config_iap_shop_pkey PRIMARY KEY (product_id, type);


--
-- Name: config_item_market_v3 config_item_market_v3_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_item_market_v3
    ADD CONSTRAINT config_item_market_v3_pkey PRIMARY KEY (id);


--
-- Name: config_item config_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_item
    ADD CONSTRAINT config_item_pkey PRIMARY KEY (id);


--
-- Name: config_lucky_wheel_reward config_lucky_wheel_reward_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_lucky_wheel_reward
    ADD CONSTRAINT config_lucky_wheel_reward_pk PRIMARY KEY (code);


--
-- Name: config_message config_message_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_message
    ADD CONSTRAINT config_message_pkey PRIMARY KEY (code);


--
-- Name: config_min_stake_hero config_min_stake_hero_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_min_stake_hero
    ADD CONSTRAINT config_min_stake_hero_pk PRIMARY KEY (rarity);


--
-- Name: config_mission config_mission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_mission
    ADD CONSTRAINT config_mission_pkey PRIMARY KEY (code);


--
-- Name: config_mystery_box_item config_mystery_box_item_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_mystery_box_item
    ADD CONSTRAINT config_mystery_box_item_pk PRIMARY KEY (item_id);


--
-- Name: config_new_user_gift config_new_user_gift_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_new_user_gift
    ADD CONSTRAINT config_new_user_gift_pk PRIMARY KEY (item_id);


--
-- Name: config_offline_reward config_offline_reward_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_offline_reward
    ADD CONSTRAINT config_offline_reward_pk PRIMARY KEY (offline_hours);


--
-- Name: config_package_auto_mine config_package_auto_mine_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_package_auto_mine
    ADD CONSTRAINT config_package_auto_mine_pk PRIMARY KEY (id);


--
-- Name: config config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config
    ADD CONSTRAINT config_pkey PRIMARY KEY (key);


--
-- Name: config_pvp_ranking config_pvp_ranking_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_pvp_ranking
    ADD CONSTRAINT config_pvp_ranking_pk PRIMARY KEY (bomb_rank);


--
-- Name: pvp_tournament_backup config_pvp_tournament_backup_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pvp_tournament_backup
    ADD CONSTRAINT config_pvp_tournament_backup_pkey PRIMARY KEY (id);


--
-- Name: pvp_tournament config_pvp_tournament_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pvp_tournament
    ADD CONSTRAINT config_pvp_tournament_pkey PRIMARY KEY (id);


--
-- Name: config_ranking_season config_ranking_season_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_ranking_season
    ADD CONSTRAINT config_ranking_season_pkey PRIMARY KEY (id);


--
-- Name: config_reset_shield_balancer config_reset_shield_balancer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_reset_shield_balancer
    ADD CONSTRAINT config_reset_shield_balancer_pkey PRIMARY KEY (rare);


--
-- Name: config_revive_hero_cost config_revive_hero_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_revive_hero_cost
    ADD CONSTRAINT config_revive_hero_pk PRIMARY KEY (times);


--
-- Name: config_reward_level_th_v2 config_reward_level_th_v2_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_reward_level_th_v2
    ADD CONSTRAINT config_reward_level_th_v2_pk PRIMARY KEY (level);


--
-- Name: config_reward_pool_th_v2 config_reward_pool_th_v2_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_reward_pool_th_v2
    ADD CONSTRAINT config_reward_pool_th_v2_pk PRIMARY KEY (pool_id, type);


--
-- Name: config_stake_vip_reward config_stake_vip_reward_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_stake_vip_reward
    ADD CONSTRAINT config_stake_vip_reward_pkey PRIMARY KEY (id);


--
-- Name: config_swap_token_realtime config_swap_token_realtime_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_swap_token_realtime
    ADD CONSTRAINT config_swap_token_realtime_pk PRIMARY KEY (key);


--
-- Name: config_th_mode config_th_mode_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_th_mode
    ADD CONSTRAINT config_th_mode_pk PRIMARY KEY (key, network);


--
-- Name: config_th_mode_v2 config_th_mode_v2_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_th_mode_v2
    ADD CONSTRAINT config_th_mode_v2_pk PRIMARY KEY (key, type);


--
-- Name: config_ton_tasks config_ton_tasks_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_ton_tasks
    ADD CONSTRAINT config_ton_tasks_pk PRIMARY KEY (id);


--
-- Name: config_upgrade_crystal config_upgrade_crystal_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_upgrade_crystal
    ADD CONSTRAINT config_upgrade_crystal_pk PRIMARY KEY (source_item_id, target_item_id);


--
-- Name: config_upgrade_hero_tr config_upgrade_hero_tr_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_upgrade_hero_tr
    ADD CONSTRAINT config_upgrade_hero_tr_pk PRIMARY KEY (index, type);


--
-- Name: config_hero_upgrade_level config_upgrade_hero_traditional_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_hero_upgrade_level
    ADD CONSTRAINT config_upgrade_hero_traditional_pkey PRIMARY KEY (level);


--
-- Name: daily_task_config daily_task_config_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_task_config
    ADD CONSTRAINT daily_task_config_pk PRIMARY KEY (id);


--
-- Name: game_config game_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_config
    ADD CONSTRAINT game_config_pkey PRIMARY KEY (key);



--
-- Name: log_block_recieve_reward_template log_block_recieve_reward_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.log_block_recieve_reward_template
    ADD CONSTRAINT log_block_recieve_reward_template_pkey PRIMARY KEY (id);


--
-- Name: log_buy_chest_gacha log_buy_chest_gacha_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.log_buy_chest_gacha
    ADD CONSTRAINT log_buy_chest_gacha_pkey PRIMARY KEY (id);


--
-- Name: user_daily_check_in pk_user_daily_check_in; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_daily_check_in
    ADD CONSTRAINT pk_user_daily_check_in PRIMARY KEY (uid, tx_hash);


--
-- Name: pvp_fixture_matches pvp_fixture_matches_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pvp_fixture_matches
    ADD CONSTRAINT pvp_fixture_matches_pk PRIMARY KEY (id);


--
-- Name: user_iap_pack uid_product_id_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_iap_pack
    ADD CONSTRAINT uid_product_id_unique UNIQUE (uid, product_id);


--
-- Name: schedule_status unique_schedule; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedule_status
    ADD CONSTRAINT unique_schedule UNIQUE (schedule);


--
-- Name: config_adventure_mode_items unique_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_adventure_mode_items
    ADD CONSTRAINT unique_type UNIQUE (type);


--
-- Name: user_activity_marketplace user_activity_marketplace_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_activity_marketplace
    ADD CONSTRAINT user_activity_marketplace_pkey PRIMARY KEY (id);


--
-- Name: user_adventure_mode user_adventure_mode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_adventure_mode
    ADD CONSTRAINT user_adventure_mode_pkey PRIMARY KEY (uid);


--
-- Name: user_auto_mine user_auto_mine_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_auto_mine
    ADD CONSTRAINT user_auto_mine_pk PRIMARY KEY (uid, type);


--
-- Name: user_bas_transactions user_bas_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_bas_transactions
    ADD CONSTRAINT user_bas_transactions_pkey PRIMARY KEY (id);


--
-- Name: user_bcoin_transactions user_bcoin_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_bcoin_transactions
    ADD CONSTRAINT user_bcoin_transactions_pkey PRIMARY KEY (id);


--
-- Name: user_block_map_pve user_block_map_pve_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_block_map_pve
    ADD CONSTRAINT user_block_map_pve_pkey PRIMARY KEY (uid, mode, type);


--
-- Name: user_block_reward_gift user_block_reward_gift_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_block_reward_gift
    ADD CONSTRAINT user_block_reward_gift_pk PRIMARY KEY (uid, modify_time);


--
-- Name: user_block_reward user_block_reward_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_block_reward
    ADD CONSTRAINT user_block_reward_pkey PRIMARY KEY (uid, reward_type, type);


--
-- Name: user_bomber_lock user_bomber_lock_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_bomber_lock
    ADD CONSTRAINT user_bomber_lock_pk PRIMARY KEY (bomber_id, data_type, hero_type);


--
-- Name: user_bomber user_bomber_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_bomber
    ADD CONSTRAINT user_bomber_pk PRIMARY KEY (bomber_id, type, data_type);


--
-- Name: user_bomber_old_season user_bomber_ton_season_1_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_bomber_old_season
    ADD CONSTRAINT user_bomber_ton_season_1_pk PRIMARY KEY (bomber_id, type, data_type);


--
-- Name: user_bomber_upgraded user_bomber_upgraded_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_bomber_upgraded
    ADD CONSTRAINT user_bomber_upgraded_pkey PRIMARY KEY (uid, bomber_id);


--
-- Name: user_booster user_booster_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_booster
    ADD CONSTRAINT user_booster_pkey PRIMARY KEY (id);


--
-- Name: user_auto_mine_buy_logs user_buy_auto_mine_logs_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_auto_mine_buy_logs
    ADD CONSTRAINT user_buy_auto_mine_logs_pk UNIQUE (uid, "time");


--
-- Name: user_buy_gem_transaction user_buy_gem_transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_buy_gem_transaction
    ADD CONSTRAINT user_buy_gem_transaction_pkey PRIMARY KEY (bill_token);


--
-- Name: user_club_bid_date user_club_bid_date_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_club_bid_date
    ADD CONSTRAINT user_club_bid_date_pk PRIMARY KEY (club_id, date);


--
-- Name: user_club_bid_date_v2 user_club_bid_date_v2_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_club_bid_date_v2
    ADD CONSTRAINT user_club_bid_date_v2_pk PRIMARY KEY (club_id, date);


--
-- Name: user_club_members user_club_members_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_club_members
    ADD CONSTRAINT user_club_members_pk PRIMARY KEY (uid, club_id, season);


--
-- Name: user_club_members_v2 user_club_members_v2_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_club_members_v2
    ADD CONSTRAINT user_club_members_v2_pk PRIMARY KEY (uid, club_id, season);


--
-- Name: user_club user_club_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_club
    ADD CONSTRAINT user_club_pk PRIMARY KEY (id);


--
-- Name: user_club_point_v2 user_club_point_v2_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_club_point_v2
    ADD CONSTRAINT user_club_point_v2_pk PRIMARY KEY (club_id, season);


--
-- Name: user_club_v2 user_club_v2_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_club_v2
    ADD CONSTRAINT user_club_v2_pkey PRIMARY KEY (id);


--
-- Name: user_config user_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_config
    ADD CONSTRAINT user_config_pkey PRIMARY KEY (uid);


--
-- Name: user_costume_preset user_costume_preset_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_costume_preset
    ADD CONSTRAINT user_costume_preset_pkey PRIMARY KEY (id);


--
-- Name: user_create_rock user_create_rock_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_create_rock
    ADD CONSTRAINT user_create_rock_pk UNIQUE (tx, uid);


--
-- Name: user_daily_task user_daily_task_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_daily_task
    ADD CONSTRAINT user_daily_task_pk UNIQUE (uid, date);


--
-- Name: user user_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_email_key UNIQUE (email);


--
-- Name: user_energy user_energy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_energy
    ADD CONSTRAINT user_energy_pkey PRIMARY KEY (user_id);


--
-- Name: user_gacha_chest user_gacha_chest_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_gacha_chest
    ADD CONSTRAINT user_gacha_chest_pkey PRIMARY KEY (chest_id);


--
-- Name: user_gacha_chest_slot_buy_logs user_gacha_chest_slot_buy_logs_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_gacha_chest_slot_buy_logs
    ADD CONSTRAINT user_gacha_chest_slot_buy_logs_pk UNIQUE (uid, "time");


--
-- Name: user_hash user_hash_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_hash
    ADD CONSTRAINT user_hash_pkey PRIMARY KEY (user_id);


--
-- Name: user_hero_house_rent user_hero_house_rent_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_hero_house_rent
    ADD CONSTRAINT user_hero_house_rent_pk PRIMARY KEY (house_id, hero_id);


--
-- Name: user_house_old_season user_house_old_season_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_house_old_season
    ADD CONSTRAINT user_house_old_season_pkey PRIMARY KEY (house_id, type);


--
-- Name: user_house user_house_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_house
    ADD CONSTRAINT user_house_pkey PRIMARY KEY (house_id, type);


--
-- Name: user_item_status user_item_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_item_status
    ADD CONSTRAINT user_item_status_pkey PRIMARY KEY (id, item_id);


--
-- Name: user_mail user_mail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_mail
    ADD CONSTRAINT user_mail_pkey PRIMARY KEY (id);


--
-- Name: user_market_selling_v3 user_market_selling_v3_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_market_selling_v3
    ADD CONSTRAINT user_market_selling_v3_pkey PRIMARY KEY (id);


--
-- Name: user_marketplace user_marketplace_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_marketplace
    ADD CONSTRAINT user_marketplace_pkey PRIMARY KEY (id, item_id);


--
-- Name: user_marketplace_status user_marketplace_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_marketplace_status
    ADD CONSTRAINT user_marketplace_status_pkey PRIMARY KEY (id, item_id);


--
-- Name: user_material user_material_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_material
    ADD CONSTRAINT user_material_pkey PRIMARY KEY (uid, item_id);


--
-- Name: user_mission user_mission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_mission
    ADD CONSTRAINT user_mission_pkey PRIMARY KEY (uid, mission_code);


--
-- Name: user_old_item user_old_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_old_item
    ADD CONSTRAINT user_old_item_pkey PRIMARY KEY (uid);


--
-- Name: user_on_boarding user_on_boarding_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_on_boarding
    ADD CONSTRAINT user_on_boarding_pk UNIQUE (uid);


--
-- Name: user user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (id_user);


--
-- Name: user_pvp_hero_energy user_pvp_hero_energy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_pvp_hero_energy
    ADD CONSTRAINT user_pvp_hero_energy_pkey PRIMARY KEY (uid);


--
-- Name: user_pvp user_pvp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_pvp
    ADD CONSTRAINT user_pvp_pkey PRIMARY KEY (uid);

--
-- Name: user_pvp_rank_template user_pvp_rank_template_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_pvp_rank_template
    ADD CONSTRAINT user_pvp_rank_template_pk PRIMARY KEY (uid);


--
-- Name: user_ranking_coin_backup user_ranking_coin_backup_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_ranking_coin_backup
    ADD CONSTRAINT user_ranking_coin_backup_pkey PRIMARY KEY (uid, network, season);


--
-- Name: user_ranking_coin user_ranking_coin_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_ranking_coin
    ADD CONSTRAINT user_ranking_coin_pk PRIMARY KEY (uid, network, season);


--
-- Name: user_ron_transactions user_ron_transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_ron_transactions
    ADD CONSTRAINT user_ron_transaction_pkey PRIMARY KEY (id);


--
-- Name: user user_second_username_uindex; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_second_username_uindex UNIQUE (second_username);


--
-- Name: user_skin user_skin_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_skin
    ADD CONSTRAINT user_skin_pkey PRIMARY KEY (id);


--
-- Name: user_sol_transactions user_sol_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_sol_transactions
    ADD CONSTRAINT user_sol_transactions_pkey PRIMARY KEY (id);


--
-- Name: user_subscription user_subscription_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_subscription
    ADD CONSTRAINT user_subscription_pkey PRIMARY KEY (uid);


--
-- Name: user_ton_completed_tasks user_ton_completed_tasks_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_ton_completed_tasks
    ADD CONSTRAINT user_ton_completed_tasks_pk PRIMARY KEY (uid, task_id);


--
-- Name: user_ton_transactions user_ton_transactions_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_ton_transactions
    ADD CONSTRAINT user_ton_transactions_pk PRIMARY KEY (id);


--
-- Name: user_total_bcoin_deposited user_total_bcoin_deposited_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_total_bcoin_deposited
    ADD CONSTRAINT user_total_bcoin_deposited_pk PRIMARY KEY (uid, type);


--
-- Name: user user_user_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_user_name_key UNIQUE (user_name);


--
-- Name: user_vic_transactions user_vic_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_vic_transactions
    ADD CONSTRAINT user_vic_transactions_pkey PRIMARY KEY (id);


--
-- Name: whitelist whitelist_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.whitelist
    ADD CONSTRAINT whitelist_pkey PRIMARY KEY (user_name);


--
-- Name: logs_th_mode_v2_races_table_name_index; Type: INDEX; Schema: logs; Owner: -
--

CREATE INDEX logs_th_mode_v2_races_table_name_index ON logs.logs_th_mode_v2_races USING btree (table_name);


--
-- Name: logs_th_mode_v2_summary_date_index; Type: INDEX; Schema: logs; Owner: -
--

CREATE INDEX logs_th_mode_v2_summary_date_index ON logs.logs_th_mode_v2_summary USING btree (date);


--
-- Name: logs_th_mode_v2_summary_hero_id_index; Type: INDEX; Schema: logs; Owner: -
--

CREATE INDEX logs_th_mode_v2_summary_hero_id_index ON logs.logs_th_mode_v2_summary USING btree (hero_id);


--
-- Name: logs_th_mode_v2_summary_uid_index; Type: INDEX; Schema: logs; Owner: -
--

CREATE INDEX logs_th_mode_v2_summary_uid_index ON logs.logs_th_mode_v2_summary USING btree (uid);


--
-- Name: logs_th_mode_v2_template_hero_id_index; Type: INDEX; Schema: logs; Owner: -
--

CREATE INDEX logs_th_mode_v2_template_hero_id_index ON logs.logs_th_mode_v2_template USING btree (hero_id);


--
-- Name: logs_th_mode_v2_template_reward_bcoin_index; Type: INDEX; Schema: logs; Owner: -
--

CREATE INDEX logs_th_mode_v2_template_reward_bcoin_index ON logs.logs_th_mode_v2_template USING btree (reward_bcoin);


--
-- Name: logs_th_mode_v2_template_reward_sen_index; Type: INDEX; Schema: logs; Owner: -
--

CREATE INDEX logs_th_mode_v2_template_reward_sen_index ON logs.logs_th_mode_v2_template USING btree (reward_sen);


--
-- Name: logs_th_mode_v2_template_uid_index; Type: INDEX; Schema: logs; Owner: -
--

CREATE INDEX logs_th_mode_v2_template_uid_index ON logs.logs_th_mode_v2_template USING btree (uid);


--
-- Name: log_block_recieve_reward_template_user_name_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX log_block_recieve_reward_template_user_name_idx ON public.log_block_recieve_reward_template USING btree (user_name, type, mode);


--
-- Name: user_bomber_bomber_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_bomber_bomber_id_idx ON public.user_bomber USING btree (bomber_id);


--
-- Name: user_bomber_data_type_uid_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_bomber_data_type_uid_index ON public.user_bomber USING btree (data_type, uid);


--
-- Name: user_booster_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_booster_idx ON public.user_booster USING btree (uid);


--
-- Name: user_email_uindex; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX user_email_uindex ON public."user" USING btree (email);


--
-- Name: user_gacha_chest_deleted_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_gacha_chest_deleted_index ON public.user_gacha_chest USING btree (deleted);


--
-- Name: user_item_status_status_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_item_status_status_index ON public.user_item_status USING btree (status);


--
-- Name: user_marketplace_unit_price_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_marketplace_unit_price_index ON public.user_marketplace USING btree (unit_price);


--
-- Name: user_ranking_coin_season_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_ranking_coin_season_index ON public.user_ranking_coin USING btree (season);


--
-- Name: user_skin_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_skin_idx ON public.user_skin USING btree (uid, status);


--
-- Name: user_swap_gem_time_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_swap_gem_time_index ON public.user_swap_gem USING btree ("time" DESC);


--
-- Name: user_user_name_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX user_user_name_idx ON public."user" USING btree (user_name);


--
-- Name: logs_th_mode_v2_template logs.insert_logs_th_mode_v2_trigger; Type: TRIGGER; Schema: logs; Owner: -
--

CREATE TRIGGER "logs.insert_logs_th_mode_v2_trigger" BEFORE INSERT ON logs.logs_th_mode_v2_template FOR EACH ROW EXECUTE FUNCTION logs.logs_th_mode_v2_insert_trigger();


--
-- Name: logs_user_block_reward_template logs.insert_logs_user_block_reward_trigger; Type: TRIGGER; Schema: logs; Owner: -
--

CREATE TRIGGER "logs.insert_logs_user_block_reward_trigger" BEFORE INSERT ON logs.logs_user_block_reward_template FOR EACH ROW EXECUTE FUNCTION logs.logs_user_block_reward_insert_trigger();


--
-- Name: user_block_reward user_block_reward_check_values_negative; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER user_block_reward_check_values_negative BEFORE INSERT OR UPDATE OF "values" ON public.user_block_reward FOR EACH ROW WHEN ((new."values" < (0)::double precision)) EXECUTE FUNCTION public.fn_tr_check_user_block_reward_values();


--
-- PostgreSQL database dump complete
--

\unrestrict SrJR32Ye34b8ONLQfcfd5zOvoS8QoXHQikYMmdW6PJGFFahxk0lmYTHfJ8o4IjF

