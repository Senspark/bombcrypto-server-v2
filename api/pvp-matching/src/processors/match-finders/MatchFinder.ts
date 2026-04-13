import IMatchFinder from "../IMatchFinder";
import IDependencies from "../../services/IDependencies";
import ILogger from "../../services/ILogger";
import {IMatch, IUser} from "../../consts/PvpData";
import MatchFinderWithBotIfWaitTooLong from "./MatchFinderWithBotIfWaitTooLong";
import MatchFinderFixture from "./MatchFinderFixture";
import MatchFinderWithBotIfNewPlayer from "./MatchFinderWithBotIfNewPlayer";
import MatchFinderUsersInSameZone from "./MatchFinderUsersInSameZone";
import MatchFinderUsersInSameZoneAndSameRank from "./MatchFinderUsersInSameZoneAndSameRank";
import MatchFinderUsersInSameRank from "./MatchFinderUsersInSameRank";
import {IFindResult} from "./Data";
import UserMeetManager from "../UserMeetManager";
import NetworkAddress from "../NetworkAddress";
import MatchFinderInChosenServer from "./MatchFinderInChosenServer";


/**
 * Includes 3 types of match finding:
 * 1. Match with Bot
 * 2. Match by Fixture (hardcoded list of opponents to meet)
 * 3. Random match
 */
export default class MatchFinder implements IMatchFinder {
    readonly _finders: IMatchFinder[];
    readonly _logger: ILogger;


    constructor(private readonly _dep: IDependencies,
                _userMeetManager: UserMeetManager,
                _networkAddress: NetworkAddress) {

        this._logger = this._dep.logger.clone('[MATCH_FINDER]');
        const pvpConfig = this._dep.envConfig.pvpConfig;
        this._finders = [
            new MatchFinderFixture(_dep, _networkAddress),
            new MatchFinderInChosenServer(_dep, _networkAddress),
            new MatchFinderWithBotIfNewPlayer(pvpConfig, _networkAddress),
            new MatchFinderUsersInSameZoneAndSameRank(_userMeetManager, _networkAddress),
            new MatchFinderUsersInSameZone(_userMeetManager, _networkAddress),
            new MatchFinderUsersInSameRank(_userMeetManager, _networkAddress),
            new MatchFinderWithBotIfWaitTooLong(pvpConfig, _networkAddress),
        ];
    }

    async find(users: IUser[]): Promise<IFindResult> {
        const allMatches: IMatch[] = [];
        let filteredUsers = [...users]; // clone
        let pendingUsers: string[] = [];

        for (const finder of this._finders) {
            const findResult = await finder.find(filteredUsers);
            const newMatches = findResult.matchesFound;
            const newPendingUsers = findResult.pendingUsers;

            allMatches.push(...newMatches);
            pendingUsers.push(...newPendingUsers);

            // Remove users who have already found a match
            const excluded = newMatches.flatMap(e => e.users).map(e => e.id);
            excluded.push(...findResult.pendingUsers);
            filteredUsers = filteredUsers.filter(e => !excluded.includes(e.id));
        }

        return {
            matchesFound: allMatches,
            pendingUsers: pendingUsers
        };
    }
}

