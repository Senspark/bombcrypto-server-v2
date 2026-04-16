-- ============================================================================
-- Migration: Expand Rarities 6-9 (Mega, Super Mega, Mystic, Super Mystic)
-- Target: BSC and Polygon Networks
-- Date: 2026-04-03
-- Author: Senspark Team
-- Description: Adds configuration data for rarities 6 (Mega), 7 (Super Mega),
--              8 (Mystic), and 9 (Super Mystic) to all relevant config tables.
-- ============================================================================

BEGIN;

-- ============================================================================
-- 1. config_min_stake_hero
-- Pattern: Geometric progression (each ~2x of previous)
-- Existing: 0→60, 1→486, 2→971, 3→1942, 4→4854, 5→9709
-- ============================================================================
INSERT INTO public.config_min_stake_hero (rarity, min_stake_amount)
VALUES
    (6, 12622),   -- 9709 * 1.30
    (7, 16409),   -- 12622 * 1.30
    (8, 21331),   -- 16409 * 1.30
    (9, 27730)    -- 21331 * 1.30
ON CONFLICT (rarity) DO NOTHING;


-- ============================================================================
-- 2. config_hero_upgrade_shield
-- Pattern: Linear progression of shield data and price
-- Existing: 0→[1000,2000,3000,4000]@[0,1,1,1] ... 5→[2000,4000,6000,8000]@[0,10,10,10]
-- ============================================================================
INSERT INTO public.config_hero_upgrade_shield (rarity, data, price)
VALUES
    (6, '[2250, 4500, 6750, 9000]', '[0,12,12,12]'),
    (7, '[2500, 5000, 7500, 10000]', '[0,14,14,14]'),
    (8, '[2750, 5500, 8250, 11000]', '[0,16,16,16]'),
    (9, '[3000, 6000, 9000, 12000]', '[0,18,18,18]')
ON CONFLICT (rarity) DO NOTHING;


-- ============================================================================
-- 3. config_hero_repair_shield
-- Pattern: 4 shield_levels (0-3) per rarity
-- Price follows linear progression, price_rock follows multiplier pattern
-- Existing max: rarity 5 → price 50, base rock 10 (level 0)
-- ============================================================================
INSERT INTO public.config_hero_repair_shield (id, rarity, shield_level, price, price_rock)
VALUES
    -- Rarity 6 (Mega): price=60, rock base=12
    (25, 6, 0, 60, 12),
    (26, 6, 1, 60, 24),
    (27, 6, 2, 60, 36),
    (28, 6, 3, 60, 48),
    -- Rarity 7 (Super Mega): price=70, rock base=14
    (29, 7, 0, 70, 14),
    (30, 7, 1, 70, 28),
    (31, 7, 2, 70, 42),
    (32, 7, 3, 70, 56),
    -- Rarity 8 (Mystic): price=80, rock base=16
    (33, 8, 0, 80, 16),
    (34, 8, 1, 80, 32),
    (35, 8, 2, 80, 48),
    (36, 8, 3, 80, 64),
    -- Rarity 9 (Super Mystic): price=90, rock base=18
    (37, 9, 0, 90, 18),
    (38, 9, 1, 90, 36),
    (39, 9, 2, 90, 54),
    (40, 9, 3, 90, 72)
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 4. config_burn_hero
-- Pattern: Increasing progression for rock yields
-- Existing: 0→5/1, 1→10/2, 2→20/4, 3→35/7, 4→55/11, 5→80/16
-- ============================================================================
INSERT INTO public.config_burn_hero (rarity, hero_s_rock, hero_l_rock)
VALUES
    (6, 110, 22),
    (7, 150, 30),
    (8, 200, 40),
    (9, 260, 52)
ON CONFLICT (rarity) DO NOTHING;


-- ============================================================================
-- 5. config_hero_upgrade_power
-- Pattern: All rarities use same power array [0,1,2,3,5]
-- ============================================================================
INSERT INTO public.config_hero_upgrade_power (rare, datas)
VALUES
    (6, '[0,1,2,3,5]'),
    (7, '[0,1,2,3,5]'),
    (8, '[0,1,2,3,5]'),
    (9, '[0,1,2,3,5]')
ON CONFLICT (rare) DO NOTHING;


-- ============================================================================
-- 6. config_reset_shield_balancer
-- Pattern: Linear progression +40000 per rarity
-- Existing: 0→160000, 1→180000, 2→200000, 3→240000, 4→280000, 5→320000
-- ============================================================================
INSERT INTO public.config_reset_shield_balancer (rare, final_damage)
VALUES
    (6, 360000),
    (7, 400000),
    (8, 440000),
    (9, 480000)
ON CONFLICT (rare) DO NOTHING;


