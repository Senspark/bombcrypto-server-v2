import 'dotenv/config'
import {bool, cleanEnv, port, str, url} from 'envalid';
import {IEnvConfig} from "../Services";
import * as process from "node:process";

export default class EnvConfig implements IEnvConfig {
    constructor() {
        const env = cleanEnv(process.env, {
            IS_GCLOUD: bool({default: false}),
            IS_PROD: bool({default: false}),
            PORT: port({default: 9120}),
            JWT_BEARER_SECRET: str(),
            ALLOWED_DOMAINS: str(),

            IS_MARKET_OPEN: bool({default: true}),
            CHECK_INTERVAL: port({default: 1000}), // 1 second
            POSTGRES_CONNECTION_STRING: url(),
            REDIS_CONNECTION_STRING: url(),

            ENABLE_REQUEST_LOGGING: bool({default: false}),
            LOG_NAME: str({default: undefined}),
            LOG_REMOTE_HOST: str({default: undefined}),
        });

        this.isProduction = env.IS_PROD;
        this.isGCP = env.IS_GCLOUD;
        this.port = env.PORT;
        this.jwtBearerSecret = env.JWT_BEARER_SECRET

        this.isMarketOpen = env.IS_MARKET_OPEN;
        this.checkInterval = env.CHECK_INTERVAL * 1000;

        this.allowedDomains = parseDomains(env.ALLOWED_DOMAINS);
        this.postgresConnectionString = env.POSTGRES_CONNECTION_STRING;
        this.redisConnectionString = env.REDIS_CONNECTION_STRING;

        this.enableRequestLogging = env.ENABLE_REQUEST_LOGGING;
        this.logName = env.LOG_NAME;
        this.logRemoteHost = env.LOG_REMOTE_HOST;

    }

    isProduction: boolean;
    isGCP: boolean;
    port: number;

    allowedDomains: string[];

    isMarketOpen: boolean;
    checkInterval: number;

    jwtBearerSecret: string;
    postgresConnectionString: string;
    redisConnectionString: string;

    enableRequestLogging: boolean;
    logName: string | undefined;
    logRemoteHost: string | undefined;
}

function parseDomains(domains: string | null | undefined): string[] {
    if (!domains || domains.length === 0) {
        return [];
    }
    return domains.split(',').map(d => {
        return d.endsWith('/') ? d.substring(0, d.length - 1) : d;
    });
}