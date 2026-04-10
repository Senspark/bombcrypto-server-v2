import IDependencies from "../services/IDependencies";
import ILogger from "../services/ILogger";
import {sleep} from "./Utils";
import {IMatch, IPvpReport, IPvpReportMatch, IPvpReportUser, IUser} from "../consts/PvpData";
import IMatchFinder from "./IMatchFinder";
import IMessenger from "./IMessenger";
import MatchFinder from "./match-finders/MatchFinder";
import {IPvpQueue} from "./IPvpQueue";
import UsersHistory from "./UsersHistory";
import UserMeetManager from "./UserMeetManager";
import NetworkAddress from "./NetworkAddress";
import PvpConfigHandlers from "../routes/PvpConfigHandlers";

const RegisterMatchTimeOut = 3000; // 3s.
const PlayingMatchTimeOut = 3 * 60 * 1000; // 3 minutes.

export default class PvpQueue implements IPvpQueue {
    readonly _logger: ILogger;
    readonly _timeOut: number;
    /**
     * Users who have entered the match
     * @private
     */
    readonly _usersInGame = new Set<string>();
    /**
     * Users who have not been matched yet
     * @private
     */
    readonly _usersInQueue = new Map<string, IUser>();
    readonly _userHistories = new UsersHistory();
    readonly _matchFinder: IMatchFinder;
    /**
     * Ongoing matches
     */
    _playingMatches: IMatch[] = [];

    constructor(
        private readonly _deps: IDependencies,
        private readonly _messenger: IMessenger,
        private readonly _userMeetManager: UserMeetManager,
        private readonly _pvpConfigHandlers: PvpConfigHandlers,
    ) {
        this._logger = _deps.logger.clone('[PVP_QUEUE]');
        this._timeOut = _deps.envConfig.queueTimeOut;
        const networkAddress = new NetworkAddress(this._pvpConfigHandlers.getServerConfig())
        this._matchFinder = new MatchFinder(_deps, this._userMeetManager, networkAddress);
        this.loop().then();
    }

    addUser(user: IUser) {
        if (this._usersInGame.has(user.id)) {
            throw Error(`${user.id} is processing`);
        }

        user.refreshTimestamp = Date.now();

        if (!this._usersInQueue.has(user.id)) {
            this._usersInQueue.set(user.id, user);
        } else {
            this._usersInQueue[user.id].refreshTimestamp = user.refreshTimestamp;
        }
    }

    removeUser(userId: string) {
        this._logger.info(`[removeUser] ${userId}`);
        this._usersInQueue.delete(userId);
        this._usersInGame.delete(userId);
    }

    removeMatch(matchId: string) {
        this._logger.info(`[removeMatch] ${matchId}`);
        this._playingMatches = this._playingMatches.filter(it => it.id !== matchId);
    }

    report(): IPvpReport {
        const users = Array.from(this._usersInQueue.values()).map(it => this.getUserReport(it));
        const matches = this._playingMatches.map(it => this.getMatchReport(it));
        return {
            queuing_users: users,
            matches: matches,
        };
    }

    // === Private ===

    private async loop() {
        const foreverLoop = true;
        while (foreverLoop) {
            await sleep(200);
            try {
                this.removeExpiredUsers();
                this.removeExpiredMatches();
                await this.findMatches();
            } catch (ex: any) {
                this._logger.errors(ex.message, ex.stack?.split(`\n`));
            }
        }
    }

    private removeExpiredUsers() {
        const now = Date.now();
        for (const user of this._usersInQueue.values()) {
            if (now - user.refreshTimestamp >= this._timeOut) {
                this._logger.info(`[removeExpiredUsers] ${user.id}`);
                this.removeUser(user.id);
            }
        }
    }

    private removeExpiredMatches() {
        const now = Date.now();
        this._playingMatches = this._playingMatches.filter(it => now - it.timestamp < PlayingMatchTimeOut);
    }

    private async findMatches() {
        const users = Array.from(this._usersInQueue.values());
        const findResult = await this._matchFinder.find(users);
        const pendingUsers = findResult.pendingUsers;

        // Extend the waiting time for pendingUsers
        if (pendingUsers.length > 0) {
            users.forEach(usr => {
                if (pendingUsers.includes(usr.id)) {
                    usr.refreshTimestamp = Date.now();
                }
            });
        }

        const matches = findResult.matchesFound;
        if (matches.length === 0) {
            // this._logger.info(`[findMatches] no match found`);
            return;
        }

        this._logger.info(`[processUsers] ${matches.length} matches & ${users.length} users`);
        await Promise.all(matches.map(async match => {
            try {
                await this.registerMatch(match);
            } catch (ex: any) {
                this._logger.errors(ex.message, ex.stack?.split(`\n`));
            }
        }));
    }

    private async registerMatch(match: IMatch) {
        const userIds = [...new Set(match.users.map(it => it.id))];

        try {
            userIds.forEach(it => this._usersInQueue.delete(it));
            this._playingMatches.push(match);
            this._messenger.registerMatch(match);

            setTimeout(() => {
                userIds.forEach(it => {
                    this.removeUser(it);
                });
            }, RegisterMatchTimeOut);

        } catch (e: any) {
            this._logger.errors(e.message, e.stack?.split(`\n`));
        }
    }

    private getUserReport(user: IUser): IPvpReportUser {
        return {
            user_id: user.id,
            is_bot: user.data.isBot,
            zone: user.networkStats.lowestPingZone,
            rank: user.rank,
            point: user.point,
            time_stamp: user.timestamp,
            refresh_time_stamp: user.refreshTimestamp,
        };
    }

    private getMatchReport(match: IMatch): IPvpReportMatch {
        return {
            match_id: match.id,
            zone: match.zone,
            mode: match.mode,
            times_stamp: match.timestamp,
            players: match.users.map(it => this.getUserReport(it)),
            desc: match.desc,
        }
    }
}