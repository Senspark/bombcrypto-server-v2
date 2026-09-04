import {IBlockchainApi} from "../IBlockchainApi";
import * as ErrorCode from "../ErrorCode";
import {Request, Response} from "express";
import IDependencies from "../services/IDependencies";
import ILogger from "../services/ILogger";
import {CachedKeys} from "../cache/CachedKeys";

// How long a fetched price set answers further requests before going upstream again. Shorter than any
// consumer's own refresh interval, so a scheduler never reads a price older than its own tick.
const COINS_PRICE_TTL_SECONDS = 60;

export default class TokenHandlers {
  constructor(
    private readonly _dep: IDependencies
  ) {
    this.#logger = _dep.logger.clone('[TOKEN]');
  }

  readonly #logger: ILogger;

  async getTotalCoins(req: Request, res: Response) {
    const locals = res.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    let result = await blockchainApi.getTotalCoins();
    const contracts = [
      blockchainApi.config.bcoinTokenAddress,
    ];
    await Promise.all(contracts.map(async address => {
      const value = await blockchainApi.getCoinBalance(address);
      result -= value;
    }));
    res.send({
      code: ErrorCode.CODE_OK,
      message: result
    });
  }
  
  //region getCoinsPrice
  /**
   * Lưu trữ lại giá coin gần nhất vào cache
   * Trong trường hợp fetch error thì sẽ lấy từ cache
   *
   * Two caches, two jobs: FRESH is a short TTL window that collapses the calls arriving from every
   * zone's price scheduler into one upstream fetch, LAST_GOOD never expires and is what answers when
   * CoinGecko is down. Without the window each caller was its own fetch, and the native rate job made
   * that worse — see the call budget in the README.
   */
  async getCoinsPrice(req: Request, res: Response) {
    const fresh = await this._getCoinsPriceFromCached(CachedKeys.AP_BL_COINS_PRICE_FRESH);
    if (fresh) {
      res.sendSuccess(fresh);
      return;
    }

    let resultData = await this._getCoinsPriceFromApi();
    const isAllFetched = Object.values(resultData).every(v => v > 0);
    if (isAllFetched) {
      const payload = JSON.stringify(resultData);
      await this._dep.redis.set(CachedKeys.AP_BL_COINS_PRICE, payload);
      await this._dep.redis.setWithTTL(CachedKeys.AP_BL_COINS_PRICE_FRESH, payload, COINS_PRICE_TTL_SECONDS);
    } else {
      const cachedData = await this._getCoinsPriceFromCached(CachedKeys.AP_BL_COINS_PRICE);
      if (cachedData) {
        resultData = cachedData;
      }
    }
    res.sendSuccess(resultData);
  }

  async _getCoinsPriceFromCached(key: string = CachedKeys.AP_BL_COINS_PRICE): Promise<ICoinsPrice | null> {
    try {
      const cached = await this._dep.redis.get(key);
      if (cached) {
        return JSON.parse(cached);
      }
      return null;
    } catch (e) {
      this.#logger.errors(`Failed to fetch token price from cache`, e);
      return null;
    }
  }

  /**
   * One request for every coin instead of one per coin: the per-coin /coins/<id> endpoint cost four
   * upstream calls per caller, which does not fit CoinGecko's free monthly credits once a scheduler
   * polls it. /simple/price returns the same USD figures for all ids at once.
   *
   * @example
   * {
   *  "polygon_sen": 0.00302789,
   *  "polygon_bcoin": 0.02098667,
   *  "bnb_bcoin": 0.01292032,
   *  "bnb_sen": 0.00235193,
   *  "polygon_native": 0.092547,
   *  "bnb_native": 716.3
   * }
   */
  async _getCoinsPriceFromApi(): Promise<ICoinsPrice> {
    // field name -> CoinGecko id. `*_native` is the chain's own coin (POL / BNB), which is what the
    // native-priced sinks charge in; BCOIN and SEN are listed separately per chain because the two
    // bridged tokens do not trade at the same price.
    const ids: Record<keyof ICoinsPrice, string> = {
      bnb_bcoin: 'bomber-coin',
      bnb_sen: 'senspark',
      bnb_native: 'binancecoin',
      polygon_bcoin: 'bombcrypto-coin',
      polygon_sen: 'senspark-matic',
      polygon_native: 'polygon-ecosystem-token',
    };

    const resultData: Record<string, number> = {};
    Object.keys(ids).forEach(name => resultData[name] = 0);

    try {
      const url = `https://api.coingecko.com/api/v3/simple/price`
        + `?ids=${Object.values(ids).join(',')}&vs_currencies=usd`;
      const response = await fetch(url);
      if (response.ok) {
        const parsed = (await response.json()) as Record<string, { usd?: number }>;
        Object.entries(ids).forEach(([name, id]) => {
          resultData[name] = parsed[id]?.usd ?? 0;
        });
      } else {
        this.#logger.errors(`Failed to fetch token prices`, new Error(`HTTP ${response.status}`));
      }
    } catch (e) {
      this.#logger.errors(`Failed to fetch token prices`, e);
    }

    return resultData as unknown as ICoinsPrice;
  }
  //endregion

  async getCirculatingSupply(req: Request, res: Response) {
    const locals = res.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    let result = await blockchainApi.getTotalCoins();
    const contracts = blockchainApi.config.lockedAddresses;
    await Promise.all(contracts.map(async address => {
      const value = await blockchainApi.getCoinBalance(address);
      result -= value;
    }));
    res.send({
      code: ErrorCode.CODE_OK,
      message: result
    });
  }

  async getCoinBalance(request: Request, response: Response) {
    const locals = response.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    const query = request.query;
    const address = query.address as string;
    const result = await blockchainApi.getCoinBalance(address);
    response.send({
      code: ErrorCode.CODE_OK,
      message: result,
    });
  }

  async getTotalHero(req: Request, res: Response) {
    const locals = res.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    const result = await blockchainApi.getHeroTokenIdCounter();
    res.send({
      code: ErrorCode.CODE_OK,
      message: result,
    });
  }

  async getTotalHouse(req: Request, res: Response) {
    const locals = res.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    const result = await blockchainApi.getHouseTokenIdCounter();
    res.send({
      code: ErrorCode.CODE_OK,
      message: result,
    });
  }
}

interface ICoinsPrice {
  polygon_sen: number;
  polygon_bcoin: number;
  polygon_native: number;
  bnb_bcoin: number;
  bnb_sen: number;
  bnb_native: number;
}