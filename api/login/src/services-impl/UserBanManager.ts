import {ILogger, IRedisDatabase} from "../Services";
import ErrorTracking from "./utils/ErrorTracking";
import {RedisKeys} from "../consts/Consts";
import {SimpleIntervalJob, Task, ToadScheduler} from "toad-scheduler";

const SYNC_INTERVAL_MINUTES = 1;

export default class UserBanManager {
    constructor(
        logger: ILogger,
        syncBannedList: boolean,
        private readonly _errorTracking: ErrorTracking,
        private readonly _redis: IRedisDatabase,
        private readonly _scheduler: ToadScheduler,
    ) {
        this._logger = logger.clone('[UBM]');
        this.updateBannedList().catch(e => this._logger.error(e));

        if (syncBannedList) {
            const task = new Task('Update Banned list scheduler', this.updateBannedList.bind(this), (e: Error) => {
                this._logger.error(`Update Banned list scheduler error: ${e.message}`);
            });
            const job = new SimpleIntervalJob({minutes: SYNC_INTERVAL_MINUTES}, task);
            this._scheduler.addSimpleIntervalJob(job);
        }
    }

    private readonly _logger: ILogger;
    private readonly _bannedList: Set<string> = new Set();

    async updateBannedList() {
        try {
            this._bannedList.clear();
            const bannedList = await this._redis.sets.read(RedisKeys.AP_SOL_LOGIN_BANNED);
            if (bannedList) {
                bannedList.forEach(walletAddress => this._bannedList.add(walletAddress));
            }
            this._logger.info(`Banned list updated`);
        } catch (e) {
            this._logger.error(`Banned list update error: ${e.message}`);
        }
    }

    isBanned(walletAddress: string): boolean {
        if (this._bannedList.has(walletAddress)) {
            return true;
        }
        const isBanned = this._errorTracking.isSuitableToBanned(walletAddress);
        if (isBanned) {
            this._bannedList.add(walletAddress);
            this._errorTracking.clearData(walletAddress);
            this._redis.sets.add(RedisKeys.AP_SOL_LOGIN_BANNED, [walletAddress]).catch(e => this._logger.error(e));
            this._logger.info(`User ${walletAddress} is banned`);
            return true;
        }
        return false;
    }

    exportBannedList(): string[] {
        return Array.from(this._bannedList);
    }
}