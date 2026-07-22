import {IBlockchainConfig} from "../BlockchainConfig";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";
import {IHeroStakeApi} from "../IHeroStakeApi";
import IRedisDatabase from "../cache/IRedisDatabase";
import {CachedKeys} from "../cache/CachedKeys";
import ILogger from "../services/ILogger";
import * as Utility from "../Utility";

export interface V4HeroData {
  id: number;
  details: string;
  stake_bcoin: number;
  stake_sen: number;
}

export interface V4HeroNetworkBindings {
  config: IBlockchainConfig;
  centerApi: BlockchainCenterApi;
  heroStakeApi: IHeroStakeApi;
}

const GET_TOKEN_DETAILS_BY_OWNER_ABI = [{
  inputs: [{internalType: 'address', name: 'owner', type: 'address'}],
  name: 'getTokenDetailsByOwner',
  outputs: [{internalType: 'uint256[]', name: '', type: 'uint256[]'}],
  stateMutability: 'view',
  type: 'function',
}];

/**
 * Short-window dedupe cache so a burst of repeated sync requests for the same
 * wallet doesn't flood ap-blockchain-center. 30s is long enough to absorb
 * client-side retry bursts; short enough that real on-chain changes propagate
 * to the game server within the next sync tick.
 */
const HERO_CACHE_SECONDS = 60 * 2; // 2 minutes

/**
 * V4HeroApi — internal-only hero sync for the SYNC_V4 stream.
 *
 * On upstream failure, {@link fetchHeroes} throws and the Redis cache is left
 * untouched. The caller (V4Handlers) skips publishing to the Redis stream when
 * this throws — silently publishing an empty array caused hero wipes on the
 * old V1/V3 path, see BlockchainHeroDatabase.query.
 *
 * Cache layout: single Redis hash at `CachedKeys.AP_BL_V4_HERO_DETAILS` with
 * field `${network}:${address}`, per-field TTL via HEXPIRE (Redis 7.4+).
 */
export class V4HeroApi {
  constructor(
    private readonly _networks: Map<string, V4HeroNetworkBindings>,
    private readonly _redis: IRedisDatabase,
    private readonly _logger: ILogger,
  ) {}

  supportsNetwork(network: string): boolean {
    return this._networks.has(network);
  }

  async fetchHeroes(network: string, address: string, opts: {bypassCache?: boolean} = {}): Promise<V4HeroData[]> {
    const binding = this._networks.get(network);
    if (!binding) {
      throw new Error(`V4HeroApi: unknown network ${network}`);
    }

    const field = `${network}:${address}`;
    if (!opts.bypassCache) {
      const cached = await this._readCache(field);
      if (cached) {
        return cached;
      }
    }

    const details = await this._fetchDetailsByOwner(binding, address);
    const live = details.filter(d => d !== '0');
    if (live.length === 0) {
      const empty: V4HeroData[] = [];
      await this._writeCache(field, empty);
      return empty;
    }

    const decoded = live.map(d => ({id: Utility.decodeHeroDetails(d).id, details: d}));
    const stakes = await binding.heroStakeApi.getStakedAmounts(...decoded.map(d => d.id));
    const heroes: V4HeroData[] = decoded.map(({id, details}, i) => ({
      id,
      details,
      stake_bcoin: stakes[i]?.[0] ?? 0,
      stake_sen: stakes[i]?.[1] ?? 0,
    }));
    await this._writeCache(field, heroes);
    return heroes;
  }

  private async _fetchDetailsByOwner(binding: V4HeroNetworkBindings, owner: string): Promise<string[]> {
    const result = await binding.centerApi.callContract(
      binding.config.name,
      binding.config.bheroTokenAddress,
      GET_TOKEN_DETAILS_BY_OWNER_ABI,
      'getTokenDetailsByOwner',
      [owner],
    );
    return (result as string[]).map(e => e.toString());
  }

  private async _readCache(field: string): Promise<V4HeroData[] | null> {
    const [raw] = await this._redis.readHashFields(CachedKeys.AP_BL_V4_HERO_DETAILS, [field]);
    if (!raw) {
      return null;
    }
    try {
      const parsed = JSON.parse(raw) as V4HeroData[];
      if (!Array.isArray(parsed)) {
        await this._redis.removeFromHash(CachedKeys.AP_BL_V4_HERO_DETAILS, [field]);
        return null;
      }
      return parsed;
    } catch (e) {
      this._logger.errors(`[V4HeroApi] corrupt cache field ${field}, evicting`, e);
      await this._redis.removeFromHash(CachedKeys.AP_BL_V4_HERO_DETAILS, [field]);
      return null;
    }
  }

  private async _writeCache(field: string, heroes: V4HeroData[]): Promise<void> {
    await this._redis.setHashFieldWithTTL(
      CachedKeys.AP_BL_V4_HERO_DETAILS,
      field,
      JSON.stringify(heroes),
      HERO_CACHE_SECONDS,
    );
  }
}
