-- Migration: add rarity 6-9 (Mega, Super Mega, Mystic, Super Mystic) to BHero config
-- Phase 2 of the rarity-expansion work
-- Source values: data.md (game design), CURRENT_CONTRACT_CONFIG_BSC_TESTNET.md (on-chain placeholders)
-- Placeholders are marked with `-- placeholder` comments where designer review is pending.

BEGIN;

-- ============================================================================
-- 1) config_burn_hero — Quartz gained per hero burned (Exchange Hero To Quartz)
-- ============================================================================
-- hero_l_rock = 0 because legacy BHero (BHero L) does not exist for rarity 6-9.
INSERT INTO config_burn_hero (rarity, hero_s_rock, hero_l_rock) VALUES
  (6, 110, 0),  -- Mega
  (7, 145, 0),  -- Super Mega
  (8, 185, 0),  -- Mystic
  (9, 230, 0);  -- Super Mystic

-- ============================================================================
-- 2) config_min_stake_hero — BHero L → BHero S conversion threshold (BCoin)
-- ============================================================================
-- Legacy BHero L stops at rarity 5; rarity 6-9 use 0 (placeholder, never reached
-- in practice). Required to satisfy startup validation that all rarities exist.
INSERT INTO config_min_stake_hero (rarity, min_stake_amount) VALUES
  (6, 0),  -- Mega          -- placeholder (legacy BHero unreachable at this rarity)
  (7, 0),  -- Super Mega    -- placeholder
  (8, 0),  -- Mystic        -- placeholder
  (9, 0);  -- Super Mystic  -- placeholder

-- ============================================================================
-- 3) config_hero_upgrade_shield — shield durability + Quartz upgrade cost
-- ============================================================================
-- data  = [Lv1, Lv2, Lv3, Lv4] durability points (Lv2=Lv1×2, Lv3=Lv1×3, Lv4=Lv1×4 per data.md)
-- price = [Lv1, Lv2, Lv3, Lv4] Quartz cost (Lv1=0, Lv2=Lv3=Lv4=base from data.md)
INSERT INTO config_hero_upgrade_shield (rarity, data, price) VALUES
  (6, '[2250, 4500, 6750, 9000]',   '[0,12,12,12]'),  -- Mega
  (7, '[2500, 5000, 7500, 10000]',  '[0,14,14,14]'),  -- Super Mega
  (8, '[2750, 5500, 8250, 11000]',  '[0,16,16,16]'),  -- Mystic
  (9, '[3000, 6000, 9000, 12000]',  '[0,18,18,18]');  -- Super Mystic

-- ============================================================================
-- 4) config_hero_repair_shield — BCoin price + Quartz price per rarity × shield_level
-- ============================================================================
-- price (BCoin flat per rarity) extrapolation: r6=60, r7=70, r8=80, r9=90 -- placeholder, designer to revise
-- price_rock = base × (shield_level + 1); base from data.md: r6=12, r7=14, r8=16, r9=18
INSERT INTO config_hero_repair_shield (id, rarity, shield_level, price, price_rock) VALUES
  -- Mega (r6)
  (25, 6, 0, 60, 12),
  (26, 6, 1, 60, 24),
  (27, 6, 2, 60, 36),
  (28, 6, 3, 60, 48),
  -- Super Mega (r7)
  (29, 7, 0, 70, 14),
  (30, 7, 1, 70, 28),
  (31, 7, 2, 70, 42),
  (32, 7, 3, 70, 56),
  -- Mystic (r8)
  (33, 8, 0, 80, 16),
  (34, 8, 1, 80, 32),
  (35, 8, 2, 80, 48),
  (36, 8, 3, 80, 64),
  -- Super Mystic (r9)
  (37, 9, 0, 90, 18),
  (38, 9, 1, 90, 36),
  (39, 9, 2, 90, 54),
  (40, 9, 3, 90, 72);

SELECT setval('config_hero_repair_shield_id_seq', 40, true);

-- ============================================================================
-- 5) game_config — TH Mode min stake JSON arrays (extend 6 → 10)
-- ============================================================================
UPDATE game_config
SET value = '[60,194,388,777,1942,3883,7766,19415,38830,77660]', date_updated = NOW()
WHERE key = 'min_stake_bcoin_th_v1';

