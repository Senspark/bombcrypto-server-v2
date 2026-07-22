import ILogger from "./ILogger";
import {IConfig} from "../Config";
import IRedisDatabase from "../cache/IRedisDatabase";
import IDatabaseManager from "../database/IDatabaseManager";
import {CachedNftDetails} from "../cache/CachedNftDetails";
import {CachedNftOwner} from "../cache/CachedNftOwner";
import IMessengerService from "../cache/IMessengerService";

export default interface IDependencies {
  logger: ILogger;
  envConfig: IConfig;
  redis: IRedisDatabase;
  messenger: IMessengerService;
  database: IDatabaseManager;
  cachedNftDetails: CachedNftDetails;
  cachedNftOwner: CachedNftOwner;
}