-- ============================================================================
-- 7. config_reward_pool_th_v2
-- Game Design Option B: Soft-Cut with Expanded Supply
-- Update existing pools (3, 4, 5) and add new pools (6, 7, 8, 9)
-- ============================================================================
-- Update existing BCOIN pools (Soft-cut)
UPDATE public.config_reward_pool_th_v2 SET max_reward = 3000, remaining_reward = LEAST(remaining_reward, 3000) WHERE pool_id = 3 AND type = 'BCOIN';
UPDATE public.config_reward_pool_th_v2 SET max_reward = 5000, remaining_reward = LEAST(remaining_reward, 5000) WHERE pool_id = 4 AND type = 'BCOIN';
UPDATE public.config_reward_pool_th_v2 SET max_reward = 7000, remaining_reward = LEAST(remaining_reward, 7000) WHERE pool_id = 5 AND type = 'BCOIN';

-- Insert new BCOIN pools for 6-9
INSERT INTO public.config_reward_pool_th_v2 (pool_id, remaining_reward, type, max_reward)
VALUES
    (6, 1300, 'BCOIN', 1300),   -- Funded by deducing from 3, 4, 5
    (7, 1000, 'BCOIN', 1000),
    (8, 800, 'BCOIN', 800),
    (9, 650, 'BCOIN', 650)
ON CONFLICT (pool_id, type) DO UPDATE SET max_reward = EXCLUDED.max_reward, remaining_reward = EXCLUDED.remaining_reward;

-- Initialize SENSPARK and COIN pools for 6-9
INSERT INTO public.config_reward_pool_th_v2 (pool_id, remaining_reward, type, max_reward)
VALUES
    (6, 8333, 'SENSPARK', 8333),
    (7, 8333, 'SENSPARK', 8333),
    (8, 8333, 'SENSPARK', 8333),
    (9, 8333, 'SENSPARK', 8333),
    (6, 500000, 'COIN', 500000),
    (7, 500000, 'COIN', 500000),
    (8, 500000, 'COIN', 500000),
    (9, 500000, 'COIN', 500000)
ON CONFLICT (pool_id, type) DO NOTHING;


-- ============================================================================
-- 8. config_th_mode_v2 (Expansion to 10 tiers)
-- reward_pool: JSONB map for rarity keys
-- min_stake: JSONB array of size 10 (+30% rule for 6-9)
-- ============================================================================
UPDATE public.config_th_mode_v2 
SET value = '{"0":1250,"1":2000,"2":3000,"3":3000,"4":5000,"5":7000,"6":1300,"7":1000,"8":800,"9":650}'
WHERE key = 'reward_pool' AND type = 'BCOIN';

UPDATE public.config_th_mode_v2 
SET value = '[0,0,0,0,0,0,1,1.3,1.7,2.2]'
WHERE key = 'min_stake';


-- ============================================================================
-- 8. config_th_mode → hero_upgrade_cost (expand from 6 to 10 arrays)
-- Pattern: Continue increasing cost progression for BSC and POLYGON
-- Existing BSC:  [[1,2,4,7],[2,4,5,9],[2,4,5,10],[3,7,11,22],[7,18,40,146],[9,25,56,199]]
-- Existing POL:  (same as BSC)
-- ============================================================================
UPDATE public.config_th_mode
SET value = '[[1,2,4,7],[2,4,5,9],[2,4,5,10],[3,7,11,22],[7,18,40,146],[9,25,56,199],[12,33,75,265],[16,44,100,353],[21,59,133,470],[28,79,177,626]]'
WHERE key = 'hero_upgrade_cost' AND network = 'BSC';

UPDATE public.config_th_mode
SET value = '[[1,2,4,7],[2,4,5,9],[2,4,5,10],[3,7,11,22],[7,18,40,146],[9,25,56,199],[12,33,75,265],[16,44,100,353],[21,59,133,470],[28,79,177,626]]'
WHERE key = 'hero_upgrade_cost' AND network = 'POLYGON';


-- ============================================================================
-- 9. config_th_mode → hero_ability_designs (expand from 6 to 10)
-- Pattern: Continue min_cost/max_cost/incremental_cost growth
-- ============================================================================
UPDATE public.config_th_mode
SET value = '[{"min_cost":2,"max_cost":2,"incremental_cost":0},{"min_cost":5,"max_cost":10,"incremental_cost":1},{"min_cost":10,"max_cost":20,"incremental_cost":2},{"min_cost":20,"max_cost":40,"incremental_cost":4},{"min_cost":35,"max_cost":60,"incremental_cost":5},{"min_cost":50,"max_cost":80,"incremental_cost":6},{"min_cost":65,"max_cost":100,"incremental_cost":7},{"min_cost":80,"max_cost":120,"incremental_cost":8},{"min_cost":100,"max_cost":150,"incremental_cost":9},{"min_cost":120,"max_cost":180,"incremental_cost":10}]'
WHERE key = 'hero_ability_designs' AND network = 'BSC';

