import dotenv from 'dotenv';
import {bool, cleanEnv, num, str} from 'envalid';
import {loadNativeConfigs, NativeNetworkConfig} from './NativeConfig';

dotenv.config();

export interface IConfig {
  isProduction: boolean;
  serverPort: number;
  privateKey: string;
  blockchainCenterApi: string;
  redisConnectionString: string;
  networks: Map<string, NativeNetworkConfig>;
}

export class EnvConfig implements IConfig {
  private env = cleanEnv(process.env, {
    IS_PROD: bool({default: false}),
    PORT: num({default: 8080}),
    // Signer key — must hold SIGNER_ROLE on the DepositNative proxies.
    PRIVATE_KEY: str(),
    BLOCKCHAIN_CENTER_API: str(),
    // The game server reaches this service only over Redis streams — required, not optional.
    REDIS_CONNECTION_STRING: str(),
  });

  isProduction = this.env.IS_PROD;
  serverPort = this.env.PORT;
  privateKey = this.env.PRIVATE_KEY;
  blockchainCenterApi = this.env.BLOCKCHAIN_CENTER_API;
  redisConnectionString = this.env.REDIS_CONNECTION_STRING;

  // Contract addresses / chainId / confirmations are hardcoded by IS_PROD (see NativeConfig).
  networks: Map<string, NativeNetworkConfig> = loadNativeConfigs(this.isProduction);
}
