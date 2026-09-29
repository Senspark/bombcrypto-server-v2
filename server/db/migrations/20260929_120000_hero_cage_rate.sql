-- Chance (0..1) that a match winner is offered 1 BHero Cage (BOMBERMAN in user_block_reward, on the
-- network the player picks). Read by HeroCageRewardManager; game_config reloads every 2 minutes, so a
-- change applies without a restart. A missing row means 0, i.e. the bonus is off.

INSERT INTO public.game_config (key, value, date_updated)
VALUES ('hero_cage_rate', '0.0025', now())
ON CONFLICT (key) DO NOTHING;
