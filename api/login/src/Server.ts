import express from "express";
import * as http from "http";
import bodyParser from "body-parser";
import cors from "cors";
import {AddressInfo} from "net";
import simpleHandlers from "./Handlers/SimpleHandlers";
import * as DependenciesInjector from "./DependenciesInjector";
import extendResponse from "./consts/ExpressExtension";
import cookieParser from "cookie-parser";
import helmet from "helmet";
import {monitorPendingRequests, recordRequest} from "./utils/PendingRequestTracker";
import {requestLogger} from "./utils/RequestLogger";
import {printRoutes} from "./utils/PrintRoutes";
import { createTonRouter } from "./routers/TonRouter";
import { createSolRouter } from "./routers/SolRouter";
import { createWebRouter } from "./routers/WebRouter";
import { createMobileRouter } from "./routers/MobileRouter";
import { createDappRouter } from "./routers/DappRouter";

const dependencies = DependenciesInjector.initDependencies();
const logger = dependencies.logger;
const envConfig = dependencies.envConfig;

const app = express();
const loginRouter = express.Router();

// app.use(rateLimit({
//     windowMs: 5 * 60 * 1000, // 5 minutes
//     limit: 100, // limit each IP to 100 requests per windowMs
//     validate: {
//         xForwardedForHeader: false
//     }
// }));
let corsOpts = {};
if (envConfig.allowedDomains.length > 0) {
    corsOpts = {
        origin: [...envConfig.allowedDomains],
        credentials: true
    };
} else {
    corsOpts = {
        origin: true,
        credentials: true
    };
}

loginRouter.use(cors(corsOpts));

app.use(bodyParser.json({limit: '5kb'})); // Limit request bodies
app.use(bodyParser.urlencoded({limit: '5kb', extended: true})); // Limit URL-encoded bodies
app.use(cookieParser());

if (envConfig.enableRequestLogging) {
    app.use(requestLogger);
}

app.use(recordRequest);

// Extension method
app.use((req, res, next) => {
    extendResponse(res);
    next();
});

app.use(helmet());

// Mount modular routers
loginRouter.use('/ton', createTonRouter(dependencies));
loginRouter.use('/sol', createSolRouter(dependencies));
loginRouter.use('/web', createWebRouter(dependencies));
loginRouter.use('/mobile', createMobileRouter(dependencies));
loginRouter.use('/dapp', createDappRouter(dependencies));

app.use(`/`, loginRouter);
app.get(`/`, simpleHandlers.healthCheckHandler);
app.get(`/health`, simpleHandlers.healthCheckHandler);

const server = http.createServer(app).listen(
    envConfig.port,
    '0.0.0.0',
    () => {
        const address = server.address() as AddressInfo;
        logger.info(`Server started at http://${address.address}:${address.port}`);
        monitorPendingRequests(logger);
        printRoutes(app, logger);
    }
);