UPDATE game_config
SET value = '[300,971,1942,3884,9709,19417,38834,77668,194170,388340]', date_updated = NOW()
WHERE key = 'min_stake_sen_th_v1';

-- ============================================================================
-- 6) config_th_mode_v2 — extend min_stake JSON (currently [0,0,0,0,0,0])
-- ============================================================================
-- NOTE: reward_pool key in this table is RUNTIME cache (auto-resets daily).
-- Do NOT update reward_pool here — config lives in config_reward_pool_th_v2.
UPDATE config_th_mode_v2
SET value = '[0,0,0,0,0,0,0,0,0,0]', date_updated = NOW()
WHERE key = 'min_stake' AND type IN ('BCOIN', 'SENSPARK');

-- ============================================================================
-- 7) config_th_mode — extend hero_upgrade_cost + hero_ability_designs (BSC + POLYGON only)
-- ============================================================================
-- The `value` column was varchar(512); the new hero_ability_designs JSON (10 entries)
-- is 514 chars. Widen to text — unbounded, same on-disk format as varchar in Postgres.
ALTER TABLE config_th_mode ALTER COLUMN value TYPE text;

-- Mirrors on-chain values; r6-9 are placeholders matching CURRENT_CONTRACT_CONFIG_BSC_TESTNET.md.
UPDATE config_th_mode
SET value = '[[1,2,4,7],[2,4,5,9],[2,4,5,10],[3,7,11,22],[7,18,40,146],[9,25,56,199],[12,34,76,269],[16,46,102,363],[22,62,138,490],[30,84,186,661]]'
WHERE key = 'hero_upgrade_cost' AND network IN ('BSC', 'POLYGON');

UPDATE config_th_mode
SET value = '[{"min_cost":2,"max_cost":2,"incremental_cost":0},{"min_cost":5,"max_cost":10,"incremental_cost":1},{"min_cost":10,"max_cost":20,"incremental_cost":2},{"min_cost":20,"max_cost":40,"incremental_cost":4},{"min_cost":35,"max_cost":60,"incremental_cost":5},{"min_cost":50,"max_cost":80,"incremental_cost":6},{"min_cost":65,"max_cost":100,"incremental_cost":7},{"min_cost":80,"max_cost":120,"incremental_cost":8},{"min_cost":95,"max_cost":140,"incremental_cost":9},{"min_cost":110,"max_cost":160,"incremental_cost":10}]'
WHERE key = 'hero_ability_designs' AND network IN ('BSC', 'POLYGON');

-- House-related keys in config_th_mode (house_mint_limits, house_stats, house_prices,
-- house_limit) are NOT touched — house has its own rarity system, out of scope.
-- fusion_fee for BSC/POLYGON: not present in current init snapshot; if a row exists in
-- prod, run a separate UPDATE matching the format used by other networks (length 10).

-- ============================================================================
-- 8) config_reward_pool_th_v2 — TH Mode max reward per rarity per type
-- ============================================================================
-- Sets BOTH max_reward and remaining_reward to the new max (remaining_reward will
-- decrement during runtime, resets daily).
-- BCOIN — UPDATE 0-5 to data.md, INSERT 6-9
UPDATE config_reward_pool_th_v2 SET remaining_reward = 250,  max_reward = 250  WHERE pool_id = 0 AND type = 'BCOIN';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 500,  max_reward = 500  WHERE pool_id = 1 AND type = 'BCOIN';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 1000, max_reward = 1000 WHERE pool_id = 2 AND type = 'BCOIN';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 1500, max_reward = 1500 WHERE pool_id = 3 AND type = 'BCOIN';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 2000, max_reward = 2000 WHERE pool_id = 4 AND type = 'BCOIN';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 2250, max_reward = 2250 WHERE pool_id = 5 AND type = 'BCOIN';
INSERT INTO config_reward_pool_th_v2 (pool_id, remaining_reward, type, max_reward) VALUES
  (6, 2500, 'BCOIN', 2500),  -- Mega
  (7, 3750, 'BCOIN', 3750),  -- Super Mega
  (8, 5000, 'BCOIN', 5000),  -- Mystic
  (9, 6250, 'BCOIN', 6250);  -- Super Mystic

