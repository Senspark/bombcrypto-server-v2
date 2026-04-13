import ILogger from "./services/ILogger";
import IEnvConfig from "./services/IEnvConfig";
import IDatabase from "./services/IDatabase";
import IRedisDatabase from "./services/IRedisDatabase";

export {ILogger, IEnvConfig, IDatabase, IRedisDatabase};

export interface IDependencies {
    logger: ILogger;
    envConfig: IEnvConfig;
    database: IDatabase;
    redis: IRedisDatabase;

    isProduction(): boolean;

    isGcp(): boolean;
}
