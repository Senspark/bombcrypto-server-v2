export default interface IEnvConfig {
    port: number;
    isProduction: boolean;
    isGCP: boolean;

    logName: string | undefined;
    logRemoteHost: string | undefined;
    redisConnectionString: string;

    postgresConnectionString: string;
}
