-- Migration: Make fn_calculate_package_auto_price prunable and sargable
-- Date: 2026-08-25
-- Applies to: bombcrypto database
--
-- PROBLEM
-- The 7-day "mined value" lookup filtered with DATE(changed_at) BETWEEN ... .
-- logs.user_block_reward is RANGE-partitioned on changed_at (yearly), and wrapping
-- the partition key in DATE() makes the predicate opaque to the planner: it can
-- neither prune partitions nor use an index on changed_at. Every call therefore
-- seq-scanned EVERY yearly partition to read 7 days of data. Measured on prod
-- 2026-08-25: 2025 = 34.7M rows / 3809 MB, 2026 = 35.5M rows / 3581 MB, 2027 empty.
-- So ~7.4 GB and ~70M rows were scanned per call, to sum at most a few dozen rows.
--
-- Under concurrency this pins the whole instance on I/O: the scanned data does not
-- fit shared_buffers (IO/DataFileRead), and concurrent backends queue on the very
-- same pages (IPC/BufferIo). Observed on prod 2026-08-25: 21 concurrent calls, the
-- oldest running 50 minutes, disk %util 98.8, PSI io full avg10 60%.
--
-- FIX
-- Rewrite the predicate as a half-open range on the raw column:
--     changed_at >= (CURRENT_DATE - 7)::timestamptz
--     changed_at <  (CURRENT_DATE)::timestamptz
--
-- This is EXACTLY equivalent to the old one, not merely similar:
--   * DATE(changed_at) casts using the session TimeZone; so does date::timestamptz.
--     Both sides therefore use the same timezone, whatever it is set to.
--   * The old upper bound CURRENT_DATE - INTERVAL '1 day' was inclusive of that
--     whole day, which ends exactly at the start of CURRENT_DATE.
-- No row is added to or removed from the result set. Prices do not change.
--
-- TIMEZONE, IN DETAIL (production runs TZ='Asia/Bangkok')
-- Column type: changed_at timestamp with time zone, i.e. timestamptz.
--   * Old form: DATE(changed_at) renders the timestamptz into the SESSION TimeZone
--     and truncates to a calendar date. CURRENT_DATE is also the session's date.
--   * New form: (CURRENT_DATE - 7)::timestamptz resolves midnight of that calendar
--     date in the SAME session TimeZone.
--   Both forms are anchored to the same clock, so they select the same rows under
--   Asia/Bangkok, under UTC, or under anything else. The fix deliberately does NOT
--   pin a timezone: pinning one would change which rows match and therefore change
--   prices, which is exactly what must not happen here.
--
--   Consequence for testing: run any EXPLAIN / row-count comparison in a session
--   with TimeZone set to Asia/Bangkok, matching the game server. A psql session
--   defaulting to UTC will legitimately return a different 7-day window -- that is
--   a property of the original code, not a regression introduced here.
--
-- SEPARATE PRE-EXISTING BUG, NOT FIXED HERE (worth a follow-up ticket)
--   The column default is `now() AT TIME ZONE 'utc'`, which yields a *naive*
--   timestamp holding UTC wall-clock time; assigning it to a timestamptz column
--   re-interprets it in the session TimeZone. Under Asia/Bangkok every logged row is
--   therefore stored 7 hours earlier than the instant it actually happened. This
--   affects the data, not this query's correctness -- old and new predicates read
--   the same skewed rows identically -- so it is out of scope for an outage fix.
--   Changing it would shift the 7-day window and move prices, and must be done
--   deliberately, not during an incident.
--
-- The bounds are interpolated as literals rather than passed as parameters so the
-- planner sees constants and prunes at plan time. This keeps pruning working on
-- PostgreSQL versions without runtime partition pruning. The values are computed by
-- the server from CURRENT_DATE, never from user input.
--
-- quote_literal(_uid) is also dropped: _uid is already an integer, quoting it forced
-- a distinct query text per user and defeated any plan reuse.
--
-- This migration is metadata-only. It rewrites one function body, takes no heavy
-- lock, touches no table data, and is safe to run while the instance is loaded.
--
-- ORDER OF OPERATIONS ON A STALLED INSTANCE
--   1. Stop the game server, cancel the piled-up backends (they only run a SELECT
--      SUM, so cancelling rolls back no game state).
--   2. Apply this migration.
--   3. Then 20260825_100100_index_user_block_reward_save_game.sql.
--   4. Restart the game server.
--   Step 2 alone cuts the scan from all yearly partitions down to one, so if the
--   window runs short, stopping after it is a valid outcome.
--
-- Production database is `bombcrypto2` (api/login/.env.example and the observed
-- pg_stat_activity). server/.env.example's `bombcrypto` is the local dev name.

CREATE OR REPLACE FUNCTION public.fn_calculate_package_auto_price(_uid integer, package_json json)
    RETURNS TABLE
            (
                package       character varying,
                num_days      integer,
                price_percent double precision,
                min_price     double precision,
                price         double precision
            )
    LANGUAGE plpgsql
AS
$$
DECLARE
    sql_query   TEXT;
    mined_value DECIMAL(10, 4) := 0.0;
    mined_day   DECIMAL(10, 4);
    _from_ts    timestamptz   := (CURRENT_DATE - 7)::timestamptz;
    _to_ts      timestamptz   := (CURRENT_DATE)::timestamptz;
BEGIN
    -- Half-open range on the bare partition key: prunes to the current year's
    -- partition and can use an index on (uid, changed_at).
    sql_query := 'SELECT COALESCE(SUM(values_changed), 0)' ||
                 ' FROM logs.user_block_reward' ||
                 ' WHERE uid = ' || _uid ||
                 ' AND reward_type = ''BCOIN''' ||
                 ' AND reason = ''Save game''' ||
                 ' AND changed_at >= ' || quote_literal(_from_ts) || '::timestamptz' ||
                 ' AND changed_at <  ' || quote_literal(_to_ts) || '::timestamptz';
    EXECUTE sql_query INTO mined_day;
    mined_value := mined_value + mined_day;

    RETURN QUERY
        SELECT r.package,
               r.num_days,
               r.price_percent::double precision,
               r.min_price::double precision,
               GREATEST(r.min_price, ROUND(mined_value * (r.price_percent / 100)::DECIMAL(10, 4)))::double precision
        FROM json_to_recordset(package_json::json) AS r(package VARCHAR(40), num_days INT,
                                                        price_percent double precision, min_price double precision);
END;
$$;
