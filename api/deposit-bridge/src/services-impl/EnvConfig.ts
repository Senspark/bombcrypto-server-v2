import 'dotenv/config'
import {bool, cleanEnv, num, port, str} from 'envalid';
import IEnvConfig from "../services/IEnvConfig";

export default class EnvConfig implements IEnvConfig {
    isProduction: boolean;
    port: number;
    redisConnectionString: string;
    appVersion: string;
    signerPrivateKey: string;
    blockchainCenterApi: string;
    signWindowSeconds: number;
    depositCacheTtlMs: number;
    indexerEnabled: boolean;
    indexerDatabaseUrl: string;
    indexerStartBlockBsc: number;
    indexerStartBlockPolygon: number;
    indexerSweepIntervalMs: number;
    indexerSweepActiveIntervalMs: number;
    indexerSweepActiveWindowMs: number;
    indexerLifecyclePollMs: number;
    adminApiKey: string;
    monitorCacheTtlMs: number;

    constructor() {
        const env = cleanEnv(process.env, {
            IS_PROD: bool({default: false}),
            PORT: port({default: 8108}),
            REDIS_CONNECTION_STRING: str({default: ''}),
            APP_VERSION: str({default: 'dev'}),
            SIGNER_PRIVATE_KEY: str({default: ''}),
            BLOCKCHAIN_CENTER_API: str({default: ''}),
            SIGN_WINDOW_SECONDS: num({default: 900}),
            DEPOSIT_CACHE_TTL_MS: num({default: 5_000}),
            INDEXER_ENABLED: bool({default: false}),
            INDEXER_DATABASE_URL: str({default: ''}),
            INDEXER_START_BLOCK_BSC: num({default: 0}),
            INDEXER_START_BLOCK_POLYGON: num({default: 0}),
            INDEXER_SWEEP_INTERVAL_MS: num({default: 300_000}),
            INDEXER_SWEEP_ACTIVE_INTERVAL_MS: num({default: 3_000}),
            INDEXER_SWEEP_ACTIVE_WINDOW_MS: num({default: 60_000}),
            INDEXER_LIFECYCLE_POLL_MS: num({default: 15_000}),
            ADMIN_API_KEY: str({default: ''}),
            MONITOR_CACHE_TTL_MS: num({default: 3_600_000}),
        });

        this.isProduction = env.IS_PROD;
        this.port = env.PORT;
        this.redisConnectionString = env.REDIS_CONNECTION_STRING;
        this.appVersion = env.APP_VERSION;
        this.signerPrivateKey = env.SIGNER_PRIVATE_KEY;
        this.blockchainCenterApi = env.BLOCKCHAIN_CENTER_API;
        this.signWindowSeconds = env.SIGN_WINDOW_SECONDS;
        this.depositCacheTtlMs = env.DEPOSIT_CACHE_TTL_MS;
        this.indexerEnabled = env.INDEXER_ENABLED;
        this.indexerDatabaseUrl = env.INDEXER_DATABASE_URL;
        this.indexerStartBlockBsc = env.INDEXER_START_BLOCK_BSC;
        this.indexerStartBlockPolygon = env.INDEXER_START_BLOCK_POLYGON;
        this.indexerSweepIntervalMs = env.INDEXER_SWEEP_INTERVAL_MS;
        this.indexerSweepActiveIntervalMs = env.INDEXER_SWEEP_ACTIVE_INTERVAL_MS;
        this.indexerSweepActiveWindowMs = env.INDEXER_SWEEP_ACTIVE_WINDOW_MS;
        this.indexerLifecyclePollMs = env.INDEXER_LIFECYCLE_POLL_MS;
        this.adminApiKey = env.ADMIN_API_KEY;
        this.monitorCacheTtlMs = env.MONITOR_CACHE_TTL_MS;
    }
}
