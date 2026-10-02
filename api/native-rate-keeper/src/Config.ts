import dotenv from 'dotenv';
import {bool, cleanEnv, num, str} from 'envalid';
import {KeeperNetworkConfig, loadKeeperConfigs} from './KeeperConfig';

dotenv.config();

export interface IConfig {
  isProduction: boolean;
  serverPort: number;
  privateKey: string;
  redisConnectionString: string;
  minChangePercent: number;
  maxChangePercent: number;
  maxMessageAgeMinutes: number;
  dryRun: boolean;
  networks: Map<string, KeeperNetworkConfig>;
}

export class EnvConfig implements IConfig {
  private env = cleanEnv(process.env, {
    IS_PROD: bool({default: false}),
    PORT: num({default: 8080}),
    // Keeper key — must hold DESIGNER_ROLE on both HeroDesign proxies (that is the role setNativeRate
    // checks today) and carry BNB / POL for gas.
    PRIVATE_KEY: str(),
    REDIS_CONNECTION_STRING: str(),
    RPC_BSC: str(),
    RPC_POLYGON: str(),
    // Below this move the on-chain rate is left alone: every write is a tx, and every change can revert
    // a player's purchase that was signed at the old price (the contract wants msg.value == price).
    MIN_CHANGE_PERCENT: num({default: 2}),
    // Step at most this much per write (clamped, never refused). The game server already clamps each
    // tick to 20%, so a bigger gap means the chain fell behind or a bad message, not the market.
    MAX_CHANGE_PERCENT: num({default: 25}),
    // A message this old is a backlog, not the current rate.
    MAX_MESSAGE_AGE_MINUTES: num({default: 30}),
    // Log what would be written without sending any tx.
    DRY_RUN: bool({default: false}),
  });

  isProduction = this.env.IS_PROD;
  serverPort = this.env.PORT;
  privateKey = this.env.PRIVATE_KEY;
  redisConnectionString = this.env.REDIS_CONNECTION_STRING;
  minChangePercent = this.env.MIN_CHANGE_PERCENT;
  maxChangePercent = this.env.MAX_CHANGE_PERCENT;
  maxMessageAgeMinutes = this.env.MAX_MESSAGE_AGE_MINUTES;
  dryRun = this.env.DRY_RUN;

  networks: Map<string, KeeperNetworkConfig> = loadKeeperConfigs(this.isProduction, {
    BSC: this.env.RPC_BSC,
    POLYGON: this.env.RPC_POLYGON,
  });
}
