# Database Migrations

Standalone SQL scripts for applying incremental changes to an **existing** `bombcrypto` database without a full schema reload.

## When to use

- **Fresh deploy**: Load `schema.sql` directly -- it already contains all changes. Migrations are not needed.
- **Existing database**: Run the relevant migration scripts in order to apply changes without rebuilding.

## How to apply

Run migrations in chronological order (the timestamp prefix ensures correct ordering):

```bash
psql -h localhost -U postgres -d bombcrypto \
  -f server/db/migrations/20260315_214108_fix_race_condition_fn_sub_user_gem.sql \
  -f server/db/migrations/20260316_164400_fix_race_condition_fn_sub_user_reward.sql \
  -f server/db/migrations/20260316_170000_sync_functions_to_production.sql \
  -f server/db/migrations/20260316_170100_create_2027_partition.sql \
  -f server/db/migrations/20260316_170200_drop_old_template_partitioning.sql
```

All function migrations use `CREATE OR REPLACE`, so they are **idempotent** -- safe to run multiple times.

## For contributors

When modifying functions or schema in `schema.sql`, also create a migration script:

1. Name it `YYYYMMDD_HHMMSS_short_description.sql` using the current UTC date/time
2. Use `CREATE OR REPLACE` for function changes
3. Add a comment header explaining what the migration does and why
4. Test that the script runs cleanly on an existing database
5. Update the migration index table below

## Partitioning: `logs.user_block_reward`

The `logs.user_block_reward` table uses **native PostgreSQL range partitioning** on the `changed_at` column, with yearly partitions.

**Current partitions:**

| Partition | Range |
|-----------|-------|
| `logs.user_block_reward_2025` | 2025-01-01 to 2026-01-01 |
| `logs.user_block_reward_2026` | 2026-01-01 to 2027-01-01 |
| `logs.user_block_reward_2027` | 2027-01-01 to 2028-01-01 |

**A new partition must be created before each new year.** Without it, all audit log INSERTs will fail. Use this template:

```sql
-- Index is created automatically from the parent table's index definition.
CREATE TABLE logs.user_block_reward_YYYY PARTITION OF logs.user_block_reward
    FOR VALUES FROM ('YYYY-01-01 00:00:00+07') TO ('NEXT_YYYY-01-01 00:00:00+07');
```


To check existing partitions:

```sql
SELECT tablename FROM pg_tables
WHERE schemaname = 'logs' AND tablename LIKE 'user_block_reward_%'
ORDER BY tablename;
```

## Migration index

