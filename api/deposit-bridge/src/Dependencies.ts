import EnvConfig from "./services-impl/EnvConfig";
import ConsoleLogger from "./services-impl/loggers/ConsoleLogger";
import MessengerService from "./services-impl/MessengerService";
import IEnvConfig from "./services/IEnvConfig";
import ILogger from "./services/ILogger";
import IMessengerService from "./services/IMessengerService";

export default class Dependencies {
    envConfig: IEnvConfig;
    logger: ILogger;
    messenger: IMessengerService;

    constructor() {
        this.envConfig = new EnvConfig();
        this.logger = new ConsoleLogger('[D]');
        this.messenger = new MessengerService(this.logger, this.envConfig);
    }
}
