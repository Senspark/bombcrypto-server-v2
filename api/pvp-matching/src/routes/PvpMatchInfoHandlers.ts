import IDependencies from "../services/IDependencies";
import ILogger from "../services/ILogger";
import {StreamKeys} from "../cache/CachedKeys";
import {
    IPvpRoomInfo,
    IPvpTournamentMatchInfo,
    ITournamentMatchListData,
    PvpTournamentMatchStatus,
    TournamentMatchListStatus
} from "../consts/MatchData";
import TournamentMatchManager from "../processors/TournamentMatchManager";
import {Request, Response} from "express";

export default class PvpMatchInfoHandlers {
    readonly _logger: ILogger;
    _roomsInfo: IPvpRoomInfo[] = [];
    readonly _tournamentMatchManager: TournamentMatchManager;

    constructor(private readonly _deps: IDependencies) {
        this._logger = _deps.logger.clone('[MATCH_INFO]');
        this._tournamentMatchManager = new TournamentMatchManager(_deps);
        _deps.messenger.listen(StreamKeys.SV_PVP_MATCH_UPDATED_STR, this.onMatchInfoUpdated.bind(this));
    }

    async getTournamentMatchesInfo(req: Request, res: Response) {
        res.send({
            code: 0,
            message: {
                matches: await this._getTournamentMatchesInfo(),
            },
        });
    }

    async getMyMatch(req: Request, res: Response) {
        const userName = req.body.username;
        if (!userName) {
            res.send({
                code: 100,
                message: 'Missing username'
            });
            return;
        }
        res.send({
            code: 0,
            message: {
                my_match: await this._getMyMatchId(userName),
            },
        });
    }

    async getTournamentRoomStatus(req: Request, res: Response) {
        res.send({
            code: 0,
            message: {
                details: [{
                    zone: "sg",
                    room_count: this._roomsInfo.length,
                    rooms: this._roomsInfo
                }],
                room_count: this._roomsInfo.length
            },
        });
    }

    /**
     * Only retrieves information about fixed matches (fixtures):
     * - Upcoming matches
     * - Matches in waiting period
     * - Matches currently being played
     * - Matches that have finished
     * - Matches that are overdue but have not yet taken place
     */
    private async _getTournamentMatchesInfo(): Promise<ITournamentMatchListData[]> {
        const matches: ITournamentMatchListData[] = [];
        const now = Date.now();
        const playingMatchIds = new Set(this._roomsInfo.map(e => e.match_info.id));
        const databaseRecords = await this._tournamentMatchManager.getTournamentMatches();

        const mWaiting = databaseRecords.filter(e => e.status === PvpTournamentMatchStatus.Pending && e.find_begin_timestamp > now);
        const mFinding = databaseRecords.filter(e => !playingMatchIds.has(e.id) && (e.status === PvpTournamentMatchStatus.Pending && e.find_begin_timestamp <= now && now < e.find_end_timestamp));
        const mPlaying = databaseRecords.filter(e => playingMatchIds.has(e.id) && e.status === PvpTournamentMatchStatus.Pending);
        const mFinished = databaseRecords.filter(e => !playingMatchIds.has(e.id) && e.status === PvpTournamentMatchStatus.Completed);
        const mAbandoned = databaseRecords.filter(e => !playingMatchIds.has(e.id) && (e.status === PvpTournamentMatchStatus.Pending && e.find_end_timestamp <= now));

        matches.push(...this.otherMatchDataMapper(mWaiting, TournamentMatchListStatus.Waiting));
        matches.push(...this.otherMatchDataMapper(mFinding, TournamentMatchListStatus.Finding));
        matches.push(...this.playingMatchDataMapper(mPlaying));
        matches.push(...this.otherMatchDataMapper(mFinished, TournamentMatchListStatus.Finished));
        matches.push(...this.otherMatchDataMapper(mAbandoned, TournamentMatchListStatus.Finished));

        return matches;
    }

    private async _getMyMatchId(username: string): Promise<string[]> {
        return await this._tournamentMatchManager.getMyMatchFromDatabase(username);

    }

    private onMatchInfoUpdated(data: any) {
        try {
            this._roomsInfo = [];
            const roomInfo: IPvpRoomInfo[] = data.rooms;
            if (roomInfo) {
                this._roomsInfo = roomInfo;
            }
        } catch (e) {
            this._logger.error(e)
        }
    }

    private otherMatchDataMapper(tournamentRecords: IPvpTournamentMatchInfo[], status: TournamentMatchListStatus): ITournamentMatchListData[] {
        return tournamentRecords.map(r => {
            return {
                id: r.id,
                status: status,
                mode: r.mode,
                observer_count: 0,
                find_begin_timestamp: r.find_begin_timestamp,
                find_end_timestamp: r.find_end_timestamp,
                start_timestamp: 0,
                finish_timestamp: r.finish_timestamp,
                info: r.info
            } as ITournamentMatchListData;
        });
    }

    private playingMatchDataMapper(records: IPvpTournamentMatchInfo[]): ITournamentMatchListData[] {
        return this._roomsInfo.map(r => {
            return {
                id: r.match_info.id,
                status: TournamentMatchListStatus.Playing,
                mode: r.match_info.mode,
                observer_count: r.match_data.observer_count,
                find_begin_timestamp: 0,
                find_end_timestamp: 0,
                start_timestamp: r.match_data.start_timestamp,
                finish_timestamp: 0,
                info: r.match_info.info.map((user, slot) => {
                    const teamId = r.match_info.team.findIndex(team => team.slots.includes(slot));
                    return {
                        score: r.match_data.results.filter(item => !item.is_draw && item.winning_team == teamId).length,
                        user_id: user.user_id,
                        username: user.username,
                        display_name: user.display_name,
                        rank: user.rank,
                    };
                })
            };
        });
    }

}
