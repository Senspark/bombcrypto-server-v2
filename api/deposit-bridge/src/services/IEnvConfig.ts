export default interface IEnvConfig {
    isProduction: boolean;
    port: number;
    redisConnectionString: string;

    // Build stamp baked into the image at build time (git sha + build number/time). Surfaced on /health and
    // /monitor/version so the running image is identifiable at a glance — a stale value after a "rebuild" means
    // the container was not recreated. 'dev' when built without the deploy script.
    appVersion: string;

    // Key holding SIGNER_ROLE on every chain's DepositBridge. Signs EIP-191 off-chain — no gas, no provider.
    signerPrivateKey: string;

    // Base URL of the shared blockchain-center-api gateway (all RPC reads route through it).
    blockchainCenterApi: string;

    // deadline = now + signWindowSeconds — the operational window a signed withdraw stays relayable (§4.4).
    signWindowSeconds: number;

    // Short per-wallet TTL for the source `deposited` read cache (monotonic-safe; anti-spam against RPC).
    depositCacheTtlMs: number;

    // When false the process is a pure signer (no DB, no loops). Signing is unaffected either way.
    indexerEnabled: boolean;

    // DSN of the DEDICATED report database (own DB, not shared with the SmartFox game DB). Tables are
    // auto-created on first run. Required only when indexerEnabled.
    indexerDatabaseUrl: string;

    // Block to start the getLogs reconcile sweep from per chain (bridge deploy block). Seeded once; the
    // cursor then advances monotonically in the DB.
    indexerStartBlockBsc: number;
    indexerStartBlockPolygon: number;

    // IDLE cadence (ms) of the sweeper's safety-net drain and the checksum/cross-chain anomaly check. The
    // sweeper sleeps this long between drains when no activity is happening, and a notify wakes it early.
    indexerSweepIntervalMs: number;

    // ACTIVE cadence (ms): once a SmartFox notify (or withdraw) arrives, the sweeper re-drains this often so a
    // tx landing seconds after the signal is caught almost immediately instead of waiting a full idle cycle.
    indexerSweepActiveIntervalMs: number;

    // How long (ms) the sweeper stays in ACTIVE mode after the last notify. Must comfortably exceed
    // confirmed-depth latency + the user's MetaMask delay so the awaited deposit/withdraw tx is swept before
    // it relaxes back to idle. Each new notify extends the window.
    indexerSweepActiveWindowMs: number;

    // Cadence (ms) the withdraw-lifecycle watcher polls withdrawn[wallet] for pending signed authorizations.
    indexerLifecyclePollMs: number;

    // Shared secret guarding the read-only /monitor REST endpoints (X-Admin-Key). Empty disables the check.
    adminApiKey: string;

    // Redis TTL (ms) for the on-chain monitor snapshots (aggregate/health). On-chain reads are slow, so the
    // page serves the cached snapshot and only re-reads when the operator hits Refresh. This is just a safety
    // expiry so data can't go infinitely stale (default 1h; 0 = keep forever).
    monitorCacheTtlMs: number;
}
