import IMatchFinder from "../IMatchFinder";
import {IMatch, IUser, PvpMode} from "../../consts/PvpData";
import IDependencies from "../../services/IDependencies";
import MatchCreator from "./MatchCreator";
import NetworkStats from "../NetworkStats";
import {CachedKeys} from "../../cache/CachedKeys";
import {IFindResult} from "./Data";
import NetworkAddress from "../NetworkAddress";

const CACHE_SECONDS = 60; // 1 minutes

/**
 * Principles:
 * 1. If the player is not in the list, skip them
 * 2. If the opponent is not yet in the queue, keep this user in the queue until:
 *   - The opponent appears
 *   - The search time stored in the database has expired
 */
export default class MatchFinderFixture implements IMatchFinder {
    readonly _matchCreator = new MatchCreator();

    constructor(private readonly _dep: IDependencies, private readonly _networkAddress: NetworkAddress) {
    }

    async find(users: IUser[]): Promise<IFindResult> {
        const fixtureMatches = await this.getFixtureMatchesFromCached();
        const newMatches: IMatch[] = [];
        const matchedUsers = new Set<string>();
        const pendingUsers = new Set<string>();

        for (const curUser of users) {
            if (!fixtureMatches.has(curUser.id)) {
                continue;
            }
            pendingUsers.add(curUser.id);
            const fx = fixtureMatches.get(curUser.id)!!;
            const player1 = curUser;
            // By default, take the first one because in reality there is only one case
            const player2Id = fx[0].user1 === curUser.id ? fx[0].user2 : fx[0].user1;
            const player2 = users.find(u => u.id === player2Id);
            if (!player2) {
                player1.refreshTimestamp = Date.now();
                // keep player1 in the queue until player2 appears
                continue;
            }

            if (matchedUsers.has(player1.id) || matchedUsers.has(player2.id)) {
                continue;
            }

            const zone = fx[0].fixedZone ? fx[0].fixedZone : NetworkStats.findLowestPingZone(player1.networkStats, player2!!.networkStats);
            const serverId = this._networkAddress.convertZoneToServerId(zone);
            const serverDetail = this._networkAddress.convertZoneToServerDetail(zone);
            const mode = fx[0].mode;
            const match = this._matchCreator.createMatch(serverId, serverDetail, mode, [player1, player2], {
                heroProfile: fx[0].heroProfile,
                desc: "Fixture match"
            });

            // Any match created by fixture match is a tournament match
            match.rule.isTournament = true;
            matchedUsers.add(player1.id);
            matchedUsers.add(player2.id);
            newMatches.push(match);
        }
        return {
            matchesFound: newMatches,
            pendingUsers: Array.from(pendingUsers)
        };
    }

    private async getFixtureMatchesFromCached(): Promise<Map<string, IFixtureMatch[]>> {
        try {
            const cached = await this._dep.redis.get(CachedKeys.AP_PVP_FIXTURE_MATCHES);
            this._dep.logger.assert(cached, 'Fixture matches not found in cache');
            this._dep.logger.assert(cached !== '{}', 'Fixture matches not found in cache');
            return new Map(JSON.parse(cached!!));
        } catch (e) {
            const map = await this.getFixtureMatchesFromDatabase();
            const cacheData = JSON.stringify(Array.from(map.entries()));
            await this._dep.redis.setWithTTL(CachedKeys.AP_PVP_FIXTURE_MATCHES, cacheData, CACHE_SECONDS);
            return map;
        }
    }

    private async getFixtureMatchesFromDatabase(): Promise<Map<string, IFixtureMatch[]>> {
        try {
            const sql =
                `SELECT pt.participant_1 AS player_1_username,
                        pt.participant_2 AS player_2_username,
                        pf.hero_profile,
                        pf.fixed_zone,
                        pf.mode
                 FROM pvp_tournament pt
                          LEFT JOIN pvp_fixture_matches pf ON pt.id = pf.id
                 WHERE status = 'PENDING'
                   AND NOW() >= from_time
                   AND NOW() <= to_time;`;
            const result = await this._dep.database.query(sql, []);

            const map = new Map<string, IFixtureMatch[]>();
            for (const r of result.rows) {
                const fx: IFixtureMatch = {
                    user1: r.player_1_username,
                    user2: r.player_2_username,
                    heroProfile: r.hero_profile ?? 0,
                    fixedZone: r.fixed_zone,
                    mode: r.mode ?? PvpMode.FFA_2
                };
                this.setUser(map, fx);
            }
            return map;
        } catch (e) {
            this._dep.logger.errors('MatchFinderFixture.getFixtureMatches', e);
            return new Map();
        }
    }

    private setUser(map: Map<string, IFixtureMatch[]>, fx: IFixtureMatch) {
        if (!map.has((fx.user1))) {
            map.set(fx.user1, [fx]);
        } else {
            map.get(fx.user1)!!.push(fx);
        }

        if (!map.has((fx.user2))) {
            map.set(fx.user2, [fx]);
        } else {
            map.get(fx.user2)!!.push(fx);
        }
    }
}

interface IFixtureMatch {
    user1: string;
    user2: string;
    heroProfile: number;
    fixedZone: string | undefined;
    mode: PvpMode;
}