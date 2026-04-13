import express, {Express, NextFunction, Request, Response, Router} from 'express';
import cors from "cors";
import {warmUp} from "./routes/SimpleHandlers";
import {errorHandlerMiddleware} from "./routes/Middlewares";
import IDependencies from "./services/IDependencies";
import extendResponse from "./consts/ExpressExtension";
import PvpQueueHandlers from "./routes/PvpQueueHandlers";
import PvpConfigHandlers from "./routes/PvpConfigHandlers";
import PvpMatchInfoHandlers from "./routes/PvpMatchInfoHandlers";

export class App {
    public readonly express: Express;
    public readonly router: Router;
    readonly _pvpQueueHandlers: PvpQueueHandlers;
    readonly _pvpConfigHandlers: PvpConfigHandlers;
    readonly _pvpMatchInfoHandlers: PvpMatchInfoHandlers;

    constructor(
        private readonly _dep: IDependencies
    ) {

        this._pvpConfigHandlers = new PvpConfigHandlers(_dep);
        this._pvpQueueHandlers = new PvpQueueHandlers(_dep, this._pvpConfigHandlers);
        this._pvpMatchInfoHandlers = new PvpMatchInfoHandlers(_dep);

        this.express = express();
        this.router = express.Router();
        this.setup();
    }

    private setup() {
        this.express.get(`/_ah/warmup`, warmUp);
        this.express.get(`/`, warmUp);

        this.router.use((req: Request, res: Response, next: NextFunction) => {
            extendResponse(this._dep.logger, res);
            next();
        });
        this.router.use((err: any, req: Request, res: Response, next: NextFunction) => errorHandlerMiddleware(this._dep.logger, err, res));
        this.express.use(cors());
        this.express.use(express.json());

        // Pvp handlers.
        this.router.post(`/join-queue`, this._pvpQueueHandlers.joinQueue.bind(this._pvpQueueHandlers));
        this.router.post(`/leave-queue`, this._pvpQueueHandlers.leaveQueue.bind(this._pvpQueueHandlers));
        this.router.get(`/report`, this._pvpQueueHandlers.report.bind(this._pvpQueueHandlers));
        this.router.get(`/config`, this._pvpConfigHandlers.getConfig.bind(this._pvpConfigHandlers));

        // Tournaments
        this.router.get(`/tournament/status`, this._pvpMatchInfoHandlers.getTournamentMatchesInfo.bind(this._pvpMatchInfoHandlers));
        this.router.post(`/tournament/my-matches`, this._pvpMatchInfoHandlers.getMyMatch.bind(this._pvpMatchInfoHandlers));
        this.router.get(`/tournament/room/status`, this._pvpMatchInfoHandlers.getTournamentRoomStatus.bind(this._pvpMatchInfoHandlers));

        this.express.use(`/`, this.router);
        this.express.use(`/pvp`, this.router); // legacy
        this.express.use(`/pvp-matching`, this.router);
        this.express.use(`/pvp-matching`, express.static('public'));
        this.express.use(`/pvp-matching-2`, this.router);
        this.express.use(`/pvp-matching-2`, express.static('public'));
    }
}