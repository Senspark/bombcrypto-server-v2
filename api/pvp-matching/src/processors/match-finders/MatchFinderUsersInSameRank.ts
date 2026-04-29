import IMatchFinder from "../IMatchFinder";
import MatchCreator from "./MatchCreator";
import {IMatch, IUser, PvpMode} from "../../consts/PvpData";
import NetworkStats from "../NetworkStats";
import {IFindResult} from "./Data";
import UserMeetManager from "../UserMeetManager";
import NetworkAddress from "../NetworkAddress";

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

        const groupedByModeAndWager = new Map<string, IUser[]>();
        for (const user of users) {
            if (!user.newServer) continue;
            const key = `${user.mode}_${user.data.gameMode}_${user.data.wagerMode}_${user.data.wagerTier}_${user.data.wagerToken}`;
            if (!groupedByModeAndWager.has(key)) {
                groupedByModeAndWager.set(key, []);
            }
            groupedByModeAndWager.get(key)!.push(user);
        }

        for (const [key, usersInGroup] of groupedByModeAndWager) {
            const mode = usersInGroup[0].mode;
            const matchCreator = new MatchCreator();
            // We need a dummy match to get the rule (room size)
            // Or we can import it. Let's just use a helper.
            const rule = this.getRule(mode);
            if (!rule) continue;

            const roomSize = rule.roomSize;
            
            // Sort users by rank to match similar players
            usersInGroup.sort((a, b) => a.rank - b.rank);

            let i = 0;
            while (i + roomSize <= usersInGroup.length) {
                const candidates = usersInGroup.slice(i, i + roomSize);
                
                // Check rank diff between min and max rank in candidates
                const minRank = candidates[0].rank;
                const maxRank = candidates[candidates.length - 1].rank;

                if (maxRank - minRank <= MAX_RANK_DIFF) {
                    // Check if they can all match together
                    let canMatch = true;
                    for (let x = 0; x < candidates.length; x++) {
                        for (let y = x + 1; y < candidates.length; y++) {
                            if (!this._userMeetManager.canMatchTogether(candidates[x].id, candidates[y].id)) {
                                canMatch = false;
                                break;
                            }
                        }
                        if (!canMatch) break;
                    }

                    if (canMatch) {
                        const zone = NetworkStats.findLowestPingZone(...candidates.map(u => u.networkStats));
                        const serverId = this._networkAddress.convertZoneToServerId(zone);
                        const serverDetail = this._networkAddress.convertZoneToServerDetail(zone);
                        const match = this._matchCreator.createMatch(serverId, serverDetail, mode, candidates, {desc: 'Same rank match'});
                        newMatches.push(match);
                        i += roomSize;
                        continue;
                    }
                }
                i++;
            }
        }
        // Đồng bộ với redis
        await this._userMeetManager.SyncWithRedis()

        return {
            matchesFound: newMatches,
            pendingUsers: []
        };
    }

    private getRule(mode: PvpMode) {
        const dict: { [key: number]: any } = {
            [PvpMode.FFA_2]: {roomSize: 2},
            [PvpMode.FFA_3]: {roomSize: 3},
            [PvpMode.FFA_4]: {roomSize: 4},
            [PvpMode.Team_2v2]: {roomSize: 4},
            [PvpMode.FFA_2_B3]: {roomSize: 2},
            [PvpMode.FFA_2_B5]: {roomSize: 2},
            [PvpMode.FFA_2_B7]: {roomSize: 2},
            [PvpMode.Team_3v3]: {roomSize: 6},
            [PvpMode.BATTLE_ROYALE]: {roomSize: 6},
        };
        return dict[mode];
    }
}