-- Migration: Sync functions/procedures to match production database
-- Date: 2026-04-01
-- Applies to: bombcrypto database
--
-- schema.sql was found to be behind production in 17 functions and missing 3.
-- This migration brings a fresh database in line with the actual production state.

--
-- fn_pvp_save_fixture_match_to_log
--

CREATE OR REPLACE FUNCTION public.fn_pvp_save_fixture_match_to_log(userid1 integer, userid2 integer, game_mode integer,
                                                                   from_time timestamp WITH TIME ZONE,
                                                                   to_time timestamp WITH TIME ZONE)
    RETURNS boolean
    LANGUAGE plpgsql
AS
$function$
DECLARE
    user1 varchar;
    user2 varchar;
BEGIN
    SELECT user_name INTO user1 FROM public.user WHERE id_user = userId1;
    SELECT user_name INTO user2 FROM public.user WHERE id_user = userId2;

    IF EXISTS(SELECT *
              FROM pvp_tournament
              WHERE mode = game_mode
                AND status = 'PENDING'
                AND ((participant_1 = user1::varchar AND participant_2 = user2::varchar)
                  OR (participant_2 = user1::varchar AND participant_1 = user2::varchar))) THEN
        RETURN FALSE;
    ELSE
        INSERT INTO public.pvp_tournament (participant_1, participant_2, mode, status, find_begin_time, find_end_time)
        VALUES (user1, user2, game_mode, 'PENDING', from_time, to_time);
        RETURN TRUE;
    END IF;

END;
$function$


--
-- sp_modify_rock_from_user_wallet
--

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

        INSERT INTO public.user_create_rock(uid, tx, heroes, rock_amount, network, status)
        VALUES (_uid, _tx, _heroes_ids, _amount, _network, 'DONE');
    END IF;
END;
$procedure$


--
-- sp_user_buy_auto_mine
--

CREATE OR REPLACE PROCEDURE public.sp_user_buy_auto_mine(IN _uid integer, IN _reward_type character varying,
                                                         IN _data_type character varying, IN _package_json json)
    LANGUAGE plpgsql
AS
$procedure$
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
$procedure$


--
-- fn_delete_tournament_match_v2
--

CREATE OR REPLACE FUNCTION public.fn_delete_tournament_match_v2(p_match_id integer)
    RETURNS TABLE
            (
                success boolean,
                message text
            )
    LANGUAGE plpgsql
AS
$function$
DECLARE
    v_exists BOOLEAN;
    v_status VARCHAR;
BEGIN
    -- Check if the match exists in pvp_tournament table
    SELECT EXISTS(SELECT 1 FROM pvp_tournament WHERE id = p_match_id) INTO v_exists;

    IF NOT v_exists THEN
        RETURN QUERY SELECT FALSE, 'Match not found in tournament table';
        RETURN;
    END IF;


    -- Hiện cho marketing xoá bất kỳ trận nào
    -- Check tournament status
--     SELECT status INTO v_status
--     FROM pvp_tournament
--     WHERE id = p_match_id;
--
--     -- Ko cho phép delete match đã completed
--     IF v_status = 'COMPLETED' THEN
--         RETURN QUERY SELECT FALSE, 'Cannot delete match from a completed tournament';
--         RETURN;
--     END IF;

    -- Begin transaction
    BEGIN
        -- Backup the tournament data to pvp_tournament_backup
        INSERT INTO pvp_tournament_backup
        SELECT *
        FROM pvp_tournament
        WHERE id = p_match_id;

        -- Delete from pvp_fixture_matches
        DELETE FROM pvp_fixture_matches WHERE id = p_match_id;

        -- Delete from pvp_tournament
        DELETE FROM pvp_tournament WHERE id = p_match_id;

        -- Return success
        RETURN QUERY SELECT TRUE, 'Tournament match deleted successfully';
    EXCEPTION
        WHEN OTHERS THEN
            -- Return error on exception
            RETURN QUERY SELECT FALSE, 'Error deleting tournament match: ' || SQLERRM;
    END;
END;
$function$


--
-- fn_get_coin_ranking_5
--

CREATE OR REPLACE FUNCTION public.fn_get_coin_ranking_5(_season integer, _network character varying)
    RETURNS TABLE
            (
                uid                 integer,
                coin_total          double precision,
                coin_current_season double precision,
                network             character varying,
                name                character varying
            )
    LANGUAGE plpgsql
