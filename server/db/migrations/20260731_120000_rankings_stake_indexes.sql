-- =============================================================================
-- Stake rankings / explorer indexes (market site "Rankings" feature)
-- =============================================================================
--
-- What it does:
--   Indexes to keep the market-api stake rankings and the wallet/hero/house
--   explorer fast as user_bomber grows. All the ranking queries filter FI
--   heroes still in the wallet (type = 0 AND "hasDelete" = 0) on one network
--   (data_type), so the indexes are partial on exactly that predicate.
--
-- Queries served (market-v2/backend, ranking.repository.ts):
--   - GET /rankings/stake/heroes : ORDER BY stake_amount|stake_sen DESC,
--     optionally filtered by rarity
--   - GET /rankings/stake/wallets: GROUP BY uid over the same predicate
--   - GET /explorer/wallet/:addr : heroes/houses of one uid
--   - GET /explorer/house/:id    : lookup by (house_id, type) - already the PK
--
-- Idempotent: uses IF NOT EXISTS.
-- =============================================================================

-- Hero ranking by BCOIN stake (covers the per-rarity variant via rare)
CREATE INDEX IF NOT EXISTS user_bomber_stake_bcoin_rank_idx
    ON public.user_bomber (data_type, stake_amount DESC, bomber_id)
    INCLUDE (rare)
    WHERE type = 0 AND "hasDelete" = 0 AND stake_amount > 0;

-- Hero ranking by SEN stake
CREATE INDEX IF NOT EXISTS user_bomber_stake_sen_rank_idx
    ON public.user_bomber (data_type, stake_sen DESC, bomber_id)
    INCLUDE (rare)
    WHERE type = 0 AND "hasDelete" = 0 AND stake_sen > 0;

-- Per-rarity hero ranking (rarity is an equality filter before the sort)
CREATE INDEX IF NOT EXISTS user_bomber_stake_bcoin_rarity_idx
    ON public.user_bomber (data_type, rare, stake_amount DESC)
    WHERE type = 0 AND "hasDelete" = 0 AND stake_amount > 0;

CREATE INDEX IF NOT EXISTS user_bomber_stake_sen_rarity_idx
    ON public.user_bomber (data_type, rare, stake_sen DESC)
    WHERE type = 0 AND "hasDelete" = 0 AND stake_sen > 0;

-- Wallet ranking / wallet profile: GROUP BY uid over STAKED heroes only.
-- The queries aggregate rows with stake > 0 (a wallet's stake sum over all
-- rows equals the sum over its staked rows), so the index stays tiny no
-- matter how big user_bomber gets; per-wallet totals for the visible page
-- come from the existing (data_type, uid) index.
CREATE INDEX IF NOT EXISTS user_bomber_stake_by_uid_idx
    ON public.user_bomber (data_type, uid)
    INCLUDE (stake_amount, stake_sen)
    WHERE type = 0 AND "hasDelete" = 0 AND (stake_amount > 0 OR stake_sen > 0);

-- Houses of a wallet (user_house only has the (house_id, type) PK)
CREATE INDEX IF NOT EXISTS user_house_type_uid_idx
    ON public.user_house (type, uid);
