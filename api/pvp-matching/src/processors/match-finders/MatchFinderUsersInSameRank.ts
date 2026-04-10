import IMatchFinder from "../IMatchFinder";
import MatchCreator from "./MatchCreator";
import {IMatch, IUser, PvpMode} from "../../consts/PvpData";
import NetworkStats from "../NetworkStats";
import {IFindResult} from "./Data";
import UserMeetManager from "../UserMeetManager";
import NetworkAddress from "../NetworkAddress";

const OnlySupportPvpMode = PvpMode.FFA_2;
const MAX_RANK_DIFF = 2;

/**
 * Each user has a rank difference of no more than 2
 */
export default class MatchFinderUsersInSameRank implements IMatchFinder {
    _matchCreator = new MatchCreator();
    _userMeetManager: UserMeetManager;

    constructor(userMeetManager: UserMeetManager, private readonly _networkAddress: NetworkAddress) {
        this._userMeetManager = userMeetManager;
    }

    async find(users: IUser[]): Promise<IFindResult> {
        const newMatches: IMatch[] = [];
        const matchedUsers = new Set<string>();

        for (let i = 0; i < users.length; i++) {
            const user1 = users[i];
            if (!user1.newServer) {
                continue;
            }
            for (let j = i + 1; j < users.length; j++) {
                const user2 = users[j];
                if (user1.mode !== OnlySupportPvpMode || user2.mode !== OnlySupportPvpMode) {
                    continue;
                }

                if (matchedUsers.has(user1.id) || matchedUsers.has(user2.id)) {
                    continue;
                }

                const rankDiff = Math.abs(user1.rank - user2.rank);
                if (rankDiff > MAX_RANK_DIFF) {
                    continue;
                }

                // Do not allow 2 users to play together too many times in a day
                if (!this._userMeetManager.canMatchTogether(user1.id, user2.id)) {
                    continue;
                }

                const zone = NetworkStats.findLowestPingZone(user1.networkStats, user2.networkStats)
                const serverId = this._networkAddress.convertZoneToServerId(zone);
                const serverDetail = this._networkAddress.convertZoneToServerDetail(zone);

                const match = this._matchCreator.createMatch(serverId, serverDetail, user1.mode, [user1, user2], {desc: 'Same rank match'});
                matchedUsers.add(user1.id);
                matchedUsers.add(user2.id);
                newMatches.push(match);
            }
        }
        // Đồng bộ với redis
        await this._userMeetManager.SyncWithRedis()

        return {
            matchesFound: newMatches,
            pendingUsers: []
        };
    }

}