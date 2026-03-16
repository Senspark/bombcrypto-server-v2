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
