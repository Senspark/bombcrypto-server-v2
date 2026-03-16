-- Migration: Create 2027 yearly partition for logs.user_block_reward
-- Date: 2026-03-16
-- Applies to: bombcrypto database
--
-- The logs.user_block_reward table is partitioned by RANGE on changed_at.
-- Existing partitions: 2025, 2026. This creates the 2027 partition.
-- Without this partition, all log INSERTs will fail after 2027-01-01.
--
-- Note: Adjust the timezone offset (+07) to match your server's timezone if different.

-- Index is created automatically from the parent table's index definition.
CREATE TABLE IF NOT EXISTS logs.user_block_reward_2027 PARTITION OF logs.user_block_reward
    FOR VALUES FROM ('2027-01-01 00:00:00+07') TO ('2028-01-01 00:00:00+07');
