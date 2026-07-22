-- Migration: Fix hero_tr_type assignment when inserting multiple heroes at once
-- Date: 2026-06-29
-- Applies to: bombcrypto database
--
-- Bug: fn_insert_new_hero_tr computes the has_hero count ONCE, then uses it in a
-- single CASE across every row produced by GENERATE_SERIES(1, _quantity). When a
-- user owns no HERO of a (charactor, color) yet and acquires _quantity >= 2 copies
-- in one insert (e.g. opening multiple gacha chests that aggregate into one
-- AddUserItemWrapper with quantity > 1), ALL inserted rows evaluate the same
-- snapshot (count = 0) and are written as 'HERO'. This violates the rule that only
-- ONE hero per (charactor, color) may be 'HERO'; every later duplicate must be 'SOUL'.
--
-- Fix: thread the per-row index from GENERATE_SERIES into the CASE so that, when the
-- user has no existing HERO (count = 0), only the first generated row (idx = 1) is
-- 'HERO' and the rest are 'SOUL'. If a HERO already exists (count >= 1) every new row
-- is 'SOUL'. This mirrors the correct logic already used in sp_buy_item_marketplace
-- (_item_type = 4), keyed on (charactor, color).

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
    AS $$
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
                           CASE
                               WHEN (SELECT count FROM has_hero) = 0 AND gs.idx = 1
                                   THEN 'HERO'
                               ELSE 'SOUL' END
                    FROM GENERATE_SERIES(1, _quantity) AS gs(idx)
                    ON CONFLICT ("bomber_id", "type", "data_type")
                        DO UPDATE SET "hasDelete" = 0,
                            active = excluded.active,
                            uid = excluded.uid,
                            time_rest = excluded.time_rest,
                            stage = _stage,
                            hero_tr_type = excluded.hero_tr_type
                    RETURNING ub.uid, ub.bomber_id::INT AS bid, ub.hero_tr_type;

END;
$$;
