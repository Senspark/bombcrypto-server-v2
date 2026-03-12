import {RedisClientType} from 'redis';
import ILogger from "../../services/ILogger";
import {IRedisDatabase} from "../../Services";
import {IRedisField, IRedisHash, IRedisSet, MultipleFields, SingleField} from "../../services/IRedisDatabase";
import getRedisClient from "./Redis";

const K_OK = 'OK';

export default class RedisDatabase implements IRedisDatabase {
    private readonly _redis: RedisClientType;
    private readonly _logger: ILogger;

    readonly fields: IRedisField;
    readonly sets: IRedisSet;
    readonly hashes: IRedisHash;

    constructor(
        logger: ILogger,
        redisConnectionString: string,
    ) {
        this._logger = logger.clone('[REDIS]');
        this._redis = getRedisClient(redisConnectionString, this._logger);
        this.fields = new Field(this._redis);
        this.sets = new Set(this._redis);
        this.hashes = new Hash(this._redis, this._logger);
    }

    async testConnection(): Promise<boolean> {
        const res = await this._redis.ping();
        this._logger.info(`PING response: ${res}`);
        return res === 'PONG';
    }

    async getAllKeys(pattern: string): Promise<string[]> {
        return this._redis.KEYS(pattern);
    }

    async delAllKeys(pattern: string): Promise<void> {
        const keys = await this.getAllKeys(pattern);
        if (keys.length === 0) {
            return;
        }
        const numberKeysRemoved = await this.fields.mDel(keys);
    }
}

class Field implements IRedisField {
    constructor(
        private readonly _redis: RedisClientType
    ) {
    }

    async get(key: string): Promise<string | null> {
        return this._redis.GET(key);
    }

    async mGet(keys: string[]): Promise<(string | null)[]> {
        return this._redis.MGET(keys);
    }


    async set(key: string, value: string): Promise<boolean> {
        const res = await this._redis.SET(key, value);
        return res === K_OK;
    }

    async setWithTTL(key: string, value: string, ttlSeconds: number): Promise<boolean> {
        if (ttlSeconds <= 0) {
            return await this.set(key, value);
        } else {
            const res = await this._redis.SET(key, value, {EX: ttlSeconds});
            return res === K_OK;
        }
    }

    async del(key: string): Promise<void> {
        const numberKeysRemoved = await this._redis.DEL(key);
    }

    async mDel(keys: string[]): Promise<void> {
        const numberKeysRemoved = await this._redis.DEL(keys);
    }
}

class Set implements IRedisSet {
    constructor(
        private readonly _redis: RedisClientType
    ) {
    }

    async read(key: string): Promise<string[]> {
        return await this._redis.sMembers(key);
    }

    async fieldExists(key: string, fieldName: string): Promise<boolean> {
        return await this._redis.sIsMember(key, fieldName);
    }

    async add(key: string, values: string[]): Promise<boolean> {
        const memberAdded = await this._redis.sAdd(key, values);
        return memberAdded > 0;
    }

    async remove(key: string, values: string[]): Promise<boolean> {
        const memberRemoved = await this._redis.sRem(key, values);
        return memberRemoved > 0;
    }
}

class Hash implements IRedisHash {
    constructor(
        private readonly _redis: RedisClientType,
        private readonly _logger: ILogger
    ) {
    }

    async read(key: string): Promise<MultipleFields> {
        const fieldsValues = await this._redis.HGETALL(key);
        return new Map<string, string>(Object.entries(fieldsValues));
    }

    async readFields(key: string, fieldsNames: string[]): Promise<(string | undefined)[]> {
        const result = await this._redis.HMGET(key, fieldsNames);
        return result.map(e => {
            if (!e || e === "null") {
                return undefined;
            }
            return e;
        });
    }

    async readField(key: string, fieldName: string): Promise<string | undefined> {
        const results = await this.readFields(key, [fieldName]);
        if (results.length === 0) {
            return undefined;
        }
        return results[0];
    }

    async add(key: string, fieldsValues: SingleField | MultipleFields): Promise<boolean> {
        return this.addWithTTL(key, fieldsValues, 0);
    }

    async addWithTTL(key: string, fieldsValues: SingleField | MultipleFields, ttlSeconds: number): Promise<boolean> {
        let memberAdded: number;
        let fieldsKeys: string | string[];
        if (fieldsValues instanceof Map) {
            memberAdded = await this._redis.HSET(key, fieldsValues);
            fieldsKeys = Array.from(fieldsValues.keys());
        } else {
            const [fieldKey, fieldValue] = fieldsValues;
            memberAdded = await this._redis.HSET(key, fieldKey, fieldValue);
            fieldsKeys = fieldKey;
        }
        if (ttlSeconds > 0) {
            await this._redis.HEXPIRE(key, fieldsKeys, ttlSeconds);
        }
        return memberAdded > 0;
    }

    async setTTL(key: string, fields: string[], ttlSeconds: number): Promise<boolean> {
        const changed = await this._redis.HEXPIRE(key, fields, ttlSeconds);
        return changed.length > 0;
    }

    async remove(key: string, fieldsNames: string[]): Promise<boolean> {
        const memberRemoved = await this._redis.HDEL(key, fieldsNames);
        return memberRemoved > 0;
    }

}