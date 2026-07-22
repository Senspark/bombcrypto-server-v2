-- Migration: Fix PVP season auto-provisioning (rank tables + reward backfill)
-- Date: 2026-05-08
-- Applies to: bombcrypto database
--
-- Two fixes bundled — both must ship together because the second one can't run
-- without the first being re-callable:
--
-- 1. sp_setup_next_pvp_season — add backfill loop for past unprocessed seasons.
--
--    The original procedure picked a single _current_season via
--    ORDER BY ps.end_date DESC LIMIT 1. Once a future season row was inserted into
--    config_ranking_season (which PvpSeasonManager.addNewSeason() does proactively
--    as soon as the previous season is closed), the SELECT always picked that
--    newest row. Consequence: any prior season that ended without
--    summary_pvp_ranking_reward running at the right moment was skipped
--    permanently, leaving user_pvp_rank_reward_ss_<id> uncreated and
--    is_calculated_reward = FALSE forever.
--
--    Fix: prepend a backfill loop that iterates every season with
--    (is_calculated_reward = FALSE AND end_date < NOW()), creating the reward
--    table and populating it from the corresponding rank table. Idempotent:
--    CREATE TABLE/SEQUENCE IF NOT EXISTS + WHERE excludes already-calculated.
--
-- 2. summary_pvp_ranking_reward — self-bootstrap schedule_status row.
--
--    The procedure SELECTed last_update from schedule_status WHERE schedule='PVP',
--    then guarded the body on (last_update + interval) < NOW(). On a fresh DB
--    (or any environment where the row was never seeded), the SELECT returned no
--    row → variables stayed NULL → guard evaluated to NULL → IF body skipped →
--    the INSERT that would have created the row was inside the IF body too.
--    Deadlock: row never gets created, sp_setup_next_pvp_season never gets
--    called from the scheduler.
--
--    Fix: prepend an idempotent INSERT ... ON CONFLICT DO NOTHING with last_update
--    set to epoch, so the first invocation seeds the row and immediately passes
--    the guard.

CREATE OR REPLACE PROCEDURE public.sp_setup_next_pvp_season()
    LANGUAGE plpgsql
    AS $$
DECLARE
    _pending              RECORD;
    _current_season       INT;
    _next_season          INT;
    _current_season_ended BOOLEAN;
