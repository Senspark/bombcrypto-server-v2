-- Migration: per-rarity reward levels for TH Mode V2
-- Replaces the single global tier table (config_reward_level_th_v2) with two
-- tables: one for tier sizing (num_users per level, shared across rarities)
-- and one for reward amounts (per rarity, per level). Splitting prevents the
-- num_users column from accidentally diverging across rarities.
-- Old table is left in place (DEPRECATED) for audit/rollback.
-- Source values: docs/config_reward_level_th_v2.md

BEGIN;

-- ============================================================================
-- 1) config_reward_level_th_v2 — mark DEPRECATED
-- ============================================================================
-- This table held a single global tier matrix (one bcoin/sen/coin per level)
-- shared across all rarities. With per-pool caps spanning 250..6250 BCOIN/day,
-- a single matrix cannot drain low-cap and high-cap pools sensibly at the same
-- time (low-cap drains in minutes, high-cap never drains).
-- The Kotlin loader is moved to the new tables below; no DROP here so historical
-- rows remain queryable.
COMMENT ON TABLE public.config_reward_level_th_v2 IS
  'DEPRECATED 2026-05-29 — replaced by config_reward_level_th_v2_2026 + '
  'config_reward_th_level_num_users. No longer read by '
  'THModeDataAccess.loadRewardLevelConfig. Kept for audit/rollback only.';

-- ============================================================================
-- 2) config_reward_th_level_num_users — tier sizing (shared across rarities)
-- ============================================================================
-- num_users is the number of top-scoring heroes that receive reward for a tier.
-- It is the same across every rarity (tier 1 always covers top 3, tier 2 covers
-- next 9, ...). Keeping it in its own table prevents accidental divergence
-- between rarities.
CREATE TABLE public.config_reward_th_level_num_users (
    level     int PRIMARY KEY,
    num_users int NOT NULL
);

INSERT INTO config_reward_th_level_num_users (level, num_users) VALUES
  ( 1,     3),
  ( 2,     9),
  ( 3,    27),
  ( 4,    81),
  ( 5,   243),
  ( 6,   729),
  ( 7,  2187),
  ( 8,  6561),
  ( 9, 19683),
  (10, 59049);

