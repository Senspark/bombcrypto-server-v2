import IMatchFinder from "../IMatchFinder";
import {IMatch, IUser} from "../../consts/PvpData";
import MatchFinderUsersInSameRank from "./MatchFinderUsersInSameRank";
import GroupUsersInSameZone from "./GroupUsersInSameZone";
import {IFindResult} from "./Data";
import UserMeetManager from "../UserMeetManager";
import NetworkAddress from "../NetworkAddress";

export default class MatchFinderUsersInSameZoneAndSameRank implements IMatchFinder {
    _groupUsersInSameZone = new GroupUsersInSameZone();
    _rankFinder: MatchFinderUsersInSameRank
    _userMeetManager: UserMeetManager;

    constructor(userMeetManager: UserMeetManager, private readonly _networkAddress: NetworkAddress) {
        this._userMeetManager = userMeetManager;
        this._rankFinder = new MatchFinderUsersInSameRank(this._userMeetManager, this._networkAddress);
    }

    async find(users: IUser[]): Promise<IFindResult> {
        const newMatches: IMatch[] = [];
        const zones = this._groupUsersInSameZone.createZoneGroup(users);

        for (const [zone, usersInZone] of zones) {
            const findResult = await this._rankFinder.find(usersInZone);
            const matches = findResult.matchesFound;
            matches.forEach(e => e.desc = 'Same zone + rank match')
            newMatches.push(...matches);
        }

        return {
            matchesFound: newMatches,
            pendingUsers: []
        };
    }
}