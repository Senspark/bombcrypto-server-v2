import {IMatch} from "../consts/PvpData";
import IDependencies from "../services/IDependencies";
import {StreamKeys} from "../cache/CachedKeys";
import ILogger from "../services/ILogger";

export default interface IMessenger {
    registerMatch(match: IMatch): void;
}

export class Messenger implements IMessenger {
    readonly _logger: ILogger;

    constructor(
        readonly _dep: IDependencies,
    ) {
        this._logger = _dep.logger.clone('[MESSENGER]');
    }

    registerMatch(match: IMatch): void {
        //const record = {data: JSON.stringify(match)};
        (async () => {
            try {
                // Send both types of variables to support old client
                //const matchReturn = IMatchToIMatchReturn(match);
                await this._dep.messenger.send(StreamKeys.AP_PVP_MATCH_FOUND_STR, match);
                // Save the meeting history of the two users to redis
                //await this._userMatchManager.addMatchToRedis(match.users[0].id, match.users[1].id);
                this._logger.info(`Match ${match.id} registered`);
            } catch (e) {
                this._logger.error(e);
            }
        })();
    }
}