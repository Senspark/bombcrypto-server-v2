import cors from 'cors';
import express, {Express, NextFunction, Request, Response, Router} from 'express';
import 'express-async-errors';
import path from 'path';
import {ValidateApi} from './apis/ValidateApi';
import {SignatureService} from './services/SignatureService';
import IDependencies from './services/IDependencies';
import JwtService from './services/JwtService';
import {warmUp} from './routes/SimpleHandlers';
import {checkBearer, errorHandlerMiddleware, getNetworkMiddleware} from './routes/Middlewares';
import SignatureHandlers from './routes/SignatureHandlers';
import ValidateHandlers from './routes/ValidateHandlers';
import ClaimV4Handlers from './routes/ClaimV4Handlers';
import extendResponse from './consts/ExpressExtension';
import {createRequestLogger} from './routes/RequestLogger';
import {BlockchainCenterApi} from './providers/BlockchainCenterApi';
import IMessengerService from './cache/IMessengerService';
import MessengerService from './cache/MessengerService';

export type ValidateApiMap = Map<string, ValidateApi>;
export type SignatureServiceMap = Map<string, SignatureService>;

export interface IContextConfig {
  dependencies: IDependencies;
  jwtService: JwtService;
  validateApis: ValidateApiMap;
  signatureServices: SignatureServiceMap;
}

export class App {
  constructor(
    private readonly _dep: IDependencies
  ) {
    this.express = express();
    this.router = express.Router();
    this.contextConfig = this.createContextConfig();
    this.#signatureHandlers = new SignatureHandlers();
    this.#validateHandlers = new ValidateHandlers();
    this.#messenger = new MessengerService(this._dep.logger, this._dep.envConfig);
    this.#claimV4Handlers = new ClaimV4Handlers(this.#messenger, this._dep.logger);
    this.setup();
  }

  public readonly express: Express;
  public readonly router: Router;
  public readonly contextConfig: IContextConfig;
  readonly #signatureHandlers: SignatureHandlers;
  readonly #validateHandlers: ValidateHandlers;
  readonly #claimV4Handlers: ClaimV4Handlers;
  readonly #messenger: IMessengerService;

  private setup() {
    this.express.get(`/_ah/warmup`, warmUp);
    this.express.get(`/`, warmUp);

    this.express.use(cors());
    this.express.use(express.json());
    this.express.use(express.urlencoded({extended: true}));
    if (this._dep.envConfig.enableRequestLogging) {
      this.express.use(createRequestLogger(this._dep.logger));
    }

    this.router.use(getNetworkMiddleware.bind(this));
    this.router.use((req: Request, res: Response, next: NextFunction) => {
      extendResponse(this._dep.logger, res);
      next();
    });

    // Signature handlers (all require bearer).
    this.router.get(`/sign/claim-airdrop`, checkBearer.bind(this), this.#signatureHandlers.claimAirdrop.bind(this.#signatureHandlers));
    this.router.post(`/sign/claim-token`, checkBearer.bind(this), this.#signatureHandlers.claimToken.bind(this.#signatureHandlers));
    this.router.post(`/sign/upgrade-hero-shield`, checkBearer.bind(this), this.#signatureHandlers.upgradeHeroShield.bind(this.#signatureHandlers));

    // Validate handlers (public).
    this.router.get(`/validate/airdrop/user`, this.#validateHandlers.airdropUser.bind(this.#validateHandlers));
    this.router.get(`/validate/check-total-claim-token`, this.#validateHandlers.checkTotalClaimToken.bind(this.#validateHandlers));
    this.router.get(`/validate/check-hero1-balance`, this.#validateHandlers.checkHero1Balance.bind(this.#validateHandlers));
    this.router.post(`/validate/recover-address`, this.#validateHandlers.recoverAddress.bind(this.#validateHandlers));

    // V4 async claim flow — acks immediately, publishes result to Redis streams.
    this.router.get(`/v4/validate/check-total-claim-token`, this.#claimV4Handlers.checkTotalClaimV4.bind(this.#claimV4Handlers));
    this.router.post(`/v4/sign/claim-token`, checkBearer.bind(this), this.#claimV4Handlers.signClaimV4.bind(this.#claimV4Handlers));

    this.express.use(`/`, this.router);
    this.express.use(`/signature`, this.router);

    // Backward.
    this.express.use(path.join(`/polygon`), this.router);
    this.express.use(path.join(`/polygon`, `signature`), this.router);

    this.express.use(errorHandlerMiddleware);
  }

  private createContextConfig(): IContextConfig {
    const validateApis = new Map<string, ValidateApi>();
    const signatureServices = new Map<string, SignatureService>();
    this._dep.envConfig.blockchainConfigs.forEach((value, key) => {
      const center = new BlockchainCenterApi(
        this._dep.envConfig.blockchainCenterApi,
        `[${key}]`,
      );
      const api = new ValidateApi(this._dep, value, center);
      validateApis.set(key, api);
      signatureServices.set(key, new SignatureService(api));
    });
    return {
      dependencies: this._dep,
      jwtService: new JwtService(this._dep),
      validateApis,
      signatureServices,
    };
  }
}
