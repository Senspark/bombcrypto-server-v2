import cors from 'cors';
import express, {Express, NextFunction, Request, Response, Router} from 'express';
import 'express-async-errors';
import path from 'path';
import {IBlockchainApi} from './IBlockchainApi';
import BlockchainApi from "./apis/BlockchainApi";
import {warmUp} from "./routes/SimpleHandlers";
import {errorHandlerMiddleware, getNetworkMiddleware} from "./routes/Middlewares";
import TokenHandlers from "./routes/TokenHandlers";
import {getHouseDetails, getHouseInfo, getHouseOwner} from "./routes/HouseHandlers";
import {DepositHandlers} from "./routes/DepositHandlers";
import IDependencies from "./services/IDependencies";
import HeroHandlers from "./routes/HeroHandlers";
import extendResponse from "./consts/ExpressExtension";
import {BlockchainCenterApi} from "./providers/BlockchainCenterApi";
import {createRequestLogger} from "./routes/RequestLogger";
import {V4HeroApi, V4HeroNetworkBindings} from "./apis/V4HeroApi";
import {V4HouseApi, V4HouseNetworkBindings} from "./apis/V4HouseApi";
import {V4DepositApi, V4DepositNetworkBindings} from "./apis/V4DepositApi";
import {V4HeroStakeRefreshApi, V4HeroStakeRefreshNetworkBindings} from "./apis/V4HeroStakeRefreshApi";
import {V4Handlers} from "./routes/V4Handlers";
import {HeroStakeApiCache} from "./database/HeroStakeApi.Cache";
import {HeroStakeApiDatabase} from "./database/HeroStakeApi.Database";
import {HeroStakeApiBlockchain} from "./contracts/HeroStakeApiBlockchain";

export type ApiMap = Map<string, IBlockchainApi>;

export interface IContextConfig {
  dependencies: IDependencies,
  apis: ApiMap;
}

export class App {
  constructor(
    private readonly _dep: IDependencies
  ) {
    this.express = express();
    this.router = express.Router();

    const {apis, heroNetworks, houseNetworks, depositNetworks, heroStakeRefreshNetworks} = this._buildBlockchainApis();
    this.contextConfig = {
      dependencies: this._dep,
      apis,
    };

    this.#heroHandlers = new HeroHandlers(_dep);
    this.#tokenHandlers = new TokenHandlers(_dep);
    this._depositHandlers = new DepositHandlers(_dep.messenger, _dep.logger.clone('[DEPOSIT]'), Array.from(apis.values()));
    this.#v4Handlers = new V4Handlers(
      new V4HeroApi(heroNetworks, _dep.redis, _dep.logger.clone('[V4][HERO]')),
      new V4HouseApi(houseNetworks, _dep.logger.clone('[V4][HOUSE]')),
      new V4DepositApi(depositNetworks, _dep.logger.clone('[V4][DEPOSIT]')),
      new V4HeroStakeRefreshApi(heroStakeRefreshNetworks, _dep.logger.clone('[V4][HERO-STAKE-REFRESH]')),
      _dep.messenger,
      _dep.logger.clone('[V4]'),
    );

