import {Express, NextFunction, Request, Response, Router} from "express";
import simpleHandlers from "./routers/SimpleHandlers";
import rateLimit from "express-rate-limit";
import bodyParser from "body-parser";
import cors from "cors";
import extendResponse from "./consts/ExpressExtension";
import IEnvConfig from "./services/IEnvConfig";
import helmet from "helmet";
import ILogger from "./services/ILogger";
import PvpFixtureHandlers from "./routers/PvpFixtureHandlers";
import PvpTestHandler from "./routers/PvpTestHandler";

const LOCAL_ENV = "localEnv"

function setupStandardModules(app: Express, logger: ILogger, envConfig: IEnvConfig) {
    if (envConfig.isGCP) {
        app.use(rateLimit({
            windowMs: 1000, // 1 seconds
            limit: 10, // limit each IP to 10 requests per seconds
            handler: simpleHandlers.tooManyRequest,
            legacyHeaders: false,
        }));
        app.use(helmet());
        app.use(cors({
            credentials: true,
        }));
        app.set('trust proxy', 1);
    } else {
        app.use(cors({
            credentials: true
        }));
    }

    app.use(bodyParser.json({limit: '10kb'})); // Limit request bodies
    app.use(bodyParser.urlencoded({limit: '1kb', extended: true})); // Limit URL-encoded bodies

    // Extension method
    const responseLogger = logger.clone('[RESPONSE]');
    app.use((req: Request, res: Response, next: NextFunction) => {
        req[LOCAL_ENV] = !envConfig.isGCP;
        extendResponse(responseLogger, res);
        next();
    });
}

function setupBasicRoutes(app: Express) {
    app.get(`/`, simpleHandlers.healthCheckHandler);
    app.get(`/health`, simpleHandlers.healthCheckHandler);
}

/**
 * For Tournament
 * @param router
 * @param handlers
 */
function setupPvpFixtureRoutes(router: Router, handlers: PvpFixtureHandlers) {
    router.get(`/registered`, handlers.getRegisteredMatches.bind(handlers));
    router.post(`/register-group`, handlers.registerMatchGroup.bind(handlers));
    router.post(`/un-register`, handlers.unregisterMatch.bind(handlers));
}

/**
 * For testing single match
 * @param router
 * @param handlers
 */
function setupPvpTestRoutes(router: Router, handlers: PvpTestHandler) {
    router.get(`/registered`, handlers.getRegisteredMatches.bind(handlers));
    router.post(`/register`, handlers.registerMatch.bind(handlers));
    router.post(`/un-register`, handlers.unregisterMatch.bind(handlers));
}


const Routes = {
    setupBasicRoutes,
    setupStandardModules,
    setupPvpFixtureRoutes,
    setupPvpTestRoutes,
};

export default Routes;
