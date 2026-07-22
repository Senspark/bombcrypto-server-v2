-- Migration: Widen user_create_rock.tx column to fit EVM tx hashes
-- Date: 2026-05-11
-- Applies to: bombcrypto database
--
-- Problem: schema.sql had user_create_rock.tx as character varying(20). EVM tx
-- hashes are 0x + 64 hex = 66 chars, so every CREATE_ROCK_V2 INSERT fails with:
--   ERROR: value too long for type character varying(20)
-- Triggered by logCreateRock in GameDataAccessPostgreSql.kt and by
-- sp_burn_hero_failed in schema.sql:3179.
--
-- Note on schema drift: production was previously ALTER'd by hand to
-- varchar(100), but the change never landed in schema.sql and no migration
-- captured it. Fresh DBs (e.g. BSC testnet) provisioned from schema.sql still
-- had the cramped varchar(20) and hit this error. This migration ratifies the
-- production width into source control. Target width = varchar(100) to match
-- production exactly; ALTER from varchar(100) to varchar(100) is a no-op on
-- production, while widening varchar(20) -> varchar(100) on fresh DBs is a
-- safe metadata-only change (no row rewrite for VARCHAR widening in Postgres).
--
-- Idempotent: re-running on a DB already at varchar(100) is a no-op.

ALTER TABLE public.user_create_rock
    ALTER COLUMN tx TYPE character varying(100);
