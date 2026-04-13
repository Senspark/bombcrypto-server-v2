import IMatchFinder from "../IMatchFinder";
import {IMatch, IUser, PvpMode} from "../../consts/PvpData";
import IDependencies from "../../services/IDependencies";
import MatchCreator from "./MatchCreator";
import {CachedKeys} from "../../cache/CachedKeys";
import {IFindResult} from "./Data";
import NetworkAddress from "../NetworkAddress";
import {createBotUser} from "./MatchFinderWithBotIfNewPlayer";

export default class MatchFinderInChosenServer implements IMatchFinder {
    readonly _matchCreator = new MatchCreator();

    constructor(private readonly _dep: IDependencies, private readonly _networkAddress: NetworkAddress) {
    }

    async find(users: IUser[]): Promise<IFindResult> {
        const fixtureMatches = await this.getTestMatchesFromCached();
        const newMatches: IMatch[] = [];
        const matchedUsers = new Set<string>();
        const pendingUsers = new Set<string>();

        for (const curUser of users) {
            if (!fixtureMatches.has(curUser.id.toLowerCase())) {
                continue;
            }
            pendingUsers.add(curUser.id);
            const fx = fixtureMatches.get(curUser.id)!!;
            const player1 = curUser;
            let player2: IUser | undefined;
            // If there is no user2, let the player play with a bot
            if (fx[0].user2 === 'bot') {
                player2 = createBotUser(player1);
                if (matchedUsers.has(player1.id)) {
                    continue;
                }
            } else {
                // By default, take the first one because in reality there is only one case
                const player2Id = fx[0].user1 === curUser.id ? fx[0].user2 : fx[0].user1;
                player2 = users.find(u => u.id === player2Id);
                if (!player2) {
                    player1.refreshTimestamp = Date.now();
                    // keep player1 in the queue until player2 appears
                    continue;
                }
                if (matchedUsers.has(player1.id) || matchedUsers.has(player2.id)) {
                    continue;
                }
            }

            const zone = fx[0].zone;
            const serverId = this._networkAddress.convertZoneToServerId(zone);
            const serverDetail = this._networkAddress.convertZoneToServerDetail(zone);
            const match = this._matchCreator.createMatch(serverId, serverDetail, PvpMode.FFA_2, [player1, player2], {
                desc: "Pre-selected test server"
            });
            matchedUsers.add(player1.id);
            newMatches.push(match);
        }
        return {
            matchesFound: newMatches,
            pendingUsers: Array.from(pendingUsers)
        };
    }

    private async getTestMatchesFromCached(): Promise<Map<string, ITestMatch>> {
        try {
            const cached = await this._dep.redis.readHash(CachedKeys.AP_PVP_TEST_MATCHES);
            this._dep.logger.assert(cached, 'Test matches not found in cache');
            const resultMap = new Map<string, ITestMatch>();
            cached.forEach((value, key) => {
                resultMap.set(key, JSON.parse(value));
            });
            return resultMap;
        } catch (e) {
            return new Map();
        }
    }
}

interface ITestMatch {
    user1: string;
    user2: string;
    zone: string;
}