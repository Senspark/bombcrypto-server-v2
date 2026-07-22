-- Migration: independent per-rarity SEN reward amounts for TH Mode V2
-- The original config_reward_level_th_v2_2026 seed materialised sen = bcoin * 2.
-- SEN now has its own calibrated curve (no longer a fixed multiple of BCOIN),
-- so this migration overwrites the sen column with the explicit per-(rarity,level)
-- values. bcoin and coin are left untouched.
-- Source values: docs/config_reward_level_th_v2.md (# Reward for SEN)
-- After applying, hot-reload the running server (no restart needed):
--   redis-cli XADD "SV:ADMIN_COMMAND" "*" cmd reload_config_th_mode_v2

BEGIN;

UPDATE config_reward_level_th_v2_2026 AS r
SET sen = t.sen
FROM (VALUES
  -- Common (rarity 0)
  (0,  1, 0.3858024691),
  (0,  2, 0.0643004115),
  (0,  3, 0.0107167353),
  (0,  4, 0.0017861225),
  (0,  5, 0.0002976871),
  (0,  6, 0.0000496145),
  (0,  7, 0.0000082691),
  (0,  8, 0.0000013782),
  (0,  9, 0.0000002297),
  (0, 10, 0.0000000383),
  -- Rare (rarity 1)
  (1,  1, 0.7716049383),
  (1,  2, 0.1286008230),
  (1,  3, 0.0214334705),
  (1,  4, 0.0035722451),
  (1,  5, 0.0005953742),
  (1,  6, 0.0000992290),
  (1,  7, 0.0000165382),
  (1,  8, 0.0000027564),
  (1,  9, 0.0000004594),
  (1, 10, 0.0000000766),
  -- Super Rare (rarity 2)
  (2,  1, 1.5432098770),
  (2,  2, 0.2572016461),
  (2,  3, 0.0428669410),
  (2,  4, 0.0071444902),
  (2,  5, 0.0011907484),
  (2,  6, 0.0001984581),
  (2,  7, 0.0000330763),
  (2,  8, 0.0000055127),
  (2,  9, 0.0000009188),
  (2, 10, 0.0000001531),
  -- Epic (rarity 3)
  (3,  1, 2.3148148150),
  (3,  2, 0.3858024691),
  (3,  3, 0.0643004115),
  (3,  4, 0.0107167353),
  (3,  5, 0.0017861225),
  (3,  6, 0.0002976871),
  (3,  7, 0.0000496145),
  (3,  8, 0.0000082691),
  (3,  9, 0.0000013782),
  (3, 10, 0.0000002297),
  -- Legend (rarity 4)
  (4,  1, 3.0864197530),
  (4,  2, 0.5144032922),
  (4,  3, 0.0857338820),
  (4,  4, 0.0142889803),
  (4,  5, 0.0023814967),
  (4,  6, 0.0003969161),
  (4,  7, 0.0000661527),
  (4,  8, 0.0000110254),
  (4,  9, 0.0000018376),
  (4, 10, 0.0000003063),
  -- Super Legend (rarity 5)
  (5,  1, 3.4722222220),
  (5,  2, 0.5787037037),
  (5,  3, 0.0964506173),
  (5,  4, 0.0160751029),
  (5,  5, 0.0026791838),
  (5,  6, 0.0004465306),
  (5,  7, 0.0000744218),
  (5,  8, 0.0000124036),
  (5,  9, 0.0000020673),
  (5, 10, 0.0000003445),
  -- Mega (rarity 6)
  (6,  1, 3.8580246910),
  (6,  2, 0.6430041152),
  (6,  3, 0.1071673525),
  (6,  4, 0.0178612254),
  (6,  5, 0.0029768709),
  (6,  6, 0.0004961452),
  (6,  7, 0.0000826909),
  (6,  8, 0.0000137818),
  (6,  9, 0.0000022970),
  (6, 10, 0.0000003828),
  -- Super Mega (rarity 7)
  (7,  1, 5.7870370370),
  (7,  2, 0.9645061728),
  (7,  3, 0.1607510288),
  (7,  4, 0.0267918381),
  (7,  5, 0.0044653064),
  (7,  6, 0.0007442177),
  (7,  7, 0.0001240363),
  (7,  8, 0.0000206727),
  (7,  9, 0.0000034455),
  (7, 10, 0.0000005742),
  -- Mystic (rarity 8)
  (8,  1, 7.7160493830),
  (8,  2, 1.2860082310),
  (8,  3, 0.2143347051),
  (8,  4, 0.0357224508),
  (8,  5, 0.0059537418),
  (8,  6, 0.0009922903),
  (8,  7, 0.0001653817),
  (8,  8, 0.0000275636),
  (8,  9, 0.0000045939),
  (8, 10, 0.0000007657),
  -- Super Mystic (rarity 9)
  (9,  1, 9.6450617280),
  (9,  2, 1.6075102880),
  (9,  3, 0.2679183813),
  (9,  4, 0.0446530636),
  (9,  5, 0.0074421773),
  (9,  6, 0.0012403629),
  (9,  7, 0.0002067271),
  (9,  8, 0.0000344545),
  (9,  9, 0.0000057424),
  (9, 10, 0.0000009571)
) AS t(rarity, level, sen)
WHERE r.rarity = t.rarity::smallint
  AND r.level  = t.level;

-- SEN daily pool caps (config_reward_pool_th_v2, type='SENSPARK').
-- The per-rarity SEN curve above is no longer 2x BCOIN, so each pool's daily cap
-- is recalibrated. remaining_reward is set to (new max_reward - old max_reward):
-- today's refill already ran on the old caps, so this only tops up the delta for
-- the rest of today; tomorrow's refillTHModeV2Pool resets remaining to the new max.
UPDATE config_reward_pool_th_v2 AS p
SET max_reward       = t.max_reward,
    remaining_reward = t.remaining_reward
FROM (VALUES
  (0, 3333,  2833),
  (1, 6667,  5667),
  (2, 13333, 11333),
  (3, 20000, 17000),
  (4, 26667, 22667),
  (5, 30000, 25500),
  (6, 33333, 28333),
  (7, 50000, 42500),
  (8, 66667, 56667),
  (9, 83333, 70833)
) AS t(pool_id, max_reward, remaining_reward)
WHERE p.type = 'SENSPARK'
  AND p.pool_id = t.pool_id;

COMMIT;
