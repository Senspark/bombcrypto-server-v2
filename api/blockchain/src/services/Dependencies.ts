import ILogger from "./ILogger";
import {EnvConfig, IConfig} from "../Config";
import IDatabaseManager from "../database/IDatabaseManager";
import IRedisDatabase from "../cache/IRedisDatabase";
import {CachedNftDetails} from "../cache/CachedNftDetails";
import {CachedNftOwner} from "../cache/CachedNftOwner";
import ConsoleLogger from "./ConsoleLogger";
import {RemoteLogger} from "./loggers/RemoteLogger";
import {AppStage} from "./loggers/AppStage";
import RedisDatabase from "../cache/RedisDatabase";
import DatabaseManager from "../database/DatabaseManager";
import IDependencies from "./IDependencies";
import IMessengerService from "../cache/IMessengerService";
import MessengerService from "../cache/MessengerService";

export default class Dependencies implements IDependencies {
  constructor(options?: IDependenciesData) {
    this.envConfig = options?.envConfig ?? new EnvConfig();

    const appStage: AppStage = this.envConfig.isProduction ? 'prod' : this.envConfig.isGcp ? 'test' : 'local';
    this.logger = new RemoteLogger({
      serviceName: 'ap-blockchain',
      instanceId: this.envConfig.logName,
      stage: appStage,
      remoteHost: this.envConfig.logRemoteHost,
    }, new ConsoleLogger('[DEFAULT]'));
    this.redis = new RedisDatabase(this.logger, this.envConfig.redisConnectionString);
    this.messenger = new MessengerService(this.logger, this.envConfig);
    this.database = new DatabaseManager(this.envConfig);
    this.cachedNftDetails = new CachedNftDetails(this.envConfig, this.redis);
    this.cachedNftOwner = new CachedNftOwner(this.envConfig, this.redis);
  }

  cachedNftDetails: CachedNftDetails;
  cachedNftOwner: CachedNftOwner;
  database: IDatabaseManager;
  envConfig: IConfig;
  logger: ILogger;
  redis: IRedisDatabase;
  messenger: IMessengerService;
}

interface IDependenciesData {
  envConfig?: EnvConfig;
}