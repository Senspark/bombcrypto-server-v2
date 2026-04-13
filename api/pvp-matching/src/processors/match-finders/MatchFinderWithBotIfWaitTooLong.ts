import IMatchFinder from "../IMatchFinder";
import {IMatch, IUser, PvpMode} from "../../consts/PvpData";
import MatchCreator from "./MatchCreator";
import {IPvpConfig} from "../../Config";
import {IFindResult} from "./Data";
import NetworkAddress from "../NetworkAddress";

const OnlySupportSingleMode = PvpMode.FFA_2;

/**
 * Users who queue for too long will be matched with a Bot
 */
export default class MatchFinderWithBotIfWaitTooLong implements IMatchFinder {

    readonly _matchCreator = new MatchCreator();

    constructor(
        private readonly _config: IPvpConfig,
        private readonly _networkAddress: NetworkAddress
    ) {
    }

    async find(users: IUser[]): Promise<IFindResult> {
        const now = Date.now();
        const matches = users.map(user => this.tryCreateMatch(user, now)).filter(match => match !== null) as IMatch[];
        return {
            matchesFound: matches,
            pendingUsers: []
        };
    }

    private tryCreateMatch(user: IUser, now: number): IMatch | null {
        if (user.mode !== OnlySupportSingleMode) {
            return null;
        }
        if (now - user.refreshTimestamp <= this._config.maxTimeForFindingUser) {
            return null;
        }
        const bot = createBotUser(user);
        let zone = user.networkStats.lowestPingZone;
        if (!user.newServer) {
            zone = zone + "1"
        }
        const serverId = this._networkAddress.convertZoneToServerId(zone);
        const serverDetail = this._networkAddress.convertZoneToServerDetail(zone);
        return this._matchCreator.createMatch(serverId, serverDetail, OnlySupportSingleMode, [user, bot], {desc: 'Waited too long'});
    }
}

function createBotUser(user: IUser): IUser {
    // Clone.
    const item: IUser = JSON.parse(JSON.stringify(user));
    item.data.isBot = true;
    return item;
}