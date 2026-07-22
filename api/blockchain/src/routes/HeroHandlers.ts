import {IBlockchainApi} from "../IBlockchainApi";
import * as ErrorCode from "../ErrorCode";
import {Request, Response} from "express";
import {IDecodedDetails} from "../IDecodedDetails";
import {decodeHeroDetails, encodeHeroDetails} from "../Utility";
import IDependencies from "../services/IDependencies";
import ILogger from "../services/ILogger";
import {CreateRockRequest} from "../contracts/HeroSApi";
import {ServerError, ValidationError} from "../consts/ServerError";
import IMessengerService from "../cache/IMessengerService";
import {CachedKeys, StreamKeys} from "../cache/CachedKeys";

export default class HeroHandlers {
  constructor(
    deps: IDependencies
  ) {
    this.logger = deps.logger.clone('[HERO]');
    this.messenger = deps.messenger;
  }

  private readonly logger: ILogger;
  private readonly messenger: IMessengerService;

  async getHeroInfo(req: Request, res: Response) {
    const locals = res.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    const query = req.query;
    const address = query.address;
    const id = query.id;
    const details = query.details;
    const clearCache = query.clear != undefined && query.clear == "true";
    const mode = parseInt(query.mode as string ?? 0);
    const userId = query.uid && parseInt(query.uid as string) as number;
    
    const result = await blockchainApi.getDecodedHeroDetails({
      address, id, details, mode, clearCache
    });
    res.send({
      code: ErrorCode.CODE_OK,
      message: result,
    });

    if (userId) {
      this.messenger.send(StreamKeys.AP_BL_SYNC_HERO, {
        uid: userId,
        type: (query.network as string)?.toUpperCase(),
        data: result,
      }).catch(e => {
        this.logger.error(e);
      });
    }
  }

  async getHeroDetails(req: Request, res: Response) {
    const locals = res.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    const query = req.query;
    const address = query.address;
    const id = query.id !== undefined ? parseInt(query.id as string) : undefined;
    const clearCache = query.clear != undefined && query.clear == "true";
    const result = await blockchainApi.getHeroDetails({
      address, id, clearCache
    });
    res.send({
      code: ErrorCode.CODE_OK,
      message: result,
    });
  }

  async getHeroOwner(req: Request, res: Response) {
    const locals = res.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    const query = req.query;
    const id = parseInt(query.id as string);
    const result = await blockchainApi.getHeroOwner(id);
    res.send({
      code: ErrorCode.CODE_OK,
      message: result,
    });
  }

  async getHeroStake(req: Request, res: Response) {
    const locals = res.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    const query = req.query;
    const id = parseInt(query.id as string);
    if (!id || isNaN(id)) {
      res.send({
        code: ErrorCode.CODE_INTERNAL,
        message: 'Missing hero id',
      });
    }
    const result = await blockchainApi.getHeroStakedAmount(id);
    res.send({
      code: ErrorCode.CODE_OK,
      message: result.length > 0 ? result[0] : 0,
    });
  }

  async getHeroStakeV2(req: Request, res: Response) {
    const locals = res.locals;
    const blockchainApi: IBlockchainApi = locals.api;
    const query = req.query;
    const id = parseInt(query.id as string);
    const result = await blockchainApi.getHeroStakedAmount(id);
    res.send({
      code: ErrorCode.CODE_OK,
      message: {
        stake_bcoin: result.length > 0 ? result[0] : 0,
        stake_sen: result.length > 1 ? result[1] : 0,
      },
    });
  }

  async handleDecodeHeroDetails(req: Request, res: Response) {
    try {
      const body = req.body;
      const details = body.details;
      const result = decodeHeroDetails(details);
      res.send({
        code: ErrorCode.CODE_OK,
        message: result,
      });
    } catch (e) {
      this.logger.info(e);
      res.send({
        code: ErrorCode.CODE_INTERNAL,
        message: e ?? 'ERROR',
      });
    }
  }

  async handleEncodeHeroDetails(req: Request, res: Response) {
    try {
      const details: IDecodedDetails = req.body;
      const result = encodeHeroDetails(details);
      res.send({
        code: ErrorCode.CODE_OK,
        message: result,
      });
    } catch (e) {
      this.logger.info(e);
      res.send({
        code: ErrorCode.CODE_INTERNAL,
        message: e ?? 'ERROR',
      });
    }
  }

  async createRock(req: Request, res: Response) {
    interface ICreateRockResponse {
      confirmed: boolean;
      errorMessage: string;
    }

    try {
      const data: CreateRockRequest = req.body;
      this.logger.assert(data, 'Invalid data');
      this.logger.assert(data.tx && data.tx.length === 66, 'Invalid address');
      this.logger.assert(data.wallet_address && data.wallet_address.length === 42, 'Invalid address');
      this.logger.assert(data.hero_ids && data.hero_ids.length > 0, 'Invalid hero ids');
      this.logger.assert(Array.isArray(data.hero_ids) && data.hero_ids.every(Number.isFinite), 'Invalid hero ids');

      const locals = res.locals;
      const blockchainApi: IBlockchainApi = locals.api;
      const result = await blockchainApi.heroSApi.verifyCreateRock(data);
      const responseData: ICreateRockResponse = {
        confirmed: result,
        errorMessage: ''
      };
      res.sendSuccess(responseData);
    } catch (e) {
      this.logger.error(e);
      if (e instanceof ValidationError) {
        const responseData: ICreateRockResponse = {
          confirmed: false,
          errorMessage: e.message
        };
        res.sendSuccess(responseData);
      } else if (e instanceof ServerError) {
        res.sendError(e.message);
      } else {
        res.sendGenericError();
      }
    }
  }

  async queryRockBurnTx(req: Request, res: Response) {
    try {
      const tx = req.query.tx as string;
      this.logger.assert(tx, 'Invalid tx');
      const locals = res.locals;
      const blockchainApi: IBlockchainApi = locals.api;
      const result = await blockchainApi.heroSApi.getBurnedHeroData(tx);
      res.sendSuccess(result);
    } catch (e) {
      if (e instanceof ServerError) {
        res.sendError(e.message);
      } else {
        this.logger.error(e);
        res.sendGenericError();
      }
    }
  }

  async refreshStake(req: Request, res: Response) {
    try {
      const locals = res.locals;
      const blockchainApi: IBlockchainApi = locals.api;
      const query = req.query;

      const heroId = parseInt(query.id as string);
      this.logger.assert(heroId, 'Invalid hero id');

      // Optional: when present, the game server fast-paths the stake push to this user.
      const uidRaw = query.uid as string | undefined;
      const uid = uidRaw ? parseInt(uidRaw) : undefined;

      const result = await blockchainApi.refreshHeroStakedAmount(heroId, Number.isNaN(uid as number) ? undefined : uid);
      res.send({
        code: ErrorCode.CODE_OK,
        message: {
          stake_bcoin: result.length > 0 ? result[0] : 0,
          stake_sen: result.length > 1 ? result[1] : 0,
        },
      });
    } catch (e) {
      this.logger.info(e);
      res.send({
        code: ErrorCode.CODE_INTERNAL,
        message: e ?? 'ERROR',
      });
    }
  }
}