import IMatchFinder from "../IMatchFinder";
import {IMatch, IUser, PvpMode} from "../../consts/PvpData";
import MatchCreator from "./MatchCreator";
import GroupUsersInSameZone from "./GroupUsersInSameZone";
import {IFindResult} from "./Data";
import UserMeetManager from "../UserMeetManager";
import NetworkAddress from "../NetworkAddress";

const OnlySupportPvpMode = PvpMode.FFA_2;

/**
 * Prioritize matching with players in the same zone
 * But will not meet each other twice
 */
export default class MatchFinderUsersInSameZone implements IMatchFinder {

    _matchCreator = new MatchCreator();
    _groupUsersInSameZone = new GroupUsersInSameZone();
    _userMeetManager: UserMeetManager;

    constructor(userMeetManager: UserMeetManager, private readonly _networkAddress: NetworkAddress) {
        this._userMeetManager = userMeetManager;
    }

    async find(users: IUser[]): Promise<IFindResult> {
        const newMatches: IMatch[] = [];
        const matchedUsers = new Set<string>;
        const zones = this._groupUsersInSameZone.createZoneGroup(users);

        for (const [zone, usersInZone] of zones) {
            for (let i = 0; i < usersInZone.length; i++) {
                const user1 = usersInZone[i];
                if (!user1.newServer) {
                    continue;
                }
                for (let j = i + 1; j < usersInZone.length; j++) {
                    const user2 = usersInZone[j];
                    if (user1.mode !== OnlySupportPvpMode || user2.mode !== OnlySupportPvpMode) {
                        continue;
                    }
                    if (matchedUsers.has(user1.id) || matchedUsers.has(user2.id)) {
                        continue;
                    }
                    // Do not allow 2 users to play together too many times in a day
                    if (!this._userMeetManager.canMatchTogether(user1.id, user2.id)) {
                        continue;
                    }
                    const serverId = this._networkAddress.convertZoneToServerId(zone);
                    const serverDetail = this._networkAddress.convertZoneToServerDetail(zone);
                    const match = this._matchCreator.createMatch(serverId, serverDetail, user1.mode, [user1, user2], {desc: 'Same zone match'});
                    matchedUsers.add(user1.id);
                    matchedUsers.add(user2.id);
                    newMatches.push(match);
                }
            }
        }
        // Sync with redis
        await this._userMeetManager.SyncWithRedis()

        return {
            matchesFound: newMatches,
            pendingUsers: []
        };
    }
}