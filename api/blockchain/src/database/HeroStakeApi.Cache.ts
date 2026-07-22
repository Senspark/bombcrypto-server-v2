import {IHeroStakeApi, StakeAmount, StakeAmountBig} from "../IHeroStakeApi";
import {HeroStakeApiDatabase} from "./HeroStakeApi.Database";
import {CachedKeys, StreamKeys} from "../cache/CachedKeys";
import IDependencies from "../services/IDependencies";
import {CHAIN_NAMES} from "../BlockchainConfig";
import {formatNumber} from "../Utility";
import ILogger from "../services/ILogger";
import {BigNumber} from "ethers";

const REDIS_KEY = CachedKeys.AP_BL_HERO_STAKED;

export class HeroStakeApiCache implements IHeroStakeApi {
  constructor(
    private readonly _dep: IDependencies,
    private readonly _database: HeroStakeApiDatabase
  ) {
    this.#logger = _dep.logger.clone('[CACHE]');
  }

  readonly #logger: ILogger;

  public get name(): string {
    return this._database.network;
  }

  public async testConnection(): Promise<boolean> {
    return await this._dep.redis.testConnection();
  }

  async getStakedAmount(heroId: number): Promise<StakeAmount> {
    return (await this.getStakedAmounts(heroId))[0];
  }

  /**
   * Chỉ cần 1 id chưa cache thì sẽ query database
   */
  async getStakedAmounts(...heroIds: number[]): Promise<StakeAmount[]> {
    const result = await this.getStakedAmountsBig(...heroIds);
    return result.map(e => [formatNumber(e[0]), formatNumber(e[1])]);
  }

  async getStakedAmountsBig(...heroIds: number[]): Promise<StakeAmountBig[]> {
    if (heroIds.length === 0) {
      return [];
    }
    const [allSuccess, valuesFromCache] = await this.getCacheHeroStakeAmounts(...heroIds);
    if (allSuccess) {
      return valuesFromCache;
    }
    const valuesFromDatabase = await this._database.getStakedAmountsBig(...heroIds);
    const map = new Map<string, string>();
    for (let i = 0; i < heroIds.length; i++) {
      map.set(this.getKey(heroIds[i]), serializeStakeAmountBig(valuesFromDatabase[i]));
    }
    // this.#logger.info(`Set batch stake amount to cache`);
    await this._dep.redis.addToHash(REDIS_KEY, map);
    return valuesFromDatabase;
  }

  async setStakedAmount(heroId: number, amount: StakeAmountBig): Promise<StakeAmount> {
    const num = await this._database.setStakedAmount(heroId, amount);
    const k = this.getKey(heroId);
    const v = serializeStakeAmountBig(amount);
    this.#logger.info(`Set stake amount: ${k} - ${v}`);
    await this._dep.redis.addToHash(REDIS_KEY, [k, v]);
    return num;
  }

  async broadcastStakedAmount(heroId: number, amount: StakeAmountBig, uid?: number): Promise<void> {
    const stakeBcoin = formatNumber(amount[0]);
    const stakeSen = formatNumber(amount[1]);
    const d: IHeroStakeStreamData = {
      network: chainNameToNumber(this.name),
      hero_id: heroId,
      stake_amount: stakeBcoin,
      stake_bcoin: stakeBcoin,
      stake_sen: stakeSen
    };
    if (uid !== undefined) {
      d.uid = uid;
    }
    this._dep.messenger.send(StreamKeys.AP_BL_HERO_STAKE_STR, d).catch(this.#logger.error);
  }

  // ==========

  private async getCacheHeroStakeAmounts(...heroIds: number[]): Promise<[boolean, StakeAmountBig[]]> {
    if (heroIds.length === 0) {
      return [false, []];
    }

    const keys = heroIds.map(heroId => this.getKey(heroId));
    const values = await this._dep.redis.readHashFields(REDIS_KEY, keys);
    // console.log(`Get cache for ${keys}: ${values}`);
    const result: StakeAmountBig[] = [];

    for (let i = 0; i < heroIds.length; i++) {
      if (values[i] == undefined) {
        // failed
        // this.#logger.info(`Get batch stake amount failed`);
        return [false, []];
      }
      // console.log(`Get for ${values[i]}`);
      const v: StakeAmountBig = deserializeStakeAmountBig(values[i]!);
      result.push(v);
    }

    // this.#logger.info(`Get batch stake amount success`);
    return [true, result];
  }

  private getKey(heroId: number): string {
    return `${heroId}-${this.name}`;
  }
}

function serializeStakeAmount(stakeAmount: StakeAmount) {
  return JSON.stringify(stakeAmount);
}

function serializeStakeAmountBig(stakeAmount: StakeAmountBig) {
  return JSON.stringify([stakeAmount[0].toString(), stakeAmount[1].toString()]);
}

function deserializeStakeAmountBig(str: string): StakeAmountBig {
  const arr = JSON.parse(str);
  return [BigNumber.from(arr[0]), BigNumber.from(arr[1])];
}

function chainNameToNumber(chain: string) {
  switch (chain.toUpperCase()) {
    case CHAIN_NAMES.BNB:
      return 0;
    case CHAIN_NAMES.POLYGON:
      return 1;
    default:
      throw new Error(`Unknown chain: ${chain}`);
  }
}

interface IHeroStakeStreamData {
  uid?: number; // present only when client-triggered via V4 refresh
  network: number;
  hero_id: number;
  stake_amount: number; // legacy
  stake_bcoin: number;
  stake_sen: number;
}