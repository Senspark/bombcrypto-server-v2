// https://www.npmjs.com/package/envalid
import 'dotenv/config'
import {bool, cleanEnv, num, port, str} from 'envalid';
import {IEnvConfig} from "../Services";

export default class EnvConfig implements IEnvConfig {
    isProduction: boolean;
    isGCP: boolean;
    port: number;
    logName: string | undefined;
    logRemoteHost: string | undefined;
    redisConnectionString: string;
    postgresConnectionString: string;

    constructor() {
        const env = cleanEnv(process.env, {
            IS_GCLOUD: bool({default: false}),
            IS_PROD: bool({default: false}),
            PORT: port({default: 8105}),
            LOG_NAME: str({default: undefined}),
            LOG_REMOTE_HOST: str({default: undefined}),
            REDIS_CONNECTION_STRING: str(),
            POSTGRES_CONNECTION_STRING: str(),
        });

        this.isProduction = env.IS_PROD;
        this.isGCP = env.IS_GCLOUD;
        this.port = env.PORT;
        this.logName = env.LOG_NAME;
        this.logRemoteHost = env.LOG_REMOTE_HOST;
        this.redisConnectionString = env.REDIS_CONNECTION_STRING;
        this.postgresConnectionString = env.POSTGRES_CONNECTION_STRING;
    }
}
