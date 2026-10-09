-- Chance (0..1) that a match winner is offered 1 BHero Cage (BOMBERMAN in user_block_reward, on the
-- network the session logged in with). Read by HeroCageRewardManager; game_config reloads every 2 minutes,
-- so a change applies without a restart. 0 (or a missing row) means the bonus is off.
-- Seeded off: wins are farmable by bots until there is an anti-bot answer. Enable with e.g. '0.0025'.

INSERT INTO public.game_config (key, value, date_updated)
VALUES ('hero_cage_rate', '0', now())
ON CONFLICT (key) DO NOTHING;
