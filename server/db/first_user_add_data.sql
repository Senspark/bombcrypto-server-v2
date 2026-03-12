-- 1. Add 3 BHero to user 1
INSERT INTO public.user_bomber (uid, gen_id, bomber_id, name, level, power, bomb_range, stamina, speed, bomb, ability,
                                charactor, color, rare, bomb_skin, energy, stage, time_rest, active, "hasDelete",
                                random, shield, ability_shield, is_reset, shield_level, type, data_type, hero_tr_type,
                                stake_amount, stake_sen, create_at)
VALUES (1, '50764782002357373746285898066351220654384171305110863873', 1, NULL, 5, 18, 6, 17, 17,
        6, '[7,2,1,6,4,3]', 8, 1, 3, 17, 283, 0, '2026-01-01 00:00:00', 1, 0, 0,
        '[{"ability":1,"finalDamage":1244745}]', '[1]', 0, 3, 0, 'BSC', 'HERO', 300, 0, NOW() AT TIME ZONE 'utc'),
       (1, '50764782002357373746285898066351220654420201201641455618', 2, NULL, 5, 18, 6, 17, 17,
        6, '[7,2,1,6,4,3]', 9, 1, 4, 17, 283, 0, '2026-01-01 00:00:00', 1, 0, 0,
        '[{"ability":1,"finalDamage":1244745}]', '[1]', 0, 3, 0, 'BSC', 'HERO', 300, 0, NOW() AT TIME ZONE 'utc'),
       (1, '50764782002357373746285898066351220654456231098172047363', 3, NULL, 5, 18, 6, 17, 17,
        6, '[7,2,1,6,4,3]', 10, 1, 5, 17, 283, 0, '2026-01-01 00:00:00', 1, 0, 0,
        '[{"ability":1,"finalDamage":1244745}]', '[1]', 0, 3, 0, 'BSC', 'HERO', 300, 0, NOW() AT TIME ZONE 'utc');

-- 2. Add 1 BHouse to user 1
INSERT INTO public.user_house (uid, gen_house_id, house_id, rarity, recovery, max_bomber, active, sync_date, type,
                               create_at, end_time_rent)
VALUES (1, '13864617326957035521', 1, 4, 14, 12, 1, NOW() AT TIME ZONE 'utc', 'BSC',
        NOW() AT TIME ZONE 'utc', NULL);
        
-- 3. Add Currencies to user 1
INSERT INTO public.user_block_reward (uid, reward_type, values, total_values, type)
VALUES (1, 'GOLD', 500000, 500000, 'TR'),
       (1, 'SENSPARK', 500000, 500000, 'BSC'),
       (1, 'BCOIN', 500000, 500000, 'BSC'),
       (1, 'COIN', 500000, 500000, 'TR'),
       (1, 'ROCK', 500000, 500000, 'TR'),
       (1, 'GEM_LOCKED', 500000, 500000, 'TR'),
       (1, 'GEM', 5000000, 500000, 'TR');