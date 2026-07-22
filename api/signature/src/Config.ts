import dotenv from 'dotenv';
import {bool, cleanEnv, num, str} from 'envalid';
import {CHAIN_NAMES, IBlockchainConfig, loadBlockchainConfigs} from './BlockchainConfig';

dotenv.config();

interface IConfig {
    isGcp: boolean;
    isProduction: boolean;
    serverPort: number;
    defaultNetwork: string;
    privateKey: string;
    jwtSecret: string;
    jwtPayloadKey: string;
    blockchainCenterApi: string;
    blockchainConfigs: Map<string, IBlockchainConfig>;
    logName?: string;
    logRemoteHost?: string;
    enableRequestLogging: boolean;
    redisConnectionString: string;
}

class EnvConfig implements IConfig {
    env = cleanEnv(process.env, {
        IS_GCLOUD: bool({default: false}),
        IS_PROD: bool(),
        PORT: num({default: 8080}),
        DEFAULT_NETWORK: str({choices: [CHAIN_NAMES.BNB, CHAIN_NAMES.POLYGON]}),
        PRIVATE_KEY: str(),
        JWT_SECRET: str(),
        JWT_PAYLOAD_KEY: str(),
        BLOCKCHAIN_CENTER_API: str(),
        LOG_NAME: str({default: undefined}),
        LOG_REMOTE_HOST: str({default: undefined}),
        ENABLE_REQUEST_LOGGING: bool({default: false}),
        REDIS_CONNECTION_STRING: str(),
    });

    isGcp = this.env.IS_GCLOUD;
    isProduction = this.env.IS_PROD;
    serverPort = this.env.PORT;
    defaultNetwork = this.env.DEFAULT_NETWORK;
    privateKey = this.env.PRIVATE_KEY;
    jwtSecret = this.env.JWT_SECRET;
    jwtPayloadKey = this.env.JWT_PAYLOAD_KEY;
    blockchainCenterApi = this.env.BLOCKCHAIN_CENTER_API;
    logName: string | undefined = this.env.LOG_NAME;
    logRemoteHost: string | undefined = this.env.LOG_REMOTE_HOST;
    enableRequestLogging = this.env.ENABLE_REQUEST_LOGGING;
    redisConnectionString = this.env.REDIS_CONNECTION_STRING;

    blockchainConfigs: Map<string, IBlockchainConfig> = loadBlockchainConfigs(this.isProduction);
}

export {
    IConfig,
    EnvConfig,
};
