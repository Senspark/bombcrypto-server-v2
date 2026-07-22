import dotenv from 'dotenv';
import {bool, cleanEnv, num, str} from "envalid";
import {
    CHAIN_NAMES,
    IBlockchainConfig,
    loadBlockchainConfigs,
} from './BlockchainConfig';

dotenv.config();

const ZERO_ADDRESS = `0x0000000000000000000000000000000000000000`;

interface IConfig {
    isGcp: boolean;
    isProduction: boolean;
    useSubscriberOnly: boolean;
    serverPort: number;
    defaultNetwork: string;
    blockchainConfigs: Map<string, IBlockchainConfig>;
    redisConnectionString: string;
    stakeCacheSeconds: number;
    postgresConnectionString: string;
    schedulerIntervalSeconds: number[];
    /**
     * Để test cho nhanh
     */
    jumpToLatestBlock: boolean;
    blockchainCenterApi: string;
    logName?: string;
    logRemoteHost?: string;
    enableRequestLogging: boolean;
}

class EnvConfig implements IConfig {
    env = cleanEnv(process.env, {
        IS_GCLOUD: bool(),
        IS_PROD: bool(),
        USE_SUBSCRIBER_ONLY: bool({default: false}),
        PORT: num({default: 8080}),
        DEFAULT_NETWORK: str({choices: [CHAIN_NAMES.BNB, CHAIN_NAMES.POLYGON]}),
        REDIS_HOST: str({default: `localhost`}),
        REDIS_PORT: num({default: 6379}),
        REDIS_PASSWORD: str({default: ``}),
        REDIS_CONNECTION_STRING: str(),
        STAKE_CACHE_SECONDS: num({default: 60 * 15}), // 15 minutes
        POSTGRES_CONNECTION_STRING: str(),
        SCHEDULER_INTERVAL_SEC: str({default: `[5,30]`}),
        JUMP_TO_LATEST_BLOCK: bool({default: false}),
        BLOCKCHAIN_CENTER_API: str(),
        LOG_NAME: str({default: undefined}),
        LOG_REMOTE_HOST: str({default: undefined}),
        ENABLE_REQUEST_LOGGING: bool({default: false}),
    });

    isGcp = this.env.IS_GCLOUD;
    isProduction = this.env.IS_PROD;
    useSubscriberOnly = this.env.USE_SUBSCRIBER_ONLY;
    serverPort = this.env.PORT;
    defaultNetwork = this.env.DEFAULT_NETWORK;
    redisConnectionString = this.env.REDIS_CONNECTION_STRING;
    stakeCacheSeconds = this.env.STAKE_CACHE_SECONDS;
    postgresConnectionString = this.env.POSTGRES_CONNECTION_STRING;
    schedulerIntervalSeconds = this.env.SCHEDULER_INTERVAL_SEC.replace('[', '').replace(']', '').split(',').map(Number);
    jumpToLatestBlock = this.env.JUMP_TO_LATEST_BLOCK;
    blockchainCenterApi = this.env.BLOCKCHAIN_CENTER_API;
    logName: string | undefined = this.env.LOG_NAME;
    logRemoteHost: string | undefined = this.env.LOG_REMOTE_HOST;
    enableRequestLogging = this.env.ENABLE_REQUEST_LOGGING;

    blockchainConfigs: Map<string, IBlockchainConfig> = loadBlockchainConfigs(this.isProduction);
}

export {
    ZERO_ADDRESS,
    IConfig,
    EnvConfig
};
