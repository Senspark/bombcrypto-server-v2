import IMatchFinder from "../IMatchFinder";
import {IMatch, IUser, PvpMode} from "../../consts/PvpData";
import MatchCreator from "./MatchCreator";
import GroupUsersInSameZone from "./GroupUsersInSameZone";
import {IFindResult} from "./Data";
import UserMeetManager from "../UserMeetManager";
import NetworkAddress from "../NetworkAddress";



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
        const zones = this._groupUsersInSameZone.createZoneGroup(users);

        for (const [zone, usersInZone] of zones) {
            const groupedByModeAndWager = new Map<string, IUser[]>();
            for (const user of usersInZone) {
                if (!user.newServer) continue;
                const key = `${user.mode}_${user.data.wagerMode}_${user.data.wagerTier}_${user.data.wagerToken}_${user.data.network}`;
                if (!groupedByModeAndWager.has(key)) {
                    groupedByModeAndWager.set(key, []);
                }
                groupedByModeAndWager.get(key)!.push(user);
            }

            for (const [key, usersGroup] of groupedByModeAndWager) {
                const mode = usersGroup[0].mode;
                const rule = this.getRule(mode);
                if (!rule) continue;
                const roomSize = rule.roomSize;

                let i = 0;
                while (i + roomSize <= usersGroup.length) {
                    const candidates = usersGroup.slice(i, i + roomSize);
                    
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
                        const serverId = this._networkAddress.convertZoneToServerId(zone);
                        const serverDetail = this._networkAddress.convertZoneToServerDetail(zone);
                        const match = this._matchCreator.createMatch(serverId, serverDetail, mode, candidates, {desc: 'Same zone match'});
                        newMatches.push(match);
                        i += roomSize;
                    } else {
                        i++;
                    }
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