AS
$function$
BEGIN
    RETURN QUERY
        WITH result AS (SELECT urc.uid,
                               SUM(urc.coin)                                                AS coin_total,
                               SUM(CASE WHEN urc.season = _season THEN urc.coin ELSE 0 END) AS coin_current_season,
                               urc.network
                        FROM user_ranking_coin urc
                        WHERE urc.network = _network
                        GROUP BY urc.uid, urc.network)
        SELECT r.*,
               CASE
                   WHEN u.user_name IS NOT NULL THEN u.user_name
                   WHEN u.name IS NOT NULL THEN u.name
                   ELSE u.second_username
                   END AS name
        FROM result AS r
                 INNER JOIN "user" AS u ON r.uid = u.id_user;
END;
$function$


--
-- fn_insert_new_hero_tr
--

CREATE OR REPLACE FUNCTION public.fn_insert_new_hero_tr(_userid integer, _datatype character varying,
                                                        _details character varying, _herotype integer, _level integer,
                                                        _bombpower integer, _bombrange integer, _stamina integer,
                                                        _speed integer, _bombcount integer, _ability character varying,
                                                        _skin integer, _color integer, _rarity integer,
                                                        _bombskin integer, _energy integer, _stage integer,
                                                        _timerest timestamp WITH TIME ZONE, _isactive boolean,
                                                        _shield character varying, _abilitys character varying,
                                                        _shieldlevel integer, _quantity integer)
    RETURNS TABLE
            (
                uid          integer,
                bid          integer,
                hero_tr_type character varying
            )
    LANGUAGE plpgsql
AS
$function$
DECLARE
BEGIN
    -- has_hero đếm số lượng hero TR cùng loại
    RETURN QUERY
        WITH has_hero AS (SELECT COUNT(*) AS count
                          FROM user_bomber ub
                          WHERE ub.uid = _userId
                            AND ub.charactor = _skin
                            AND ub.color = _color
                            AND ub.type = 2
                            AND ub.hero_tr_type = 'HERO')
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
                           CASE WHEN (SELECT count FROM has_hero) >= 1 THEN 'SOUL' ELSE 'HERO' END
                    FROM GENERATE_SERIES(1, _quantity)
                    ON CONFLICT ("bomber_id", "type", "data_type")
                        DO UPDATE SET "hasDelete" = 0,
                            active = excluded.active,
                            uid = excluded.uid,
                            time_rest = excluded.time_rest,
                            stage = _stage,
                            hero_tr_type = excluded.hero_tr_type
                    RETURNING ub.uid, ub.bomber_id::INT AS bid, ub.hero_tr_type;

END;
$function$


--
-- fn_sub_user_gem
--

CREATE OR REPLACE FUNCTION public.fn_sub_user_gem(_uid integer, _datatype character varying, _amount double precision,
                                                  _reason character varying)
    RETURNS text
    LANGUAGE plpgsql
AS
$function$
DECLARE
    _gem_locked_amount     DOUBLE PRECISION;
    _gem_amount            DOUBLE PRECISION;
    _sub_gem_locked_amount DOUBLE PRECISION;
    _sub_amount            DOUBLE PRECISION;
    _message               text;
    _new_gem_locked_amount DOUBLE PRECISION;
    _new_gem_amount        DOUBLE PRECISION;
BEGIN

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

        INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new, reason)
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

        INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new, reason)
        VALUES (_uid, 'GEM', _dataType, _gem_amount, -_sub_amount, _new_gem_amount, _reason);
    END IF;

    RETURN JSON_BUILD_OBJECT('GEM_LOCKED', _sub_gem_locked_amount, 'GEM', _sub_amount)::text;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%',SQLERRM;
END;
$function$


--
-- fn_update_user_ton_transaction
--

CREATE OR REPLACE FUNCTION public.fn_update_user_ton_transaction(_id integer, _amount double precision,
                                                                 _tx_hash character varying, _token character varying)
    RETURNS character varying
    LANGUAGE plpgsql
AS
$function$
DECLARE
    v_tx_hash   VARCHAR;
    v_user_name VARCHAR;
    _uid        integer;
