import JwtService from "./services-impl/JwtService";
import IObfuscate from "./services-impl/encrypt/IObfuscate";
import {AppendBytesObfuscate} from "./services-impl/encrypt/AppendBytesObfuscate";
import IpWhitelist from "./services-impl/encrypt/IpWhitelist";
import BotSecurityService from "./services-impl/encrypt/BotSecurityService";
import {UserAccountCache} from "./services-impl/UserAccountCache";
import {IDatabaseManager, IEnvConfig, ILogger, IRedisDatabase} from "./Services";
import {ToadScheduler} from "toad-scheduler";
import DatabaseAccess from "./services-impl/DatabaseAccess";

export interface IGeneralServices {
    jwtService: JwtService;
    obfuscate: IObfuscate;
    ipWhitelist: IpWhitelist;
    botSecurityService: BotSecurityService;
    userAccountCache: UserAccountCache;
    databaseAccess: DatabaseAccess;
}

export class GeneralServices implements IGeneralServices {
    private readonly _jwtService: JwtService;
    private readonly _obfuscate: IObfuscate;
    private readonly _ipWhitelist: IpWhitelist;
    private readonly _botSecurityService: BotSecurityService;
    private readonly _userAccountCache: UserAccountCache;
    private readonly _databaseAccess: DatabaseAccess;

    constructor(
        private readonly _logger: ILogger,
        private readonly _envConfig: IEnvConfig,
        private readonly _redis: IRedisDatabase,
        private readonly _scheduler: ToadScheduler,
        private readonly _database: IDatabaseManager) {
        // Initialize JwtService
        this._jwtService = new JwtService(
            this._logger.clone('[JWT]'),
            this._envConfig.jwtLoginSecret
        );

        // Initialize IObfuscate
        this._obfuscate = new AppendBytesObfuscate(this._envConfig.obfuscateBytesAppend);

        // Initialize IpWhitelist
        this._ipWhitelist = new IpWhitelist(
            this._logger.clone('[IP-WHITELIST]'),
            this._redis
        );

        // Initialize BotSecurityService
        this._botSecurityService = new BotSecurityService(
            this._envConfig.isProduction,
            this._envConfig.isGCP,
            this._logger.clone('[BOT-SECURITY]'),
            this._ipWhitelist
        );

        // Initialize UserAccountCache
        this._userAccountCache = new UserAccountCache(
            this._logger.clone('[ACCOUNT-CACHE]'),
            this._scheduler
        );

        // Initialize DatabaseAccess
        this._databaseAccess = new DatabaseAccess(
            this._logger.clone('[DATABASE-ACCESS]'),
            this._database
        );
    }

    public get jwtService(): JwtService {
        return this._jwtService;
    }

    public get obfuscate(): IObfuscate {
        return this._obfuscate;
    }

    public get ipWhitelist(): IpWhitelist {
        return this._ipWhitelist;
    }

    public get botSecurityService(): BotSecurityService {
        return this._botSecurityService;
    }

    public get userAccountCache(): UserAccountCache {
        return this._userAccountCache;
    }

    public get databaseAccess(): DatabaseAccess {
        return this._databaseAccess;
    }
}

export default GeneralServices;
