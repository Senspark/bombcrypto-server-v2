-- Market-tracking native rate.
--
-- config_native_rate.native_per_bcoin is the single conversion every native price goes through: the
-- rock pack, auto mine and the IAP gem shop are all priced in BCOIN and charged in the chain's coin at
-- this rate. It was hand-maintained, which means it is a market rate frozen on the day someone typed
-- it -- as BCOIN or POL/BNB move, every one of those prices drifts away from its BCOIN list price
-- together.
--
-- From here the BNB/POL extension's price scheduler rewrites it every few minutes from the USD quotes
-- it already fetches for the gem swap (rate = bcoin_usd / native_usd, per network). Nothing about how
-- prices are computed changes -- fn_native_price and every caller stay exactly as they were -- only
-- who maintains the number.
--
-- No schema change is needed for that: the writer updates the existing column and stamps modify_date.
-- This migration only seeds the two knobs, and both have the same defaults compiled into the server,
-- so it is convenience rather than a requirement.
--
-- Guards live in the writer, not here: it never stores a non-positive rate, and it moves the stored
-- value by at most a set percentage per tick, so one bad quote from the price feed cannot land whole.
-- If the feed stays down the rate simply stops moving -- the last real market rate keeps being charged,
-- which is what modify_date is for.

BEGIN;

INSERT INTO public.game_config (key, value, date_updated)
VALUES
    -- How often the scheduler rewrites the rate. Read when the scheduler starts, so a change needs a
    -- server restart. 15 minutes tracks the market closely enough that no one can shop on a stale
    -- price, while leaving the number still long enough that it does not move mid-purchase.
    ('native_rate_update_minutes', '15', now()),
    -- Ceiling on how far one tick may move the stored rate, in percent. An ordinary day never reaches
    -- it; a broken quote is cut down to it and undone by the next good one.
    ('native_rate_max_change_percent', '20', now())
ON CONFLICT (key) DO NOTHING;

COMMIT;
