import {IBlockchainApi} from "../IBlockchainApi";
import * as ErrorCode from "../ErrorCode";
import {Request, Response} from "express";
import IDependencies from "../services/IDependencies";
import ILogger from "../services/ILogger";
import {CachedKeys} from "../cache/CachedKeys";

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
   */
  async getCoinsPrice(req: Request, res: Response) {
    let resultData = await this._getCoinsPriceFromApi();
    const isAllFetched = Object.values(resultData).every(v => v > 0);
    if (isAllFetched) {
      await this._dep.redis.set(CachedKeys.AP_BL_COINS_PRICE, JSON.stringify(resultData));
    } else {
      const cachedData = await this._getCoinsPriceFromCached();
      if (cachedData) {
        resultData = cachedData;
      }
    }
    res.sendSuccess(resultData);
  }

  async _getCoinsPriceFromCached(): Promise<ICoinsPrice | null> {
    try {
      const k = CachedKeys.AP_BL_COINS_PRICE;
      const cached = await this._dep.redis.get(k);
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
   * @example
   * {
   *  "polygon_sen": 0.00302789,
   *  "polygon_bcoin": 0.02098667,
   *  "bnb_bcoin": 0.01292032,
   *  "bnb_sen": 0.00235193
   * }
   */
  async _getCoinsPriceFromApi(): Promise<ICoinsPrice> {
    interface TokenData {
      market_data: {
        current_price: {
          usd: number
        }
      }
    }

    let resultData = {};

    type Tokens = [name: string, url: string];
    const tokens: Tokens[] = [
      ['bnb_bcoin', 'https://api.coingecko.com/api/v3/coins/bomber-coin'],
      ['bnb_sen', 'https://api.coingecko.com/api/v3/coins/senspark'],
      ['polygon_bcoin', 'https://api.coingecko.com/api/v3/coins/bombcrypto-coin'],
      ['polygon_sen', 'https://api.coingecko.com/api/v3/coins/senspark-matic'],
    ];

    await Promise.all(tokens.map(async ([name, url]) => {
      let price = 0;
      try {
        const response = await fetch(url);
        if (response.ok) {
          const parsedResponse: TokenData = (await response.json()) as TokenData;
          price = parsedResponse.market_data.current_price.usd;
        }
      } catch (e) {
        this.#logger.errors(`Failed to fetch token price for ${name}`, e);
        price = 0;
      }

      resultData[name] = price;
    }));

    return resultData as ICoinsPrice;
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
  bnb_bcoin: number;
  bnb_sen: number;
}