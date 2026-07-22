import {IConfig} from "../Config";
import IRedisDatabase from "./IRedisDatabase";
import {CacheStatus} from "../consts/Consts";

interface IValue {
  timestamp: number;
  details: string[];
}

export interface IReturnValue {
  status: CacheStatus;
  details: string[];
}

const REDIS_KEY = 'blkch-nft';
const REDIS_KEY_EXPIRE = 3600; // 1 hour - thời gian để auto xoá key khỏi redis 

export class CachedNftDetails {
  constructor(
    private readonly envConfig: IConfig,
    private readonly redis: IRedisDatabase
  ) {
    this.MaxCacheTime = envConfig.stakeCacheSeconds;
  }

  private readonly MaxCacheTime: number; // thời gian để refresh

  async getDetailsByOwner(key: string, walletAddress: string): Promise<IReturnValue> {
    const k = `${REDIS_KEY}-${key}-${walletAddress}`;
    return this.getDetailsByKey(k);
  }

  setDetailsByOwner(key: string, walletAddress: string, details: string[]) {
    const k = `${REDIS_KEY}-${key}-${walletAddress}`;
    this.setDetailsByKey(k, details).catch(console.error);
  }

  async getDetailsById(key: string, houseId: number): Promise<IReturnValue> {
    const k = `${REDIS_KEY}-${key}-${houseId}`;
    return this.getDetailsByKey(k);
  }

  setDetailsById(key: string, houseId: number, details: string[]) {
    const k = `${REDIS_KEY}-${key}-${houseId}`;
    this.setDetailsByKey(k, details).catch(console.error);
  }

  async clearAll() {
    const keys = await this.redis.getAllKeys(`${REDIS_KEY}*`);
    for (const key of keys) {
      this.redis.del(key).then();
    }
  }

  async getDetailsByKey(k: string): Promise<IReturnValue> {
    const v = await this.redis.get(k);
    const notExistValue: IReturnValue = {
      status: CacheStatus.NotExist,
      details: []
    };
    if (!v) {
      return notExistValue;
    }

    try {
      const value = JSON.parse(v) as IValue;
      if (!Array.isArray(value.details)) {
        this.redis.del(k).then();
        return notExistValue;
      }
      const expired = value.timestamp + this.MaxCacheTime > Date.now();
      return {
        status: expired ? CacheStatus.Expired : CacheStatus.Valid,
        details: value.details
      };
    } catch (e) {
      this.redis.del(k).then();
      console.error(e);
      return notExistValue;
    }
  }

  async setDetailsByKey(k: string, v: string[]) {
    const value = JSON.stringify({
      timestamp: Date.now(),
      details: v,
    });
    console.info(`Set nft details: ${k} - ${value}`);
    this.redis.setWithTTL(k, value, REDIS_KEY_EXPIRE).catch(console.error);
  }
}

