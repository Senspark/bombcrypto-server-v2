import express from "express";
import {createServer} from "http";
import bodyParser from "body-parser";
import cors from "cors";
import {AddressInfo} from "net";
import simpleHandlers from "./routers/SimpleHandlers";
import * as Dependencies from "./Dependencies";
import extendResponse from "./consts/ExpressExtension";
import helmet from "helmet";
import MarketHandler from "./routers/MarketHandler";
import {requestLogger} from "./utils/RequestLogger";
import {printRoutes} from "./utils/PrintRoutes";

const dependencies = Dependencies.initDependencies();
const logger = dependencies.logger;
const envConfig = dependencies.envConfig;

const app = express();
const loginRouter = express.Router();

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

app.use(cors(corsOpts));
app.use(bodyParser.json({limit: '100kb'}));
app.use(bodyParser.urlencoded({limit: '100kb', extended: true}));

if (envConfig.enableRequestLogging) {
    app.use(requestLogger);
}

// Extension method
app.use((req, res, next) => {
    extendResponse(res);
    next();
});

app.use(helmet());

const marketHandler = new MarketHandler(dependencies);

loginRouter.post(`/order`, marketHandler.order.bind(marketHandler));
loginRouter.post(`/cancel_order`, marketHandler.cancelOrder.bind(marketHandler));

loginRouter.post(`/buy`, marketHandler.buy.bind(marketHandler));
loginRouter.post(`/sell`, marketHandler.sell.bind(marketHandler));
loginRouter.post(`/edit`, marketHandler.edit.bind(marketHandler));
loginRouter.post(`/cancel`, marketHandler.cancel.bind(marketHandler));

loginRouter.get(`/get_config`, marketHandler.getConfig.bind(marketHandler));
loginRouter.post(`/get_my_item`, marketHandler.getMyItem.bind(marketHandler));

loginRouter.get(`/get_selling`, marketHandler.getSelling.bind(marketHandler));
loginRouter.get(`/get_ordering`, marketHandler.getOrdering.bind(marketHandler));
loginRouter.get(`/get_expensive`, marketHandler.getExpensive.bind(marketHandler));
loginRouter.get(`/get_fixed`, marketHandler.getFixed.bind(marketHandler));


app.use(`/`, loginRouter);
app.get(`/`, simpleHandlers.healthCheckHandler);
app.get(`/health`, simpleHandlers.healthCheckHandler);

const server = createServer(app).listen(
    envConfig.port,
    '0.0.0.0',
    () => {
        const address = server.address() as AddressInfo;
        logger.info(`Server started at http://${address.address}:${address.port}`);
        printRoutes(app, logger);
    }
);