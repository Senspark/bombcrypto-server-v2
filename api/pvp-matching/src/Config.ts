import dotenv from 'dotenv';
import {bool, cleanEnv, num, str} from "envalid";

dotenv.config();

export class EnvConfig implements IConfig {
    env = cleanEnv(process.env, {
        IS_GCLOUD: bool(),
        RUNTIME_ENV: str({choices: [RunTimeEnv.Production, RunTimeEnv.Test, RunTimeEnv.Local, RunTimeEnv.Tournament]}),
        PORT: num({default: 8080}),
        REDIS_CONNECTION_STRING: str(),
        POSTGRES_CONNECTION_STRING: str(),
        JWT_SECRET: str({default: ''}),
        QUEUE_TIME_OUT: num({default: 30000}), // 30 seconds
        CURRENT_PVP_SEASON: num({default: 4}),
        LOG_NAME: str({default: undefined}),
        LOG_REMOTE_HOST: str({default: undefined}),
    });

    isGcp = this.env.IS_GCLOUD;
    runTimeEnv = this.env.RUNTIME_ENV;
    isProduction =
        this.env.RUNTIME_ENV === RunTimeEnv.Production
        || this.env.RUNTIME_ENV === RunTimeEnv.Tournament;
    serverPort = this.env.PORT;
    redisConnectionString = this.env.REDIS_CONNECTION_STRING;
    postgresConnectionString = this.env.POSTGRES_CONNECTION_STRING;
    jwtSecret = this.env.JWT_SECRET;
    queueTimeOut = this.env.QUEUE_TIME_OUT;
    pvpConfig: IPvpConfig = {
        maxTimeForFindingUser: 20_000,
        maxTotalMatchForFindingBot: 3,
        maxNumberTwoPlayerCanMeet: 2,
    };
    currentPvpSeason = this.env.CURRENT_PVP_SEASON;

    logName: string | undefined = this.env.LOG_NAME;
    logRemoteHost: string | undefined = this.env.LOG_REMOTE_HOST;
}

export interface IPvpConfig {
    /**
     * After this time, a Bot will be searched for
     */
    maxTimeForFindingUser: number;

    /**
     * If the total number of matches is less than this, always play with a Bot
     */
    maxTotalMatchForFindingBot: number;

    /**
     * Maximum number of times two users can meet each other in one day
     */
    maxNumberTwoPlayerCanMeet: number;
}

export interface IConfig {
    isGcp: boolean;
    isProduction: boolean;
    runTimeEnv: RunTimeEnv;
    serverPort: number;
    redisConnectionString: string;
    postgresConnectionString: string;
    jwtSecret: string;
    queueTimeOut: number;
    pvpConfig: IPvpConfig;
    currentPvpSeason: number;

    logName: string | undefined;
    logRemoteHost: string | undefined;
}

enum RunTimeEnv {
    Production = `prod`,
    Test = `test`,
    Local = `local`,
    Tournament = `tournament`
}