-- ============================================================================
-- 3) config_reward_level_th_v2_2026 — per-rarity per-level reward amounts
-- ============================================================================
-- Each rarity gets its own 10-tier reward column, calibrated so a fully
-- populated pool drains its 24h cap exactly: sum(num_users × bcoin) ≈ cap/1440.
-- Within a rarity, each tier is 1/6 of the previous (numUsers grows ×3, so each
-- tier contributes ×0.5 of the previous tier's total).
-- SEN = 2 × BCOIN, COIN = 4 × BCOIN — relationship is materialised at insert
-- time, can be tuned per-row later without touching code.
-- FK on level ensures a reward row cannot reference a non-existent tier.

CREATE TABLE public.config_reward_level_th_v2_2026 (
    rarity smallint         NOT NULL,
    level  int              NOT NULL,
    bcoin  double precision,
    sen    double precision,
    coin   double precision,
    CONSTRAINT config_reward_level_th_v2_2026_pk PRIMARY KEY (rarity, level),
    CONSTRAINT config_reward_level_th_v2_2026_level_fk
        FOREIGN KEY (level) REFERENCES public.config_reward_th_level_num_users(level)
);

INSERT INTO config_reward_level_th_v2_2026 (rarity, level, bcoin, sen, coin)
SELECT
    rarity::smallint,
    level,
    bcoin::double precision,
    (bcoin * 2)::double precision AS sen,
    (bcoin * 4)::double precision AS coin
FROM (VALUES
  -- Common (rarity 0, cap 250 BCOIN/day)
  (0,  1, 0.0289351852),
  (0,  2, 0.0048225309),
  (0,  3, 0.0008037551),
  (0,  4, 0.0001339592),
  (0,  5, 0.0000223265),
  (0,  6, 0.0000037211),
  (0,  7, 0.0000006202),
  (0,  8, 0.0000001034),
  (0,  9, 0.0000000172),
  (0, 10, 0.0000000029),
  -- Rare (rarity 1, cap 500)
  (1,  1, 0.0578703704),
  (1,  2, 0.0096450617),
  (1,  3, 0.0016075103),
  (1,  4, 0.0002679184),
  (1,  5, 0.0000446531),
  (1,  6, 0.0000074422),
  (1,  7, 0.0000012404),
  (1,  8, 0.0000002067),
  (1,  9, 0.0000000345),
  (1, 10, 0.0000000057),
  -- Super Rare (rarity 2, cap 1000)
  (2,  1, 0.1157407407),
  (2,  2, 0.0192901235),
  (2,  3, 0.0032150206),
  (2,  4, 0.0005358368),
  (2,  5, 0.0000893061),
  (2,  6, 0.0000148844),
  (2,  7, 0.0000024807),
  (2,  8, 0.0000004135),
  (2,  9, 0.0000000689),
  (2, 10, 0.0000000115),
  -- Epic (rarity 3, cap 1500)
  (3,  1, 0.1736111111),
  (3,  2, 0.0289351852),
  (3,  3, 0.0048225309),
  (3,  4, 0.0008037551),
  (3,  5, 0.0001339592),
  (3,  6, 0.0000223265),
  (3,  7, 0.0000037211),
  (3,  8, 0.0000006202),
  (3,  9, 0.0000001034),
  (3, 10, 0.0000000172),
  -- Legend (rarity 4, cap 2000)
  (4,  1, 0.2314814815),
  (4,  2, 0.0385802469),
  (4,  3, 0.0064300412),
  (4,  4, 0.0010716735),
  (4,  5, 0.0001786123),
  (4,  6, 0.0000297687),
  (4,  7, 0.0000049615),
  (4,  8, 0.0000008269),
  (4,  9, 0.0000001378),
  (4, 10, 0.0000000230),
  -- Super Legend (rarity 5, cap 2250)
  (5,  1, 0.2604166667),
  (5,  2, 0.0434027778),
  (5,  3, 0.0072337963),
  (5,  4, 0.0012056327),
  (5,  5, 0.0002009388),
  (5,  6, 0.0000334898),
  (5,  7, 0.0000055816),
  (5,  8, 0.0000009303),
  (5,  9, 0.0000001550),
  (5, 10, 0.0000000258),
  -- Mega (rarity 6, cap 2500)
  (6,  1, 0.2893518519),
  (6,  2, 0.0482253086),
  (6,  3, 0.0080375514),
  (6,  4, 0.0013395919),
  (6,  5, 0.0002232653),
  (6,  6, 0.0000372109),
  (6,  7, 0.0000062018),
  (6,  8, 0.0000010336),
  (6,  9, 0.0000001723),
  (6, 10, 0.0000000287),
  -- Super Mega (rarity 7, cap 3750)
  (7,  1, 0.4340277778),
  (7,  2, 0.0723379630),
  (7,  3, 0.0120563272),
  (7,  4, 0.0020093879),
  (7,  5, 0.0003348980),
  (7,  6, 0.0000558163),
  (7,  7, 0.0000093027),
  (7,  8, 0.0000015505),
  (7,  9, 0.0000002584),
  (7, 10, 0.0000000431),
  -- Mystic (rarity 8, cap 5000)
  (8,  1, 0.5787037037),
  (8,  2, 0.0964506173),
  (8,  3, 0.0160751029),
  (8,  4, 0.0026791838),
  (8,  5, 0.0004465306),
  (8,  6, 0.0000744218),
  (8,  7, 0.0000124036),
  (8,  8, 0.0000020673),
  (8,  9, 0.0000003445),
  (8, 10, 0.0000000574),
  -- Super Mystic (rarity 9, cap 6250)
  (9,  1, 0.7233796296),
  (9,  2, 0.1205632716),
  (9,  3, 0.0200938786),
  (9,  4, 0.0033489798),
  (9,  5, 0.0005581633),
  (9,  6, 0.0000930272),
  (9,  7, 0.0000155045),
  (9,  8, 0.0000025841),
  (9,  9, 0.0000004307),
  (9, 10, 0.0000000718)
) AS t(rarity, level, bcoin);

COMMIT;
