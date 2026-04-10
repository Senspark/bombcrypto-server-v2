import IDependencies from "../services/IDependencies";
import ILogger from "../services/ILogger";
import IRedisDatabase from "../cache/IRedisDatabase";
import {IConfig} from "../Config";

export default class UserMeetManager {
    _logger: ILogger;
    _redis: IRedisDatabase;
    _envConfig: IConfig;
    maxCanMeet: number;
    key = 'USER_MEET_COUNT';
    mapUserMatch = new Map<string, string>();
    setNeedDelete = new Set<string>();
    setNeedSave = new Set<string>();

    constructor(private readonly _dep: IDependencies) {
        this._logger = _dep.logger.clone('[USER_MATCH]');
        this._redis = _dep.redis;
        this._envConfig = _dep.envConfig;
        this.maxCanMeet = this._envConfig.pvpConfig.maxNumberTwoPlayerCanMeet;

        // Load the number of times two users have met from redis when initializing
        this.loadMatchesFromRedis().then()
    }

    getCombineUserName(userName1: string, userName2: string) {
        return userName1 > userName2 ? `${userName1}_${userName2}` : `${userName2}_${userName1}`;
    }

    async addMatchToRedis(combinedUserName: string) {
        // Check if the MapUserMatch has the key
        if (this.mapUserMatch.has(combinedUserName)) {
            // If it does, increment the value by 1
            const currentValue = this.mapUserMatch.get(combinedUserName);
            if (currentValue !== undefined) {
                const [value, date] = currentValue.split('_');
                this.mapUserMatch.set(combinedUserName, `${Number(value) + 1}_${date}`);
            }
        } else {
            // If it doesn't, add a new one
            // Get today's date
            const today = new Date();
            const formattedDate = `${today.getDate()}`;
            this.mapUserMatch.set(combinedUserName, `1_${formattedDate}`);
        }

        // Send the key and value to Redis
        const valueToSend = this.mapUserMatch.get(combinedUserName);
        if (valueToSend !== undefined) {
            return await this._redis.addToHash(this.key, new Map([[combinedUserName, valueToSend]]));
        }
        return false;
    }

    async loadMatchesFromRedis() {
        this.mapUserMatch = await this._redis.readHash(this.key);
    }

    canMatchTogether(userName1: string, userName2: string) {
        const combinedUserName = this.getCombineUserName(userName1, userName2);
        const value = this.mapUserMatch.get(combinedUserName);

        if (value !== undefined) {
            const [count, date] = value.split('_');
            const today = new Date().getDate().toString();

            // If a new day has passed, reset the meeting count and allow matching
            if (date !== today) {
                this.mapUserMatch.delete(combinedUserName);
                // Add to set for deletion from redis later
                this.addToDelete(combinedUserName);
                // Add to set for saving to redis later
                this.addToSave(combinedUserName);
                return true;
            }
            // Met more times than allowed in a day
            if (Number(count) >= this.maxCanMeet && date === today) {
                return false;
            }
            // Met fewer times than allowed, allow matching
            if (Number(count) < this.maxCanMeet && date === today) {
                this.addToSave(combinedUserName);
                return true;
            }
        }
        // First time meeting
        this.addToSave(combinedUserName);
        return true;
    }

    async SyncWithRedis() {
        // Delete keys that have passed to a new day from redis
        if (this.setNeedDelete.size > 0) {
            await this._redis.removeFromHash(this.key, Array.from(this.setNeedDelete));
            this.setNeedDelete.clear();
        }

        // Save new keys or update meeting counts on redis
        if (this.setNeedSave.size > 0) {
            this.setNeedSave.forEach((key) => {
                this.addMatchToRedis(key);
            });

            this.setNeedSave.clear();
        }

        // Resynchronize between redis and local variable, to support manual deletion on redis
        await this.loadMatchesFromRedis();
    }

    private addToSave(combinedUserName: string) {
        if (!this.setNeedSave.has(combinedUserName)) {
            this.setNeedSave.add(combinedUserName);
        }
    }

    private addToDelete(combinedUserName: string) {
        if (!this.setNeedDelete.has(combinedUserName)) {
            this.setNeedDelete.add(combinedUserName);
        }
    }


}