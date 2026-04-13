import {IDependencies} from "./Services";
import EnvConfig from "./services-impl/EnvConfig";
import Database from "./services-impl/Database";
import RedisDatabase from "./services-impl/RedisDatabase";
import IEnvConfig from "./services/IEnvConfig";
import IDatabase from "./services/IDatabase";
import ILogger from "./services/ILogger";
import IRedisDatabase from "./services/IRedisDatabase";
import {RemoteLogger} from "./services-impl/loggers/RemoteLogger";
import ConsoleLogger from "./services-impl/loggers/ConsoleLogger";
import {AppStage} from "./services-impl/loggers/AppStage";

export default class Dependencies implements IDependencies {
    redis: IRedisDatabase;
    database: IDatabase;
    envConfig: IEnvConfig;
    logger: ILogger;

    constructor(options?: IOptions) {
        this.envConfig = options?.envConfig ?? new EnvConfig();

        const isProduction = this.envConfig.isProduction;
        const isGcp = this.envConfig.isGCP;
        const appStage: AppStage = isProduction ? 'prod' : isGcp ? 'test' : 'local';

        this.logger = new RemoteLogger({
                serviceName: 'pvp-matching-web-server',
                instanceId: this.envConfig.logName,
                stage: appStage,
                remoteHost: this.envConfig.logRemoteHost,
            },
            new ConsoleLogger('[D]')
        )
        this.database = new Database(this.envConfig.postgresConnectionString);
        this.redis = new RedisDatabase(this.logger, this.envConfig.redisConnectionString);
    }

    isGcp(): boolean {
        return this.envConfig.isGCP;
    }

    isProduction(): boolean {
        return this.envConfig.isProduction;
    }
}

interface IOptions {
    envConfig?: IEnvConfig
}