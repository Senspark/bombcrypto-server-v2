export default interface IEnvConfig {
    port: number;
    isProduction: boolean;
    isMarketOpen: boolean;
    checkInterval: number;

    /**
     * Có phải đang ở môi trường trên Google Cloud ko?
     */
    isGCP: boolean;

    allowedDomains: string[];
    enableRequestLogging: boolean;
}