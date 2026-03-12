import {IDatabaseManager, IEnvConfig, ILogger, IRedisDatabase} from "./Services";

import ConsoleLogger from "./services-impl/loggers/ConsoleLogger";
import EnvConfig from "./services-impl/EnvConfig";
import DatabaseManager from "./services-impl/DatabaseManager";
import RedisDatabase from "./services-impl/redis/RedisDatabase";
import JwtBearerService from "./services-impl/JwtBearerService";
import {ToadScheduler} from "toad-scheduler";
import {AppStage} from "./services-impl/loggers/AppStage";
import {RemoteLogger} from "./services-impl/loggers/RemoteLogger";
import GeneralServices, {IGeneralServices} from './GeneralServices';

interface IDependencies {
    logger: ILogger;
    envConfig: IEnvConfig;
    databaseBackend: IDatabaseManager;
    databaseBombcrypto: IDatabaseManager;
    redis: IRedisDatabase;
    bearerService: JwtBearerService;
    scheduler: ToadScheduler;
    generalServices: IGeneralServices;
}

let dependencies: IDependencies;

function initDependencies(): IDependencies {
    const envConfig = new EnvConfig();
    const isProduction = envConfig.isProduction;
    const isGcp = envConfig.isGCP;
    const appStage: AppStage = isProduction ? 'prod' : isGcp ? 'test' : 'local';

    const logger = new RemoteLogger({
            serviceName: 'ap-login',
            instanceId: envConfig.logName,
            stage: appStage,
            remoteHost: envConfig.logRemoteHost,
        },
        new ConsoleLogger('[D]')
    )

    const databaseBackend = new DatabaseManager(envConfig.postgresConnectionStringBackend);
    const databaseBombcrypto = new DatabaseManager(envConfig.postgresConnectionStringBombcrypto);
    const redis = new RedisDatabase(logger, envConfig.redisConnectionString);
    const bearerService = new JwtBearerService(logger, envConfig.jwtBearerSecret);
    const scheduler = new ToadScheduler();
    const generalServices = new GeneralServices(logger, envConfig, redis, scheduler, databaseBackend)

    dependencies = {
        logger,
        envConfig,
        databaseBackend,
        databaseBombcrypto,
        redis,
        bearerService,
        scheduler,
        generalServices
    }

    return dependencies;
}

export {IDependencies, dependencies, initDependencies}