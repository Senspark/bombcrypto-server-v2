import express from "express";
import Routes from "./Routes";
import Dependencies from "./Dependencies";
import PvpFixtureHandlers from "./routers/PvpFixtureHandlers";
import PvpTestHandler from "./routers/PvpTestHandler";

const dependencies = new Dependencies();
export const logger = dependencies.logger;
export const envConfig = dependencies.envConfig;
const pvpFixtureHandlers = new PvpFixtureHandlers(dependencies);
const pvpTestHandler = new PvpTestHandler(dependencies);

try {
    const app = express();
    Routes.setupStandardModules(app, logger, envConfig);

    const tournamentRouter = express.Router();
    const matchTestRouter = express.Router();

    app.use(`/pvp/tournament`, tournamentRouter);
    app.use(`/pvp/test`, matchTestRouter);

    Routes.setupBasicRoutes(app);
    Routes.setupPvpFixtureRoutes(tournamentRouter, pvpFixtureHandlers);
    Routes.setupPvpTestRoutes(matchTestRouter, pvpTestHandler);

    (async () => {
        await dependencies.redis.testConnection();
        await dependencies.database.testConnection();
    })();

    app.listen(envConfig.port, () => {
        logger.info(`Server started at http://localhost:${envConfig.port}`);
    });
} catch (e) {
    logger.error(`Error starting server:`);
    logger.error(e);
}