UPDATE public.config_th_mode
SET value = '[{"min_cost":2,"max_cost":2,"incremental_cost":0},{"min_cost":5,"max_cost":10,"incremental_cost":1},{"min_cost":10,"max_cost":20,"incremental_cost":2},{"min_cost":20,"max_cost":40,"incremental_cost":4},{"min_cost":35,"max_cost":60,"incremental_cost":5},{"min_cost":50,"max_cost":80,"incremental_cost":6},{"min_cost":65,"max_cost":100,"incremental_cost":7},{"min_cost":80,"max_cost":120,"incremental_cost":8},{"min_cost":100,"max_cost":150,"incremental_cost":9},{"min_cost":120,"max_cost":180,"incremental_cost":10}]'
WHERE key = 'hero_ability_designs' AND network = 'POLYGON';


-- ============================================================================
-- 10. config_th_mode → house_mint_limits (expand from 6 to 10)
-- Pattern: Ultra-rare houses for higher rarities
-- ============================================================================
UPDATE public.config_th_mode
SET value = '[2500,1250,750,250,200,50,25,10,5,2]'
WHERE key = 'house_mint_limits' AND network = 'BSC';

UPDATE public.config_th_mode
SET value = '[2500,1250,750,250,200,50,25,10,5,2]'
WHERE key = 'house_mint_limits' AND network = 'POLYGON';


-- ============================================================================
-- 11. config_th_mode → house_stats (expand from 6 to 10)
-- Pattern: Continue recovery/capacity progression
-- ============================================================================
UPDATE public.config_th_mode
SET value = '[{"recovery":120,"capacity":4},{"recovery":300,"capacity":6},{"recovery":480,"capacity":8},{"recovery":660,"capacity":10},{"recovery":840,"capacity":12},{"recovery":1020,"capacity":14},{"recovery":1200,"capacity":16},{"recovery":1380,"capacity":18},{"recovery":1560,"capacity":20},{"recovery":1740,"capacity":22}]'
WHERE key = 'house_stats' AND network = 'BSC';

UPDATE public.config_th_mode
SET value = '[{"recovery":120,"capacity":4},{"recovery":300,"capacity":6},{"recovery":480,"capacity":8},{"recovery":660,"capacity":10},{"recovery":840,"capacity":12},{"recovery":1020,"capacity":14},{"recovery":1200,"capacity":16},{"recovery":1380,"capacity":18},{"recovery":1560,"capacity":20},{"recovery":1740,"capacity":22}]'
WHERE key = 'house_stats' AND network = 'POLYGON';


-- ============================================================================
-- 12. config_th_mode → house_prices (expand from 6 to 10)
-- Pattern: Continue price progression for BSC/POLYGON
-- ============================================================================
UPDATE public.config_th_mode
SET value = '[720,2400,5400,9600,15000,21600,29400,38400,48600,60000]'
WHERE key = 'house_prices' AND network = 'BSC';

UPDATE public.config_th_mode
SET value = '[720,2400,5400,9600,15000,21600,29400,38400,48600,60000]'
WHERE key = 'house_prices' AND network = 'POLYGON';


-- ============================================================================
-- 13. config_th_mode → fusion_fee for BSC and POLYGON (NEW - does not exist yet)
-- Pattern: Based on TON pattern [0,8,11,14,23,38,56,74,95,116]
-- BSC/POL use Coin (BCOIN) based pricing
-- ============================================================================
INSERT INTO public.config_th_mode (key, value, network)
VALUES
    ('fusion_fee', '[0,8,11,14,23,38,56,74,95,116]', 'BSC'),
    ('fusion_fee', '[0,8,11,14,23,38,56,74,95,116]', 'POLYGON')
ON CONFLICT DO NOTHING;


-- ============================================================================
-- Validation Queries (run after migration to verify)
-- ============================================================================
-- SELECT * FROM config_min_stake_hero ORDER BY rarity;
-- SELECT * FROM config_hero_upgrade_shield ORDER BY rarity;
-- SELECT * FROM config_hero_repair_shield WHERE rarity >= 6 ORDER BY rarity, shield_level;
-- SELECT * FROM config_burn_hero ORDER BY rarity;
-- SELECT * FROM config_hero_upgrade_power ORDER BY rare;
-- SELECT * FROM config_reset_shield_balancer ORDER BY rare;
-- SELECT * FROM config_reward_pool_th_v2 WHERE pool_id >= 6 ORDER BY pool_id, type;
-- SELECT * FROM config_th_mode WHERE key IN ('hero_upgrade_cost', 'hero_ability_designs', 'fusion_fee') AND network IN ('BSC', 'POLYGON');

COMMIT;
