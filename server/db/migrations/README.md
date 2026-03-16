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
  -f server/db/migrations/20260316_164400_fix_race_condition_fn_sub_user_reward.sql
```

All migrations use `CREATE OR REPLACE FUNCTION`, so they are **idempotent** -- safe to run multiple times.

## For contributors

When modifying functions or schema in `schema.sql`, also create a migration script:

1. Name it `YYYYMMDD_HHMMSS_short_description.sql` using the current UTC date/time
2. Use `CREATE OR REPLACE` for function changes
3. Add a comment header explaining what the migration does and why
4. Test that the script runs cleanly on an existing database
5. Update the migration index table below

## Migration index

| Date | File | Description |
|------|------|-------------|
| 2026-03-15 | `20260315_214108_fix_race_condition_fn_sub_user_gem.sql` | Add `FOR UPDATE` lock to `fn_sub_user_gem` to prevent double-spend (PR #2) |
| 2026-03-16 | `20260316_164400_fix_race_condition_fn_sub_user_reward.sql` | Add `FOR UPDATE` lock to both `fn_sub_user_reward` overloads |