-- SENSPARK — UPDATE 0-5 to data.md, INSERT 6-9
UPDATE config_reward_pool_th_v2 SET remaining_reward = 500,   max_reward = 500   WHERE pool_id = 0 AND type = 'SENSPARK';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 1000,  max_reward = 1000  WHERE pool_id = 1 AND type = 'SENSPARK';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 2000,  max_reward = 2000  WHERE pool_id = 2 AND type = 'SENSPARK';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 3000,  max_reward = 3000  WHERE pool_id = 3 AND type = 'SENSPARK';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 4000,  max_reward = 4000  WHERE pool_id = 4 AND type = 'SENSPARK';
UPDATE config_reward_pool_th_v2 SET remaining_reward = 4500,  max_reward = 4500  WHERE pool_id = 5 AND type = 'SENSPARK';
INSERT INTO config_reward_pool_th_v2 (pool_id, remaining_reward, type, max_reward) VALUES
  (6, 5000,  'SENSPARK', 5000),   -- Mega
  (7, 7500,  'SENSPARK', 7500),   -- Super Mega
  (8, 10000, 'SENSPARK', 10000),  -- Mystic
  (9, 12500, 'SENSPARK', 12500);  -- Super Mystic

-- COIN — keep existing 0-5 unchanged, INSERT 6-9 with default 500000 (data.md doesn't specify COIN)
INSERT INTO config_reward_pool_th_v2 (pool_id, remaining_reward, type, max_reward) VALUES
  (6, 500000, 'COIN', 500000),  -- placeholder, designer review
  (7, 500000, 'COIN', 500000),  -- placeholder, designer review
  (8, 500000, 'COIN', 500000),  -- placeholder, designer review
  (9, 500000, 'COIN', 500000);  -- placeholder, designer review

-- ============================================================================
-- 9) config_reward_level_th_v2 — Per-race per-tier reward amounts (rescaled ×0.6)
-- ============================================================================
-- Old values were calibrated for the 6-rarity TH mode. With 4 new rarities (Mega →
-- Super Mystic) and the revised per-pool caps in step 8, the table is rescaled so
-- the lowest-cap pool (Common = 250 BCOIN) drains over ~24h under expected load.
-- See docs/config_reward_level_th_v2.md for the derivation.
UPDATE config_reward_level_th_v2 SET bcoin = 0.0810474537, sen = 0.1620949074, coin = 0.3241898148 WHERE level = 1;
UPDATE config_reward_level_th_v2 SET bcoin = 0.0252912809, sen = 0.0505825617, coin = 0.1011651234 WHERE level = 2;
UPDATE config_reward_level_th_v2 SET bcoin = 0.0078587963, sen = 0.0157175926, coin = 0.0314351852 WHERE level = 3;
UPDATE config_reward_level_th_v2 SET bcoin = 0.0024290552, sen = 0.0048581104, coin = 0.0097162208 WHERE level = 4;
UPDATE config_reward_level_th_v2 SET bcoin = 0.0007461706, sen = 0.0014923411, coin = 0.0029846822 WHERE level = 5;
UPDATE config_reward_level_th_v2 SET bcoin = 0.0002275520, sen = 0.0004551040, coin = 0.0009102080 WHERE level = 6;
UPDATE config_reward_level_th_v2 SET bcoin = 0.0000687935, sen = 0.0001375870, coin = 0.0002751740 WHERE level = 7;
UPDATE config_reward_level_th_v2 SET bcoin = 0.0000205788, sen = 0.0000411576, coin = 0.0000823152 WHERE level = 8;
UPDATE config_reward_level_th_v2 SET bcoin = 0.0000060755, sen = 0.0000121509, coin = 0.0000243018 WHERE level = 9;
UPDATE config_reward_level_th_v2 SET bcoin = 0.0000017638, sen = 0.0000035276, coin = 0.0000070552 WHERE level = 10;

COMMIT;