BEGIN
    -- Check if the transaction exists
    SELECT tx_hash
    INTO v_tx_hash
    FROM public.user_ton_transactions
    WHERE id = _id;

    -- If no roq is found, throw an exception
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Transaction not found for user id: %', _id;
    END IF;

    -- If tx_hash is NULL, update row
    IF v_tx_hash IS NULL THEN
        UPDATE public.user_ton_transactions
        SET amount           = _amount,
            tx_hash          = _tx_hash,
            token_name       = _token,
            transaction_type = 'Deposit'
        WHERE id = _id;

        SELECT uid
        INTO _uid
        FROM public.user_ton_transactions
        WHERE id = _id;

        PERFORM fn_add_user_reward(_uid, _token, _amount, 'TON_DEPOSITED', 'Deposit Ton');

        -- Select user_name from public.user
        SELECT user_name
        INTO v_user_name
        FROM public.user
        WHERE id_user = _uid;

        -- Return the user_name
        RETURN v_user_name;
    ELSE
        -- If tx_hash is not NULL, throw an exception
        RAISE EXCEPTION 'Transaction hash already exists for user id: %', _id;
    END IF;
END;
$function$


--
-- sp_buy_item_marketplace
--

CREATE OR REPLACE PROCEDURE public.sp_buy_item_marketplace(IN _item_id integer, IN _item_type integer,
                                                           IN _quantity integer, IN _unit_price double precision,
                                                           IN _buyer_id integer, IN _reward_type integer,
                                                           IN _name character varying, IN _expiration_after integer)
    LANGUAGE plpgsql
AS
$procedure$
DECLARE
    feeSell    FLOAT DEFAULT 0.2;
    typeReward VARCHAR(20) DEFAULT 'TR';
    _price     double precision DEFAULT _quantity * _unit_price;
    param      RECORD;
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
        SET uid    = _buyer_id,
            status = 0
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
$procedure$


--
-- sp_edit_item_marketplace
--

CREATE OR REPLACE PROCEDURE public.sp_edit_item_marketplace(IN _uid integer, IN _item_id integer,
                                                            IN _old_quantity integer,
                                                            IN _old_unit_price double precision,
                                                            IN _new_quantity integer,
                                                            IN _new_unit_price double precision,
                                                            IN _expiration_after integer)
    LANGUAGE plpgsql
AS
$procedure$
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
    SET list_id          = (SELECT JSON_AGG(instance_id) FROM _tbl_items_new),
        price            = _new_unit_price * (SELECT COUNT(*) FROM _tbl_items_new),
        unit_price       = _new_unit_price,
        quantity         = (SELECT COUNT(*) FROM _tbl_items_new),
        status           = 0,
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
$procedure$


--
-- sp_repair_hero_shield_with_rock
--

CREATE OR REPLACE PROCEDURE public.sp_repair_hero_shield_with_rock(IN _uid integer, IN _network character varying,
                                                                   IN _hero_id integer, IN _price double precision,
                                                                   IN _reward_type character varying,
                                                                   IN _remain_shield integer, IN _new_shield text)
    LANGUAGE plpgsql
AS
$procedure$
BEGIN
    PERFORM fn_sub_user_reward(_uid, 'TR', _price, _reward_type, 'Repair Shield Hero ' || _hero_id);

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
            'TR',
            _uid,
            _reward_type);

    UPDATE user_bomber
    SET shield = _new_shield
    WHERE bomber_id = _hero_id
      AND uid = _uid
      AND data_type = _network;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END;
$procedure$


--
-- sp_save_user_claim_reward_data
--

CREATE OR REPLACE PROCEDURE public.sp_save_user_claim_reward_data(IN _uid integer, IN _data_type character varying,
                                                                  IN _reward_type character varying,
                                                                  IN _min_claim double precision,
                                                                  IN _claim_fee_percent double precision,
                                                                  IN _api_synced_value double precision)
    LANGUAGE plpgsql
AS
$procedure$
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

--     trừ reward
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
$procedure$


--
-- sp_sell_item_marketplace
--

CREATE OR REPLACE PROCEDURE public.sp_sell_item_marketplace(IN _listid json, IN _type integer, IN _itemid integer,
                                                            IN _quantity integer, IN _price double precision,
                                                            IN _uid integer, IN _rewardtype integer,
                                                            IN _expiration_after integer)
    LANGUAGE plpgsql
AS
$procedure$
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
$procedure$


--
-- sp_setup_next_pvp_season
--

CREATE OR REPLACE PROCEDURE public.sp_setup_next_pvp_season()
    LANGUAGE plpgsql
AS
$procedure$
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
$procedure$


--
-- sp_sync_user_deposit
--

