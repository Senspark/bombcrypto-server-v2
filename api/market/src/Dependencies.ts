import {IEnvConfig, ILogger, IRedisDatabase} from "./Services";
import EnvConfig from "./services-impl/EnvConfig";
import BearerService from "./services-impl/BearerService";

import IDatabaseManager from "./services/IDatabaseManager";
import DatabaseManager from "./services-impl/DatabaseManager";
import RedisDatabase from "@project/services-impl/RedisDatabase";
import {AppStage} from "@project/services-impl/loggers/AppStage";
import {RemoteLogger} from "@project/services-impl/loggers/RemoteLogger";
import ConsoleLogger from "@project/services-impl/loggers/ConsoleLogger";

interface IDependencies {
    logger: ILogger;
    envConfig: IEnvConfig;
    bearerService: BearerService;
    database: IDatabaseManager;
    redis: IRedisDatabase;
}

let dependencies: IDependencies;

function initDependencies(): IDependencies {
    const envConfig = new EnvConfig();
    const isProduction = envConfig.isProduction;
    const isGcp = envConfig.isGCP;
    const appStage: AppStage = isProduction ? 'prod' : isGcp ? 'test' : 'local';



    const logger = new RemoteLogger({
            serviceName: 'ap-market',
            instanceId: envConfig.logName,
            stage: appStage,
            remoteHost: envConfig.logRemoteHost,
        },
        new ConsoleLogger('[D]')
    )

    const bearerService = new BearerService(logger, envConfig.jwtBearerSecret);
    const database = new DatabaseManager(envConfig.postgresConnectionString);
    const redis = new RedisDatabase(logger, envConfig.redisConnectionString);



    dependencies = {
        logger,
        envConfig,
        bearerService,
        database,
        redis,
    };

    return dependencies;
}

export {IDependencies, dependencies, initDependencies}