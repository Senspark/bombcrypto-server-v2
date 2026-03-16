-- Migration: Drop deprecated template-based partitioning system
-- Date: 2026-03-16
-- Applies to: bombcrypto database
--
-- The old daily partitioning system (logs.logs_user_block_reward_template + trigger)
-- has been fully replaced by native PostgreSQL range partitioning on logs.user_block_reward.
-- No functions insert into the template table anymore.
--
-- This migration removes the dead code:
-- 1. The BEFORE INSERT trigger on the template table
-- 2. The trigger function that routed inserts to daily partitions
-- 3. The helper function that created daily partition tables
-- 4. The template table itself
--
-- IMPORTANT: Verify no application code references these objects before running.
-- Run this query first to confirm:
--   SELECT proname FROM pg_proc WHERE proname LIKE '%logs_user_block_reward%';
--   SELECT tgname FROM pg_trigger WHERE tgname LIKE '%logs_user_block_reward%';

-- 1. Drop trigger
DROP TRIGGER IF EXISTS "logs.insert_logs_user_block_reward_trigger" ON logs.logs_user_block_reward_template;

-- 2. Drop trigger function
DROP FUNCTION IF EXISTS logs.logs_user_block_reward_insert_trigger();

-- 3. Drop partition creation helper
DROP FUNCTION IF EXISTS logs.logs_user_block_reward_create_partition_from_template(text);

-- 4. Drop template table
DROP TABLE IF EXISTS logs.logs_user_block_reward_template;
