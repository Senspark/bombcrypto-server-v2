import IAutoExpireMap from "./IAutoExpireMap";
import {ILogger, IRedisDatabase} from "../../Services";

export default class RedisExpireMap<V> implements IAutoExpireMap<string, V> {

    constructor(
        private readonly _logger: ILogger,
        private readonly _redis: IRedisDatabase,
        private readonly _redisHashKey: string,
        sampleData: V,
    ) {
        // get type of V
        const t = typeof sampleData;
        if (t === 'string') {
            this._fromStrToV = (value: string) => value as unknown as V;
            this._fromVToStr = (value: V) => value as unknown as string;
        } else if (t === 'object') {
            this._fromStrToV = JSON.parse;
            this._fromVToStr = JSON.stringify;
        } else {
            throw new Error(`Not support type of ${t}`);
        }
    }

    private readonly _fromStrToV: (value: string) => V;
    private readonly _fromVToStr: (value: V) => string;

    async add(key: string, value: V, expireSeconds: number): Promise<void> {
        const v = this._fromVToStr(value);
        await this._redis.hashes.addWithTTL(this._redisHashKey, [key, v], expireSeconds);
    }

    async get(key: string): Promise<V | null> {
        const value = await this._redis.hashes.readField(this._redisHashKey, key);
        if (!value) {
            return null;
        }
        return this._fromStrToV(value);
    }

    async extendExpireTime(key: string, expireSeconds: number): Promise<void> {
        await this._redis.hashes.setTTL(this._redisHashKey, [key], expireSeconds);
    }

    async remove(key: string): Promise<void> {
        await this._redis.hashes.remove(this._redisHashKey, [key]);
    }
}