BEGIN
    -- Backfill: tính reward cho MỌI season đã end nhưng is_calculated_reward = FALSE.
    FOR _pending IN
        SELECT id
        FROM config_ranking_season
        WHERE is_calculated_reward = FALSE
          AND end_date < (NOW() AT TIME ZONE 'utc')
        ORDER BY id
    LOOP
        EXECUTE FORMAT('CREATE SEQUENCE IF NOT EXISTS user_pvp_rank_ss_%1$s_seq START 1;', _pending.id);
        EXECUTE FORMAT('CREATE TABLE IF NOT EXISTS user_pvp_rank_ss_%1$s
                       (
                            LIKE user_pvp_rank_template INCLUDING ALL
                       )', _pending.id);
        EXECUTE FORMAT('CREATE TABLE IF NOT EXISTS user_pvp_rank_reward_ss_%1$s
                       (
                            LIKE user_pvp_rank_reward_template INCLUDING ALL
                       )', _pending.id);
        EXECUTE FORMAT('TRUNCATE TABLE user_pvp_rank_reward_ss_%1$s;', _pending.id);
        EXECUTE FORMAT('INSERT INTO user_pvp_rank_reward_ss_%1$s (uid,
                                                                  rank,
                                                                  reward,
                                                                  total_match)
                                          SELECT r.uid,
                                                 r.rank,
                                                 bsr.reward,
                                                 r.total_match
                                          FROM user_pvp_rank_ss_%1$s AS r
                                                   LEFT JOIN config_pvp_ranking_reward bsr
                                                             ON r.rank >= bsr.rank_min AND r.rank <= bsr.rank_max AND r.total_match >= 30
                                          WHERE r.rank IS NOT NULL', _pending.id);

        UPDATE config_ranking_season
        SET is_calculated_reward = TRUE
        WHERE id = _pending.id;
    END LOOP;

    -- Xác định current/next để tạo bảng rank cho mùa đang diễn ra (và mùa kế).
    SELECT ps.id             AS current_season,
           ns.id             AS next_season,
           CASE
               WHEN NOW() AT TIME ZONE 'utc' BETWEEN ps.start_date AND ps.end_date
                   THEN FALSE
               ELSE TRUE END AS current_season_ended
    INTO _current_season,
        _next_season,
        _current_season_ended
    FROM config_ranking_season AS ps
             LEFT JOIN config_ranking_season AS ns ON ps.id = ns.id - 1
    WHERE NOW() AT TIME ZONE 'utc' BETWEEN ps.start_date AND ps.end_date
       OR NOW() AT TIME ZONE 'utc' BETWEEN ps.end_date AND ns.start_date
       OR NOW() AT TIME ZONE 'utc' > ps.end_date
    ORDER BY ps.end_date DESC
    LIMIT 1;

    -- tạo bảng ranking cho mùa hiện tại nếu chưa có
    IF _current_season IS NOT NULL THEN
        EXECUTE FORMAT('CREATE SEQUENCE if NOT EXISTS user_pvp_rank_ss_%1$s_seq START 1;', _current_season);
        EXECUTE FORMAT('CREATE TABLE IF NOT EXISTS user_pvp_rank_ss_%1$s
                       (
                            LIKE user_pvp_rank_template INCLUDING ALL
                       )', _current_season);
    END IF;

    -- nếu có mùa tiếp theo thì tạo bảng cho mùa tiếp theo
    IF _next_season IS NOT NULL AND _current_season_ended THEN
        EXECUTE FORMAT('CREATE SEQUENCE if NOT EXISTS user_pvp_rank_ss_%1$s_seq START 1;', _next_season);
        EXECUTE FORMAT('CREATE TABLE IF NOT EXISTS user_pvp_rank_ss_%1$s
                       (
                            LIKE user_pvp_rank_template INCLUDING ALL
                       )', _next_season);
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END
$$;

CREATE OR REPLACE PROCEDURE public.summary_pvp_ranking_reward()
    LANGUAGE plpgsql
    AS $$
DECLARE
    i_season         INT;
    i_last_update    timestamp with time zone;
    i_interval_value INT;
BEGIN
    -- Self-bootstrap: đảm bảo row 'PVP' tồn tại trước khi đọc.
    -- Nếu schedule_status rỗng, SELECT trả NULL → guard "(NULL + ...) < NOW()" = NULL
    -- → IF body skip vĩnh viễn → INSERT bên trong IF không bao giờ chạy → deadlock.
    INSERT INTO schedule_status(schedule, status, last_update)
    VALUES ('PVP', 0, '2000-01-01 00:00:00+00')
    ON CONFLICT (schedule) DO NOTHING;

    -- Check if interval + last_update < now
    SELECT last_update,
           COALESCE(interval_update, 300)
    INTO i_last_update,
        i_interval_value
    FROM schedule_status
    WHERE schedule = 'PVP';

    IF (i_last_update + (i_interval_value * interval '1 second')) < NOW() THEN
        UPDATE schedule_status
        SET last_update = NOW()
        WHERE schedule = 'PVP';

        SELECT ps.id
        INTO i_season
        FROM config_ranking_season AS ps
                 LEFT JOIN config_ranking_season AS ns ON ps.id = ns.id - 1
        WHERE NOW() AT TIME ZONE 'utc' BETWEEN ps.start_date AND ps.end_date
           OR NOW() AT TIME ZONE 'utc' BETWEEN ps.end_date AND ns.start_date
           OR NOW() AT TIME ZONE 'utc' > ps.end_date
        ORDER BY ps.end_date DESC
        LIMIT 1;

        INSERT INTO schedule_status(schedule, status, season)
        VALUES ('PVP', 1, i_season)
        ON CONFLICT (schedule) DO UPDATE SET status = 1, season = i_season;
        CALL sp_update_user_pvp_rank();
        CALL sp_setup_next_pvp_season();
        UPDATE schedule_status SET status = 0 WHERE schedule = 'PVP';
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END
$$;
