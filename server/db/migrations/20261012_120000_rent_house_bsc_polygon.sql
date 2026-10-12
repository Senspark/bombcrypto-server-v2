-- Rent house for BSC/Polygon.
--
-- 1. user_hero_house_rent gets a network column. BSC and Polygon BHouse/BHero token ids overlap, so
--    (house_id, hero_id) alone can match a row from the other chain. Existing rows (airdrop only) are
--    backfilled from the owning house + hero; rows that match nothing are stale and dropped.
-- 2. Rent packages for BSC/POLYGON. Prices are the BCOIN list price; the server charges BNB/POL at the
--    live config_native_rate via fn_native_price.

BEGIN;

ALTER TABLE public.user_hero_house_rent ADD COLUMN IF NOT EXISTS type character varying(20);

UPDATE public.user_hero_house_rent r
SET type = uh.type
FROM public.user_house uh
         JOIN public.user_bomber ub ON ub.uid = uh.uid AND ub.data_type = uh.type
WHERE r.type IS NULL
  AND uh.house_id = r.house_id
  AND ub.bomber_id = r.hero_id;

DELETE FROM public.user_hero_house_rent WHERE type IS NULL;

ALTER TABLE public.user_hero_house_rent ALTER COLUMN type SET NOT NULL;
ALTER TABLE public.user_hero_house_rent DROP CONSTRAINT IF EXISTS user_hero_house_rent_pk;
ALTER TABLE public.user_hero_house_rent
    ADD CONSTRAINT user_hero_house_rent_pk PRIMARY KEY (house_id, hero_id, type);

INSERT INTO public.config_package_rent_house_v2 (rarity, num_days, price, data_type)
SELECT rarity, num_days, price, net
FROM public.config_package_rent_house_v2
         CROSS JOIN (VALUES ('BSC'), ('POLYGON')) AS n(net)
WHERE data_type = 'TON'
  AND NOT EXISTS (SELECT 1 FROM public.config_package_rent_house_v2 WHERE data_type = n.net);

COMMIT;
