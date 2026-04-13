import {RedisClientType} from 'redis';
import ILogger from "../services/ILogger";
import {IConfig} from "../Config";
import IRedisDatabase from "./IRedisDatabase";
import getRedisClient from "./Redis";

const K_OK = 'OK';

export default class RedisDatabase implements IRedisDatabase {
    readonly _logger: ILogger;
    private readonly _redis: RedisClientType;

    constructor(
        logger: ILogger,
        envConfig: IConfig
    ) {
        this._logger = logger.clone('[REDIS]');
        this._redis = getRedisClient(envConfig.redisConnectionString);
    }

    async testConnection(): Promise<boolean> {
        const res = await this._redis.ping();
        this._logger.info(`PING response: ${res}`);
        return res === 'PONG';
    }

    async get(key: string): Promise<string | null> {
        return this._redis.get(key);
    }

    async mGet(keys: string[]): Promise<(string | null)[]> {
        return this._redis.mGet(keys);
    }

    async getAllKeys(pattern: string): Promise<string[]> {
        return this._redis.keys(pattern);
    }

    async set(key: string, value: string): Promise<boolean> {
        const res = await this._redis.set(key, value);
        return res === K_OK;
    }

    async addToHash(key: string, fieldsValues: Map<string, string>): Promise<boolean> {
        const memberAdded = await this._redis.HSET(key, fieldsValues);
        return memberAdded > 0;
    }


    async readHash(key: string): Promise<Map<string, string>> {
        const fieldsValues = await this._redis.HGETALL(key);
        return new Map<string, string>(Object.entries(fieldsValues));
    }

    async removeFromHash(key: string, fields: string[]): Promise<boolean> {
        const memberRemoved = await this._redis.HDEL(key, fields);
        return memberRemoved > 0;
    }

    async setWithTTL(key: string, value: string, ttlSeconds: number): Promise<boolean> {
        if (ttlSeconds <= 0) {
            return await this.set(key, value);
        } else {
            const res = await this._redis.set(key, value, {EX: ttlSeconds});
            return res === K_OK;
        }
    }

    async del(key: string): Promise<void> {
        const numberKeysRemoved = await this._redis.del(key);
    }

    async mDel(keys: string[]): Promise<void> {
        const numberKeysRemoved = await this._redis.del(keys);
    }

    async delAllKeys(pattern: string): Promise<void> {
        const keys = await this.getAllKeys(pattern);
        if (keys.length === 0) {
            return;
        }
        const numberKeysRemoved = await this.mDel(keys);
    }
}