-- ap-deposit-bridge — bridge report database.
-- DEDICATED database, NOT the SmartFox game DB. Apply this by hand before starting the service (the code
-- never provisions it). All wei live in numeric(78,0) (uint256 max ~ 1.15e77 < 1e78); addresses lowercased.

-- Per (chain, token, wallet) cumulative counters, from Deposit/Withdraw event totals or targeted reads.
CREATE TABLE IF NOT EXISTS bridge_wallet_state (
    chain         varchar(20)   NOT NULL,           -- BSC | POLYGON
    token         varchar(64)   NOT NULL,           -- token contract address (lowercased)
    symbol        varchar(20)   NOT NULL,           -- BCOIN | SEN (denormalized for the cross-chain join)
    wallet        varchar(64)   NOT NULL,           -- user EOA (lowercased)
    deposited     numeric(78,0) NOT NULL DEFAULT 0, -- cumulative deposited[wallet][token] (wei)
    withdrawn     numeric(78,0) NOT NULL DEFAULT 0, -- cumulative withdrawn[wallet][token] (wei)
    updated_block bigint        NOT NULL DEFAULT 0,
    updated_at    timestamptz   NOT NULL DEFAULT now(),
    PRIMARY KEY (chain, token, wallet)
);
CREATE INDEX IF NOT EXISTS idx_bws_symbol_wallet ON bridge_wallet_state (symbol, wallet);

-- Withdraw lifecycle: signed -> executed | expired. Bounds the watch window so no record hangs.
CREATE TABLE IF NOT EXISTS bridge_withdraw_lifecycle (
    id               bigserial     PRIMARY KEY,
    chain            varchar(20)   NOT NULL,         -- withdraw chain
    token            varchar(64)   NOT NULL,
    symbol           varchar(20)   NOT NULL,
    wallet           varchar(64)   NOT NULL,
    other_deposited  numeric(78,0) NOT NULL,         -- value the signer authorized
    deadline         bigint        NOT NULL,         -- unix seconds
    status           varchar(20)   NOT NULL DEFAULT 'signed', -- signed | executed | expired
    tx_block         bigint,
    signed_at        timestamptz   NOT NULL DEFAULT now(),
    resolved_at      timestamptz
);
CREATE INDEX IF NOT EXISTS idx_bwl_status ON bridge_withdraw_lifecycle (status);
CREATE INDEX IF NOT EXISTS idx_bwl_wallet ON bridge_withdraw_lifecycle (chain, token, wallet);

-- Tail cursor, one row per chain. On startup the sweep jumps this to the current head (never races history)
-- and tails forward at a fixed cadence; a failed read is retried in place, so the cursor just stalls until
-- reads recover.
CREATE TABLE IF NOT EXISTS bridge_indexer_cursor (
    chain        varchar(20) PRIMARY KEY,
    block_number bigint      NOT NULL DEFAULT 0
);

-- Block ranges the tail never covered — the history before first start (from the deploy block) plus any
-- downtime windows. An operator backfills these by hand from a block-explorer export; the frontend lists the
-- open ranges and links to the explorer filtered by contract.
CREATE TABLE IF NOT EXISTS bridge_indexer_gap (
    id         bigserial   PRIMARY KEY,
    chain      varchar(20) NOT NULL,
    from_block bigint      NOT NULL,
    to_block   bigint      NOT NULL,
    status     varchar(20) NOT NULL DEFAULT 'open',  -- open | filled | discarded
    created_at timestamptz NOT NULL DEFAULT now(),
    filled_at  timestamptz,
    UNIQUE (chain, from_block, to_block)
);
CREATE INDEX IF NOT EXISTS idx_bgap_open ON bridge_indexer_gap (chain, status);

-- Itemized per-user event ledger (§18): one row per Deposit/Withdraw event, so a wallet's full topup/withdraw
-- timeline across both chains is reconstructable (the counter table above only keeps running totals). Idempotent
-- by (chain, tx_hash, log_index). Also shipped as db/migrations/002_event_ledger.sql for existing databases.
CREATE TABLE IF NOT EXISTS bridge_event_ledger (
    chain      varchar(20)   NOT NULL,           -- BSC | POLYGON
    token      varchar(64)   NOT NULL,           -- token contract address (lowercased)
    symbol     varchar(20)   NOT NULL,           -- BCOIN | SEN
    wallet     varchar(64)   NOT NULL,           -- user EOA (lowercased); the event's indexed `user`
    kind       varchar(10)   NOT NULL,           -- deposit | withdraw
    amount     numeric(78,0) NOT NULL,           -- gross amount moved by this event (wei)
    net        numeric(78,0),                    -- withdraw payout after fee (wei); NULL for deposit
    cumulative numeric(78,0) NOT NULL,           -- deposited/withdrawn total for (wallet,token) AFTER this event
    block      bigint        NOT NULL,
    tx_hash    varchar(66)   NOT NULL,
    log_index  int           NOT NULL,
    ts         bigint,                            -- block unix seconds (timeline sort key); NULL if unresolved
    created_at timestamptz   NOT NULL DEFAULT now(),
    PRIMARY KEY (chain, tx_hash, log_index)
);
CREATE INDEX IF NOT EXISTS idx_bel_wallet_ts ON bridge_event_ledger (wallet, ts);
CREATE INDEX IF NOT EXISTS idx_bel_symbol_wallet ON bridge_event_ledger (symbol, wallet);
-- Global transaction feed (/monitor/transactions): newest-first paging over the whole ledger.
CREATE INDEX IF NOT EXISTS idx_bel_ts ON bridge_event_ledger (ts DESC);

-- Anomaly alert log (alert-only — no on-chain auto-pause; an operator pulls the switch via MetaMask).
CREATE TABLE IF NOT EXISTS bridge_anomaly (
    id         bigserial   PRIMARY KEY,
    kind       varchar(40) NOT NULL,   -- CROSS_CHAIN_INSOLVENT | PER_USER_OVER_WITHDRAW | CHECKSUM_MISMATCH
    chain      varchar(20),
    token      varchar(64),
    wallet     varchar(64),
    detail      jsonb       NOT NULL DEFAULT '{}'::jsonb,
    status      varchar(20) NOT NULL DEFAULT 'open',  -- open | resolved (re-checked live, condition gone)
    created_at  timestamptz NOT NULL DEFAULT now(),
    resolved_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_banomaly_created ON bridge_anomaly (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_banomaly_open ON bridge_anomaly (status);