CREATE OR REPLACE PROCEDURE public.sp_sync_user_deposit(IN _uid integer, IN _type character varying,
                                                        IN _total_bcoin_deposited double precision,
                                                        IN _total_sen_deposited double precision)
    LANGUAGE plpgsql
AS
$procedure$
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
    WHERE uid = _uid
      AND type = _type;

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
        PERFORM fn_add_user_reward(_uid, _type, _addition_bcoin_deposited, 'BCOIN_DEPOSITED', 'Deposite Bcoin');
    END IF;

    IF _addition_sen_deposited > 0 THEN
        PERFORM fn_add_user_reward(_uid, _type, _addition_sen_deposited, 'SENSPARK_DEPOSITED', 'Deposite Sen');
    END IF;

END;
$procedure$


--
-- sp_user_buy_gacha_chest_slot
--

CREATE OR REPLACE PROCEDURE public.sp_user_buy_gacha_chest_slot(IN _uid integer, IN _data_type character varying,
                                                                IN _price integer, IN _slot_id integer,
                                                                IN _gacha_chest_slots character varying)
    LANGUAGE plpgsql
AS
$procedure$
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
$procedure$


--
-- sp_user_buy_rock_pack
--

CREATE OR REPLACE PROCEDURE public.sp_user_buy_rock_pack(IN _uid integer, IN _pack_name character varying,
                                                         IN _network character varying,
                                                         IN _reward_type character varying,
                                                         IN _second_reward_type character varying)
    LANGUAGE plpgsql
AS
$procedure$
DECLARE
    _price       NUMERIC;
    _rock_amount INT;
BEGIN
    -- Load giá
    IF _second_reward_type = 'SENSPARK' THEN
        SELECT sen_price, rock_amount INTO _price, _rock_amount FROM config_rock_pack WHERE pack_name = _pack_name;
    ELSE
        SELECT bcoin_price, rock_amount INTO _price, _rock_amount FROM config_rock_pack WHERE pack_name = _pack_name;
    END IF;

    -- Trừ tiền
    PERFORM fn_sub_user_reward(_uid, _network, _price, _reward_type, _second_reward_type, 'Buy rock pack');

    -- Cộng đá
    PERFORM fn_add_user_reward(_uid, 'TR', _rock_amount, 'ROCK', 'Buy rock pack');

    -- Lưu lại lịch sử
    INSERT INTO bombcrypto2.logs.logs_user_buy_rock_pack (uid, time_stamp, package_name, rock_amount, price, token_name,
                                                          network)
    VALUES (_uid, CURRENT_TIMESTAMP, _pack_name, _rock_amount, _price, _reward_type, _network);

END;
$procedure$


--
-- fn_get_coin_ranking
--

CREATE OR REPLACE FUNCTION public.fn_get_coin_ranking()
    RETURNS TABLE
            (
                uid     integer,
                coin    double precision,
                network character varying,
                name    character varying
            )
    LANGUAGE plpgsql
AS
$function$
BEGIN
    RETURN QUERY
        WITH result AS (SELECT * FROM user_ranking_coin)
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
$function$


--
-- fn_get_coin_ranking_ton
--

CREATE OR REPLACE FUNCTION public.fn_get_coin_ranking_ton(_season integer)
    RETURNS TABLE
            (
                uid                 integer,
                coin_total          double precision,
                coin_current_season double precision,
                network             character varying,
                name                character varying
            )
    LANGUAGE plpgsql
AS
$function$
BEGIN
    RETURN QUERY
        WITH result AS (SELECT urc.uid,
                               SUM(urc.coin)                                                AS coin_total,
                               SUM(CASE WHEN urc.season = _season THEN urc.coin ELSE 0 END) AS coin_current_season,
                               urc.network
                        FROM user_ranking_coin urc
                        WHERE urc.network = 'TON'
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
$function$


--
-- fn_insert_new_ton_hero
--

CREATE OR REPLACE FUNCTION public.fn_insert_new_ton_hero(_uid integer, _level integer, _power integer,
                                                         _bomb_range integer, _stamina integer, _speed integer,
                                                         _bomb integer, _ability character varying, _charactor integer,
                                                         _color integer, _rare integer, _bomb_skin integer,
                                                         _shield character varying, _network character varying,
                                                         _ability_shield character varying, _active integer)
    RETURNS integer
    LANGUAGE plpgsql
AS
$function$
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

$function$
