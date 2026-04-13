import ILogger from "./ILogger";
import {IConfig} from "../Config";
import IRedisDatabase from "../cache/IRedisDatabase";
import IDatabaseManager from "../database/IDatabaseManager";
import IMessengerService from "../cache/IMessengerService";

export default interface IDependencies {
    logger: ILogger;
    envConfig: IConfig;
    redis: IRedisDatabase;
    messenger: IMessengerService;
    database: IDatabaseManager;
}