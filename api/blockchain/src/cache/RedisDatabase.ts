import {createClient, RedisClientType} from 'redis';
import ILogger from "../services/ILogger";
import {IConfig} from "../Config";
import IRedisDatabase, {MultipleFields, SingleField} from "./IRedisDatabase";
import getRedisClient from "./Redis";

const K_OK = 'OK';

export default class RedisDatabase implements IRedisDatabase {
  private readonly _redis: RedisClientType;
  readonly #logger: ILogger;

  constructor(
    logger: ILogger,
    redisConnectionString: string
  ) {
    this.#logger = logger.clone('[REDIS]');
    this._redis = getRedisClient(redisConnectionString);
  }

  async testConnection(): Promise<boolean> {
    const res = await this._redis.ping();
    this.#logger.info(`PING response: ${res}`);
    return res === 'PONG';
  }

  async get(key: string): Promise<string | null> {
    return this._redis.GET(key);
  }

  async mGet(keys: string[]): Promise<(string | null)[]> {
    return this._redis.MGET(keys);
  }

  async getAllKeys(pattern: string): Promise<string[]> {
    return this._redis.KEYS(pattern);
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

  async delAllKeys(pattern: string): Promise<void> {
    const keys = await this.getAllKeys(pattern);
    if (keys.length === 0) {
      return;
    }
    const numberKeysRemoved = await this.mDel(keys);
  }

  // =========== Set<string> ===========

  async readSet(key: string): Promise<string[]> {
    return await this._redis.sMembers(key);
  }

  async addToSet(key: string, values: string[]): Promise<boolean> {
    const memberAdded = await this._redis.sAdd(key, values);
    return memberAdded > 0;
  }

  async removeFromSet(key: string, values: string[]): Promise<boolean> {
    const memberRemoved = await this._redis.sRem(key, values);
    return memberRemoved > 0;
  }

  // =========== Hash<string,string> ===========

  async readHash(key: string): Promise<MultipleFields> {
    const fieldsValues = await this._redis.HGETALL(key);
    return new Map<string, string>(Object.entries(fieldsValues));
  }

  async readHashFields(key: string, fieldsNames: string[]): Promise<(string | undefined)[]> {
    const result = await this._redis.HMGET(key, fieldsNames);
    return result.map(e => {
      if (!e || e === "null") {
        return undefined;
      }
      return e;
    });
  }

  async addToHash(key: string, fieldsValues: SingleField | MultipleFields): Promise<boolean> {
    if (fieldsValues instanceof Map) {
      const memberAdded = await this._redis.HSET(key, fieldsValues);
      return memberAdded > 0;
    } else {
      const [fieldKey, fieldValue] = fieldsValues;
      const memberAdded = await this._redis.HSET(key, fieldKey, fieldValue);
      return memberAdded > 0;
    }
  }

  async removeFromHash(key: string, fieldsNames: string[]): Promise<boolean> {
    const memberRemoved = await this._redis.HDEL(key, fieldsNames);
    return memberRemoved > 0;
  }

  async setHashFieldWithTTL(key: string, field: string, value: string, ttlSeconds: number): Promise<void> {
    await this._redis.HSET(key, field, value);
    if (ttlSeconds > 0) {
      await this._redis.HEXPIRE(key, field, ttlSeconds);
    }
  }
}