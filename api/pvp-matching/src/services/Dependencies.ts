import ILogger from "./ILogger";
import {EnvConfig, IConfig} from "../Config";
import IDatabaseManager from "../database/IDatabaseManager";
import IRedisDatabase from "../cache/IRedisDatabase";
import RedisDatabase from "../cache/RedisDatabase";
import DatabaseManager from "../database/DatabaseManager";
import IDependencies from "./IDependencies";
import MessengerService from "../cache/MessengerService";
import IMessengerService from "../cache/IMessengerService";
import {RemoteLogger} from "./loggers/RemoteLogger";
import {AppStage} from "./loggers/AppStage";
import ConsoleLogger from "./loggers/ConsoleLogger";

export default class Dependencies implements IDependencies {
    database: IDatabaseManager;
    envConfig: IConfig;
    logger: ILogger;
    redis: IRedisDatabase;
    messenger: IMessengerService;

    constructor() {
        this.envConfig = new EnvConfig();

        const isProduction = this.envConfig.isProduction;
        const isGcp = this.envConfig.isGcp;
        const appStage: AppStage = isProduction ? 'prod' : isGcp ? 'test' : 'local';

        this.logger = new RemoteLogger({
                serviceName: 'ap-pvp-matching',
                instanceId: this.envConfig.logName,
                stage: appStage,
                remoteHost: this.envConfig.logRemoteHost,
            },
            new ConsoleLogger('[D]')
        )

        this.redis = new RedisDatabase(this.logger, this.envConfig);
        this.messenger = new MessengerService(this.logger, this.envConfig);
        this.database = new DatabaseManager(this.envConfig);
    }
}