-- ============================================
-- RESET PVP SEASON TO 1 (FULL CLEANUP)
-- ============================================

-- 1. Reset config_ranking_season to only season 1
DELETE FROM public.config_ranking_season;
INSERT INTO public.config_ranking_season (id, start_date, end_date, modify_date, is_calculated_reward)
VALUES (1,
        DATE_TRUNC('month', NOW() AT TIME ZONE 'utc'),
        DATE_TRUNC('month', NOW() AT TIME ZONE 'utc') + INTERVAL '26 days 23 hours 59 minutes 59 seconds',
        NULL,
        FALSE);

-- 2. Truncate season 1 ranking table (fresh start)
TRUNCATE TABLE public.user_pvp_rank_ss_1;
CREATE SEQUENCE IF NOT EXISTS public.user_pvp_rank_ss_1_seq START WITH 1;
ALTER SEQUENCE public.user_pvp_rank_ss_1_seq RESTART WITH 1;

