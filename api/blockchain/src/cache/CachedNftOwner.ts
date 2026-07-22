// Mục đích để loại trừ các NFT mà ko còn là owner nữa

import {IConfig} from "../Config";
import IRedisDatabase from "./IRedisDatabase";
import {CacheStatus} from "../consts/Consts";

interface IValue {
  timestamp: number;
  walletAddress: string;
}

export interface IReturnValue {
  status: CacheStatus;
  walletAddress: string;
}

const REDIS_KEY = 'blkch-nft-owner';
const REDIS_KEY_EXPIRE = 3600; // 1 hour - thời gian để auto xoá key khỏi redis

export class CachedNftOwner {
  constructor(
    private readonly envConfig: IConfig,
    private readonly redis: IRedisDatabase
  ) {
    this.MaxCacheTime = envConfig.stakeCacheSeconds;
  }

  private readonly MaxCacheTime: number; // thời gian để refresh

  async getCachedOwnerOfNftId(cacheKey: string, nftId: number): Promise<IReturnValue> {
    const k = `${REDIS_KEY}-${cacheKey}-${nftId}`;
    const v = await this.redis.get(k);

    const notExistValue = {
      status: CacheStatus.NotExist,
      walletAddress: ''
    };

    if (!v) {
      return notExistValue;
    }

    try {
      const value = JSON.parse(v) as IValue;
      const isExpired = Date.now() - value.timestamp > this.MaxCacheTime;
      return {
        status: isExpired ? CacheStatus.Expired : CacheStatus.Valid,
        walletAddress: value.walletAddress
      };
    } catch (e) {
      console.error(e);
      return notExistValue;
    }
  }

  setCachedOwnerOfNftId(cacheKey: string, nftId: number, walletAddress: string) {
    const k = `${REDIS_KEY}-${cacheKey}-${nftId}`;
    const v: IValue = {
      timestamp: Date.now(),
      walletAddress: walletAddress
    };
    this.redis.setWithTTL(k, JSON.stringify(v), REDIS_KEY_EXPIRE).catch(console.error);
  }
}