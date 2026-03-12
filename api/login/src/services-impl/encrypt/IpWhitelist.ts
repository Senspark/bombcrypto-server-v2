import {ILogger, IRedisDatabase} from "../../Services";

export default class IpWhitelist {
    constructor(
        logger: ILogger,
        private readonly _redis: IRedisDatabase,
    ) {
        this._logger = logger;
    }

    private readonly _logger: ILogger;

    isInWhitelist(ip: string | undefined | null): boolean {
        return true;
    }
}