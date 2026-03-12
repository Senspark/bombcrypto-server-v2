export default interface IEnvConfig {
    port: number;
    logName: string | undefined;
    logRemoteHost: string | undefined;
    isProduction: boolean;

    /**
     * Có phải đang ở môi trường trên Google Cloud ko?
     */
    isGCP: boolean;

    /**
     * Latest Web client version
     */
    versionSol: string;
    versionWeb: string;

    redisConnectionString: string;
    postgresConnectionStringBackend: string;
    postgresConnectionStringBombcrypto: string;

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
}