import IDependencies from "../services/IDependencies";
import ILogger from "../services/ILogger";
import {Request, Response} from "express";
import {IJoinQueueRequestBody, ILeaveQueueRequestBody, IPvpResultInfo, IUser} from "../consts/PvpData";
import PvpQueue from "../processors/PvpQueue";
import {Messenger} from "../processors/IMessenger";
import NetworkStats from "../processors/NetworkStats";
import {StreamKeys} from "../cache/CachedKeys";
import BetterJson from "../processors/BetterJson";
import UserMeetManager from "../processors/UserMeetManager";
import PvpConfigHandlers from "./PvpConfigHandlers";
import {parseAvatar} from "../processors/ReturnUtils";

export default class PvpQueueHandlers {
    readonly _logger: ILogger;
    readonly _queue: PvpQueue;
    readonly _userMatchManager: UserMeetManager;

    constructor(private readonly _deps: IDependencies, readonly _pvpConfigHandlers: PvpConfigHandlers) {
        this._logger = _deps.logger.clone('[PVP_HANDLERS]');
        this._userMatchManager = new UserMeetManager(_deps);
        this._queue = new PvpQueue(_deps, new Messenger(_deps), this._userMatchManager, _pvpConfigHandlers);
        this._pvpConfigHandlers = _pvpConfigHandlers;
        _deps.messenger.listen(StreamKeys.SV_GAME_JOIN_PVP_STR, this.joinQueueByStream.bind(this));
        _deps.messenger.listen(StreamKeys.SV_GAME_LEAVE_PVP_STR, this.leaveQueueByStream.bind(this));
        _deps.messenger.listen(StreamKeys.SV_PVP_MATCH_FINISHED_STR, this.endMatchByStream.bind(this));
    }

    public async joinQueue(req: Request, res: Response) {
        try {
            const body: IJoinQueueRequestBody = req.body;
            body.pings = BetterJson.objectToMap(body.pings);
            //body.data.available_boosters = new Map(Object.entries(body.data.available_boosters));
            this._logger.assert(body, 'Invalid request body');
            this._queue.addUser(this.createUser(body));
            res.sendSuccess(null);
        } catch (e) {
            this._logger.error(e);
            res.sendError(e);
        }
    }

    public async leaveQueue(req: Request, res: Response) {
        try {
            const body: IJoinQueueRequestBody = req.body;
            this._logger.assert(body, 'Invalid request body');
            this._queue.removeUser(body.userName);
            res.sendSuccess(null);
        } catch (e) {
            this._logger.error(e);
            res.sendError(e);
        }
    }

    public async report(req: Request, res: Response) {
        try {
            const data = this._queue.report();
            res.sendSuccess(data);
        } catch (e) {
            this._logger.error(e);
            res.sendError(e);
        }
    }

    private async joinQueueByStream(data: any) {
        try {
            this._logger.info("-----------On Join Queue-----------")
            const body: IJoinQueueRequestBody = data;
            body.pings = new Map(Object.entries(body.pings));
            //body.data.available_boosters = new Map(Object.entries(body.data.available_boosters));
            this._logger.assert(body, 'Invalid request body');
            this._queue.addUser(this.createUser(body));
        } catch (e) {
            this._logger.error(e);
        }
    }

    private async leaveQueueByStream(data: any) {
        try {
            this._logger.info("-----------On Leave Queue-----------")
            const body: ILeaveQueueRequestBody = data;
            this._logger.assert(body, 'Invalid request body');
            this._queue.removeUser(body.userName);
        } catch (e) {
            this._logger.error(e);

        }
    }

    private async endMatchByStream(data: any) {
        try {
            this._logger.info("-----------On End Match-----------")
            const body: IPvpResultInfo = data;
            this._logger.assert(body, 'Invalid request body');
            this._queue.removeMatch(body.id);

        } catch (e) {
            this._logger.error(e);
        }
    }

    private createUser(body: IJoinQueueRequestBody) {
        const now = Date.now();
        const user: IUser = {
            id: body.userName,
            serverId: body.data.serverId,
            networkStats: new NetworkStats(body.pings),
            matchId: body.data.matchId,
            mode: body.data.mode,
            totalMatchCount: body.data.totalMatchCount,
            rank: body.data.rank,
            point: body.data.point,
            timestamp: now,
            refreshTimestamp: now,
            newServer: body.newServer,
            data: {
                isBot: body.data.isBot,
                displayName: body.data.displayName,
                boosters: body.data.boosters,
                availableBoosters: body.data.availableBoosters,
                avatar: parseAvatar(body.data.avatar),
                hero: body.data.hero,
                wagerMode: body.wagerMode,
                wagerTier: body.wagerTier,
                wagerToken: body.wagerToken,
                gameMode: body.gameMode
            }
        };
        return user;
    }
}

