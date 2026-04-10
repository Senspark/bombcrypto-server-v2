import {IMatch} from "../consts/PvpData";
import {IMatchReturn} from "../consts/PvpDataReturn";

export function IMatchToIMatchReturn(match: IMatch): IMatchReturn {
    return {
        id: match.id,
        zone: match.zone,
        serverDetail: JSON.stringify(match.serverDetail),
        mode: match.mode,
        rule: match.rule,
        team: match.team,
        users: match.users,
        desc: match.desc,
        timestamp: match.timestamp
    };
}

export function parseAvatar(avatar: number | undefined) {
    if (!avatar || avatar == 0) {
        return -1;
    }
    return avatar;
}