-- Migration: add rarity 6-9 to config_hero_upgrade_power
--
-- The rarity-expansion migration 20260505_120000_add_rarity_6_to_9.sql covered seven config tables
-- but missed this one, so rarity 6-9 have never had a row. BaseDataManager.get logs
-- "Can not find key: 6" and returns null, getPowerIncrease returns 0, and HeroHelper.getTotalPower
-- adds nothing — an upgraded Mega or above gains no power at all while paying full price. Harmless
-- until now because Upgrade heroNFT was disabled; it surfaces the moment the feature is enabled.
--
-- Values: the same [0,1,2,3,5] every seeded rarity already uses. This table has never varied by
-- rarity. Confirmed as the intended design 2026-09-08, not a placeholder.
--
-- Reading: datas is indexed by level, so a hero at level N gets datas[N] as its TOTAL power bonus,
-- not a per-upgrade delta. Level 1 gets 0; one upgrade therefore grants +1, +1, +1, +2 across the
-- four tiers.

BEGIN;

INSERT INTO config_hero_upgrade_power (rare, datas) VALUES
  (6, '[0,1,2,3,5]'),  -- Mega
  (7, '[0,1,2,3,5]'),  -- Super Mega
  (8, '[0,1,2,3,5]'),  -- Mystic
  (9, '[0,1,2,3,5]')   -- Super Mystic
ON CONFLICT (rare) DO NOTHING;

COMMIT;

-- Verify — must return 10 rows, 0..9:
--   SELECT rare, datas FROM config_hero_upgrade_power ORDER BY rare;
