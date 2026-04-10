import {CachedKeys} from "../cache/CachedKeys";
import {IPvpTournamentMatchInfo, PvpTournamentMatchStatus} from "../consts/MatchData";
import IDependencies from "../services/IDependencies";
import ILogger from "../services/ILogger";
import * as console from "node:console";

const CACHE_SECONDS = 60 * 1; // 1 minutes
const MY_MATCH_SECONDS = 60 * 15; // 15 minutes
export default class TournamentMatchManager {
    _logger: ILogger;
    _currentPvpSeason: number = 0;

    constructor(private readonly _dep: IDependencies) {
        this._logger = _dep.logger.clone('[TOURNAMENT]');
        this._currentPvpSeason = _dep.envConfig.currentPvpSeason;
    }

    public async getTournamentMatches(): Promise<IPvpTournamentMatchInfo[]> {
        return await this.getTournamentMatchesFromCached();
    }

    public async getMyMatchFromDatabase(userName: string): Promise<string[]> {
        try {
            // Get match data from Redis hash using userName as field
            const matchesMap = await this._dep.redis.readHash(CachedKeys.AP_PVP_MY_MATCH);
            if (matchesMap && matchesMap.has(userName)) {
                const matchData = matchesMap.get(userName);
                if (matchData && matchData !== '[]') {
                    return JSON.parse(matchData);
                }
            }

            // If we reach here, data wasn't found or was empty in Redis, get from database
            return await this.fetchAndCacheMyMatches(userName);
        } catch (e) {
            this._logger.error(`Error getting match data for user ${userName}: ${e}`);
            return []; // Return empty array on error
        }
    }

    private async getTournamentMatchesFromCached(): Promise<IPvpTournamentMatchInfo[]> {
        try {
            const cached = await this._dep.redis.get(CachedKeys.AP_PVP_TOURNAMENT_MATCHES);
            this._logger.assert(cached, `Fixture matches not found in cache`);
            this._logger.assert(cached !== '{}', 'Fixture matches not found in cache');
            return JSON.parse(cached!!);
        } catch (e) {
            this._logger
            const arr = await this.getTournamentMatchesFromDatabase();
            const cacheData = JSON.stringify(arr);
            await this._dep.redis.setWithTTL(CachedKeys.AP_PVP_TOURNAMENT_MATCHES, cacheData, CACHE_SECONDS);
            return arr;
        }
    }

    private async fetchAndCacheMyMatches(userName: string): Promise<string[]> {
        try {
            // Query to get userName2
            const userName2Result = await this._dep.database.query(
                `SELECT user_name FROM public.user WHERE second_username = $1`, [userName]
            );
            const userName2 = userName2Result.rows.length > 0 ? userName2Result.rows[0].user_name : "";

            // Query to get match IDs
            const matchIdsResult = await this._dep.database.query(
                `SELECT id FROM public.pvp_tournament WHERE participant_1 = $1 OR participant_2 = $1 OR participant_1 = $2 OR participant_2 = $2`,
                [userName, userName2]
            );
            const matchIds = matchIdsResult.rows.map(row => row.id);

            // Save the match IDs in Redis hash with userName as field
            const cacheData = JSON.stringify(matchIds);
            const fieldsValues = new Map<string, string>();
            fieldsValues.set(userName, cacheData);
            await this._dep.redis.addToHash(CachedKeys.AP_PVP_MY_MATCH, fieldsValues);

            return matchIds;
        } catch (e) {
            this._logger.error(`Database error getting matches for user ${userName}: ${e}`);
            return []; // Return empty array on database error
        }
    }

    private async getTournamentMatchesFromDatabase(): Promise<IPvpTournamentMatchInfo[]> {
        try {
            const sql =
                `WITH _cpt AS (SELECT id,
                                      mode,
                                      participant_1,
                                      participant_2,
                                      status,
                                      EXTRACT(EPOCH FROM find_begin_time) AS find_begin_timestamp,
                                      EXTRACT(EPOCH FROM find_end_time)   AS find_end_timestamp,
                                      EXTRACT(EPOCH FROM finish_time)     AS finish_timestamp,
                                      user_1_score,
                                      user_2_score
                               FROM pvp_tournament)
                 SELECT cpt.*,
                        u1.id_user                                          AS user_id_1,
                        u2.id_user                                          AS user_id_2,
                        u1.user_name                                        AS username_1,
                        u2.user_name                                        AS username_2,
                        COALESCE(u1.name, u1.second_username, u1.user_name) AS display_name_1,
                        COALESCE(u2.name, u2.second_username, u2.user_name) AS display_name_2,
                        COALESCE(uprs1.point, 0)                            AS rank_point_1,
                        COALESCE(uprs2.point, 0)                            AS rank_point_2
                 FROM _cpt AS cpt
                          INNER JOIN "user" AS u1
                                     ON (cpt.participant_1 = u1.user_name OR cpt.participant_1 = u1.second_username)
                          INNER JOIN "user" AS u2
                                     ON (cpt.participant_2 = u2.user_name OR cpt.participant_2 = u2.second_username)
                          LEFT JOIN user_pvp_rank_ss_${this._currentPvpSeason} AS uprs1 ON u1.id_user = uprs1.uid
                          LEFT JOIN user_pvp_rank_ss_${this._currentPvpSeason} AS uprs2 ON u2.id_user = uprs2.uid;`;
            const result = await this._dep.database.query(sql, []);
            console.log("Database: " + JSON.stringify(result));
            return result.rows.map(r => {
                return {
                    id: r.id,
                    status: this.databaseStatusToEnum(r.status),
                    find_begin_timestamp: parseInt(r.find_begin_timestamp) * 1000,
                    find_end_timestamp: parseInt(r.find_end_timestamp) * 1000,
                    finish_timestamp: parseInt(r.finish_timestamp ?? 0) * 1000,
                    mode: r.mode,
                    info: [
                        {
                            score: r.user_1_score,
                            user_id: r.user_id_1,
                            username: r.username_1,
                            display_name: r.display_name_1,
                            rank: r.rank_point_1
                        },
                        {
                            score: r.user_2_score,
                            user_id: r.user_id_2,
                            username: r.username_2,
                            display_name: r.display_name_2,
                            rank: r.rank_point_2
                        }
                    ]
                } as IPvpTournamentMatchInfo
            });
        } catch (e) {
            this._logger.error(e);
            return [];
        }
    }

    private databaseStatusToEnum(status: String): PvpTournamentMatchStatus {
        switch (status) {
            case 'PENDING':
                return PvpTournamentMatchStatus.Pending;
            case 'ABORTED':
                return PvpTournamentMatchStatus.Aborted;
            case 'COMPLETED':
                return PvpTournamentMatchStatus.Completed;
            default:
                return PvpTournamentMatchStatus.Aborted;
        }
    }
}