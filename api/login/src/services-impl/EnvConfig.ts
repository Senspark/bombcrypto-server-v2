import 'dotenv/config'
import {bool, cleanEnv, num, port, str, url} from 'envalid';
import {IEnvConfig} from "../Services";

export default class EnvConfig implements IEnvConfig {
    isProduction: boolean;
    isGCP: boolean;
    port: number;
    logName: string | undefined;
    logRemoteHost: string | undefined;
    versionSol: string;
    versionWeb: string;

    postgresConnectionStringBackend: string;
    postgresConnectionStringBombcrypto: string;
    redisConnectionString: string;

    allowedDomains: string[];
    telegramBotTokens: string[];

    jwtLoginSecret: string;
    jwtBearerSecret: string;

    syncBannedList: boolean;

    isServerTonMaintenance: boolean;
    isServerWebMaintenance: boolean;
    isServerSolMaintenance: boolean

    mailSender: string;
    mailPassword: string;
    resetPasswordLink: string;
    resetTokenExpire: number;

    enableRequestLogging: boolean;

    aesSecret: string;
    gameSignPadding: string;
    dappSignPadding: string;

    obfuscateBytesAppend: number;
    rsaDelimiter: string;

    constructor() {
        const env = cleanEnv(process.env, {
            IS_GCLOUD: bool({default: false}),
            IS_PROD: bool({default: false}),
            PORT: port({default: 8105}),
            LOG_NAME: str({default: undefined}),
            LOG_REMOTE_HOST: str({default: undefined}),

            REDIS_CONNECTION_STRING: url(),
            POSTGRES_CONNECTION_STRING_BACKEND: url(),
            POSTGRES_CONNECTION_STRING_BOMBCRYPTO: url(),

            ALLOWED_DOMAINS: str(),
            TELEGRAM_BOT_TOKEN: str(),

            JWT_LOGIN_SECRET: str(),
            JWT_BEARER_SECRET: str(),
            SYNC_BANNED_LIST: bool({default: false}),
            VERSION_SOL: str({default:'0'}),
            VERSION_WEB: str({default:'0'}),

            TON_SERVER_MAINTENANCE: bool({default: false}),
            SOL_SERVER_MAINTENANCE: bool({default: false}),
            WEB_SERVER_MAINTENANCE: bool({default: false}),

            MAIL_SENDER: str(),
            MAIL_PASSWORD: str(),
            RESET_PASSWORD_LINK: str(),
            RESET_TOKEN_EXPIRE: num({default: 60 * 60}),// 1 hour

            ENABLE_REQUEST_LOGGING: bool({default: false}),

            AES_SECRET: str(),
            GAME_SIGN_PADDING: str(),
            DAPP_SIGN_PADDING: str(),

            OBFUSCATE_BYTES_APPEND: num(),
            RSA_DELIMITER: str(),
        });

        this.isProduction = env.IS_PROD;
        this.isGCP = env.IS_GCLOUD;
        this.port = env.PORT;
        this.logName = env.LOG_NAME;
        this.logRemoteHost = env.LOG_REMOTE_HOST;
        this.versionSol = env.VERSION_SOL;
        this.versionWeb = env.VERSION_WEB;

        this.postgresConnectionStringBackend = env.POSTGRES_CONNECTION_STRING_BACKEND;
        this.postgresConnectionStringBombcrypto = env.POSTGRES_CONNECTION_STRING_BOMBCRYPTO;
        this.redisConnectionString = env.REDIS_CONNECTION_STRING;

        this.allowedDomains = env.ALLOWED_DOMAINS ? env.ALLOWED_DOMAINS.split(',') : [];
        this.telegramBotTokens = env.TELEGRAM_BOT_TOKEN ? env.TELEGRAM_BOT_TOKEN.split(',') : [];

        this.jwtLoginSecret = env.JWT_LOGIN_SECRET;
        this.jwtBearerSecret = env.JWT_BEARER_SECRET;

        this.syncBannedList = env.SYNC_BANNED_LIST;

        this.isServerTonMaintenance = env.TON_SERVER_MAINTENANCE;
        this.isServerWebMaintenance = env.WEB_SERVER_MAINTENANCE;
        this.isServerSolMaintenance = env.SOL_SERVER_MAINTENANCE;

        this.mailSender = env.MAIL_SENDER;
        this.mailPassword = env.MAIL_PASSWORD;
        this.resetPasswordLink = env.RESET_PASSWORD_LINK;
        this.resetTokenExpire = env.RESET_TOKEN_EXPIRE;

        this.enableRequestLogging = env.ENABLE_REQUEST_LOGGING;

        this.aesSecret = env.AES_SECRET;
        this.gameSignPadding = env.GAME_SIGN_PADDING;
        this.dappSignPadding = env.DAPP_SIGN_PADDING;

        this.obfuscateBytesAppend = env.OBFUSCATE_BYTES_APPEND;
        this.rsaDelimiter = env.RSA_DELIMITER;
    }
}