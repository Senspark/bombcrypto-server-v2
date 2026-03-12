import LocalExpireMap from "./utils/LocalExpireMap";
import {ILogger} from "../Services";
import {ToadScheduler} from "toad-scheduler";
import {UserAccount} from "./DatabaseAccess";

export class UserAccountCache {

    private readonly _map: LocalExpireMap<string, UserAccount>
    private readonly _cacheSeconds = 60;

    constructor(
        private readonly _logger: ILogger,
        private readonly _scheduler: ToadScheduler,
    ) {
        this._map = new LocalExpireMap<string, UserAccount>(_logger, _scheduler);
    }

    public async set(userName: string, account: UserAccount) {
        await this._map.add(userName, account, this._cacheSeconds);
    }

    public async get(userName: string) {
        const u = await this._map.get(userName);
        if (u) {
            await this._map.extendExpireTime(userName, this._cacheSeconds);
        }
        return u;
    }
}