| Date | File | Description |
|------|------|-------------|
| 2026-03-15 | `20260315_214108_fix_race_condition_fn_sub_user_gem.sql` | Add `FOR UPDATE` lock to `fn_sub_user_gem` to prevent double-spend (PR #2) |
| 2026-03-16 | `20260316_164400_fix_race_condition_fn_sub_user_reward.sql` | Add `FOR UPDATE` lock to both `fn_sub_user_reward` overloads |
| 2026-03-16 | `20260316_170000_sync_functions_to_production.sql` | Sync claim functions and log table references to match production |
| 2026-03-16 | `20260316_170100_create_2027_partition.sql` | Create 2027 yearly partition for `logs.user_block_reward` |
| 2026-03-16 | `20260316_170200_drop_old_template_partitioning.sql` | Drop deprecated template-based partitioning system |
| 2026-04-01 | `20260401_000001_sync_functions_to_production.sql` | Sync 17 out-of-date functions and add 3 missing ones so a fresh `schema.sql` matches actual production state (led by `fn_pvp_save_fixture_match_to_log`) |
| 2026-05-05 | `20260505_120000_add_rarity_6_to_9.sql` | Add rarity 6-9 (Mega, Super Mega, Mystic, Super Mystic) to BHero config tables (`config_burn_hero` and related). Phase 2 of the rarity-expansion work; some values are placeholders pending designer review |
| 2026-05-08 | `20260508_100000_split_claim_sync_from_save.sql` | Split sync logic out of `sp_save_user_claim_reward_data` (Branch B bug fix). Adds `sp_sync_user_claim_synced` + `fn_sync_user_claim_synced`, drops `sp_fix_user_claim_reward_data`. See `CLAIM_TOKENS_REFACTOR_DESIGN.md` |
| 2026-05-08 | `20260508_150000_fix_pvp_season_reward_backfill.sql` | Fix PVP season auto-provisioning: `sp_setup_next_pvp_season` gains a backfill loop for past unprocessed seasons (previously only ever picked the newest season row, so seasons that closed without `summary_pvp_ranking_reward` running were silently skipped) |
| 2026-05-11 | `20260511_040403_widen_user_create_rock_tx.sql` | Widen `user_create_rock.tx` from varchar(20) to varchar(100) (matches production, which was hand-ALTER'd long ago and never captured in source). Old width truncated 66-char EVM tx hashes and broke `CREATE_ROCK_V2` inserts on fresh DBs (e.g. BSC testnet) |
| 2026-05-29 | `20260529_120000_per_rarity_reward_levels.sql` | Replace the single global TH Mode V2 tier table (`config_reward_level_th_v2`) with two tables: tier sizing (shared across rarities) and per-rarity reward amounts. Old table kept as DEPRECATED for audit/rollback. Source values: `docs/config_reward_level_th_v2.md` |
| 2026-06-22 | `20260622_120000_th_v2_sen_per_rarity.sql` | Give TH Mode V2 SEN rewards their own calibrated per-(rarity,level) curve instead of `sen = bcoin * 2`; overwrites the `sen` column only, `bcoin`/`coin` untouched. Requires a `reload_config_th_mode_v2` hot-reload after applying |
| 2026-06-29 | `20260629_120000_fix_hero_tr_type_multi_quantity.sql` | Fix `fn_insert_new_hero_tr` writing multiple `HERO` rows when acquiring `_quantity >= 2` heroes of the same (charactor, color) at once. Thread the GENERATE_SERIES per-row index into the CASE so only the first row becomes `HERO` (when none exists); rest are `SOUL` |
| 2026-07-02 | `20260702_120000_fix_create_rock_upsert_idempotent.sql` | Make `sp_modify_rock_from_user_wallet` idempotent + conflict-safe: skip when `(tx, uid)` already `DONE`, and upsert on `(tx, uid)` so a prior `PENDING`/`FALSE`/`ERROR` row (e.g. the game's rejected verify of a smart-account delegated burn) is flipped to `DONE` instead of raising `duplicate key ... user_create_rock_pk` on pay-rock retry |
| 2026-07-08 | `20260708_120000_add_cross_chain_bridge_server_submit.sql` | Cross-chain bridge, complete feature (supersedes the deleted `20260701_110214_add_cross_chain_bridge.sql`, which only ran on local dev). Adds `cross_chain_bridge_sync` / `_pending` / `_history` tables + `fn_bridge_sync_deposit` / `fn_bridge_request_withdraw` / `fn_bridge_sync_withdraw`; seeds `bridge_deposit_enabled` / `bridge_withdraw_enabled` kill-switch flags. Phase-8 "Option 2" (server-submitted withdraw): `_pending` gains a tx lifecycle (`status`/`tx_hash`/`nonce`/`submitting_at`/`attempts`/`last_error`/`updated_at`) + `fn_bridge_refund` / `fn_bridge_mark_submitted` / `fn_bridge_mark_failed`. Bridge balance lives in `user_block_reward` at `(uid, reward_type ∈ {BCOIN_BRIDGE, SEN_BRIDGE}, type='BP')`. See `docs/cross-chain-bridge-phase8-server-submit-plan.md` §9. **Local dev DB (ran the old file): run the one-time `server/db/PATCH_bridge_server_submit_upgrade.sql` instead, then delete it.** |