    this.setup();
  }

  public readonly express: Express;
  public readonly router: Router;
  public readonly contextConfig: IContextConfig;
  readonly #heroHandlers: HeroHandlers;
  readonly #tokenHandlers: TokenHandlers;
  private readonly _depositHandlers: DepositHandlers;
  readonly #v4Handlers: V4Handlers;

  private setup() {
    this.express.get(`/`, warmUp);

    this.express.use(express.json());
    if (this._dep.envConfig.enableRequestLogging) {
      this.express.use(createRequestLogger(this._dep.logger));
    }

    this.router.use(getNetworkMiddleware.bind(this));
    this.router.use((req: Request, res: Response, next: NextFunction) => {
      extendResponse(this._dep.logger, res);
      next();
    });
    this.router.use(errorHandlerMiddleware);

    // Token handlers.
    this.router.get(`/total_coins`, this.#tokenHandlers.getTotalCoins.bind(this.#tokenHandlers));
    this.router.get(`/coins_price`, this.#tokenHandlers.getCoinsPrice.bind(this.#tokenHandlers));
    this.router.get(`/circulating_supply`, this.#tokenHandlers.getCirculatingSupply.bind(this.#tokenHandlers));
    this.router.get(`/coin_balance`, cors(), this.#tokenHandlers.getCoinBalance.bind((this.#tokenHandlers)));
    this.router.get(`/total_hero`, this.#tokenHandlers.getTotalHero.bind(this.#tokenHandlers));
    this.router.get(`/total_house`, this.#tokenHandlers.getTotalHouse.bind(this.#tokenHandlers));

    // Hero handlers.
    this.router.get(`/hero`, this.#heroHandlers.getHeroInfo.bind(this.#heroHandlers));
    this.router.get(`/hero_details`, this.#heroHandlers.getHeroDetails.bind(this.#heroHandlers));
    this.router.get(`/hero_owner`, this.#heroHandlers.getHeroOwner.bind(this.#heroHandlers));
    this.router.get(`/hero_stake`, this.#heroHandlers.getHeroStake.bind(this.#heroHandlers));
    this.router.get(`/hero_stake_v2`, this.#heroHandlers.getHeroStakeV2.bind(this.#heroHandlers));
    this.router.post(`/hero_decode`, this.#heroHandlers.handleDecodeHeroDetails.bind(this.#heroHandlers));
    this.router.post(`/hero_encode`, this.#heroHandlers.handleEncodeHeroDetails.bind(this.#heroHandlers));
    this.router.post(`/create_rock`, this.#heroHandlers.createRock.bind(this.#heroHandlers));
    this.router.get(`/query_rock_burn_tx`, this.#heroHandlers.queryRockBurnTx.bind(this.#heroHandlers));
    this.router.get(`/refresh_stake`, this.#heroHandlers.refreshStake.bind(this.#heroHandlers));

    // House handlers.
    this.router.get(`/house`, getHouseInfo);
    this.router.get(`/house_details`, getHouseDetails);
    this.router.get(`/house_owner`, getHouseOwner);

    // Deposit handlers.
    this.router.get(`/v3/deposited`, this._depositHandlers.getDepositedV3.bind(this._depositHandlers));

    // V4 fire-and-forget sync handlers (respond immediately, publish to Redis stream in the background).
    this.router.get(`/v4/hero`, this.#v4Handlers.getHeroInfoV4);
    this.router.get(`/v4/house`, this.#v4Handlers.getHouseInfoV4);
    this.router.get(`/v4/deposited`, this.#v4Handlers.getDepositedV4);
    this.router.post(`/v4/hero-stake/refresh`, this.#v4Handlers.refreshHeroStakeV4);

    this.express.use(`/`, this.router);
    this.express.use(`/blockchain`, this.router);

    // Backward.
    this.express.use(path.join(`/polygon`), this.router);
    this.express.use(path.join(`/polygon`, `blockchain`), this.router);
  }

  private _buildBlockchainApis(): {
    apis: ApiMap;
    heroNetworks: Map<string, V4HeroNetworkBindings>;
    houseNetworks: Map<string, V4HouseNetworkBindings>;
    depositNetworks: Map<string, V4DepositNetworkBindings>;
    heroStakeRefreshNetworks: Map<string, V4HeroStakeRefreshNetworkBindings>;
  } {
    const apis = new Map<string, IBlockchainApi>();
    const heroNetworks = new Map<string, V4HeroNetworkBindings>();
    const houseNetworks = new Map<string, V4HouseNetworkBindings>();
    const depositNetworks = new Map<string, V4DepositNetworkBindings>();
    const heroStakeRefreshNetworks = new Map<string, V4HeroStakeRefreshNetworkBindings>();

    this._dep.envConfig.blockchainConfigs.forEach((config, network) => {
      const centerApi = new BlockchainCenterApi(this._dep.envConfig.blockchainCenterApi, `[${network}]`);
      apis.set(network, new BlockchainApi(this._dep, config, centerApi));

      const heroStakeApi = new HeroStakeApiCache(this._dep, new HeroStakeApiDatabase(this._dep, network));
      const stakeBlockchainApi = new HeroStakeApiBlockchain(heroStakeApi, config, `[${network}][V4-REFRESH]`, centerApi);
      heroNetworks.set(network, {config, centerApi, heroStakeApi});
      houseNetworks.set(network, {config, centerApi});
      depositNetworks.set(network, {config, centerApi});
      heroStakeRefreshNetworks.set(network, {
        config,
        centerApi,
        stakeBlockchainApi,
        stakeStorageApi: heroStakeApi,
      });
    });

    return {apis, heroNetworks, houseNetworks, depositNetworks, heroStakeRefreshNetworks};
  }
}
