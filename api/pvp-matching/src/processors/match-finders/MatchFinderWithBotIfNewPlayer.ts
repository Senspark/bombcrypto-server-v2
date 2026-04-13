import IMatchFinder from "../IMatchFinder";
import {IMatch, IUser, PvpMode} from "../../consts/PvpData";
import MatchCreator from "./MatchCreator";
import {IPvpConfig} from "../../Config";
import {IFindResult} from "./Data";
import NetworkAddress from "../NetworkAddress";

const OnlySupportPvpMode = PvpMode.FFA_2;

/**
 * Users who have played less than 3 matches will have to meet a Bot
 */
export default class MatchFinderWithBotIfNewPlayer implements IMatchFinder {

    readonly _matchCreator = new MatchCreator();

    constructor(
        private readonly _config: IPvpConfig,
        private readonly _networkAddress: NetworkAddress
    ) {
    }

    async find(users: IUser[]): Promise<IFindResult> {
        const matches = users.map(user => this.tryCreateMatch(user)).filter(match => match !== null) as IMatch[];
        return {
            matchesFound: matches,
            pendingUsers: []
        };
    }

    private tryCreateMatch(user: IUser): IMatch | null {
        if (user.mode !== OnlySupportPvpMode) {
            return null;
        }
        if (user.totalMatchCount > this._config.maxTotalMatchForFindingBot) {
            return null;
        }
        const bot = createBotUser(user);
        const zone = user.networkStats.lowestPingZone;
        const serverId = this._networkAddress.convertZoneToServerId(zone);
        const serverDetail = this._networkAddress.convertZoneToServerDetail(zone);
        return this._matchCreator.createMatch(serverId, serverDetail, OnlySupportPvpMode, [user, bot], {desc: 'New user'});
    }
}

export function createBotUser(user: IUser): IUser {
    // Clone.
    const item: IUser = JSON.parse(JSON.stringify(user));
    item.data.isBot = true;
    return item;
}