import {Request, Response} from "express";
import * as ErrorCode from "../ErrorCode";
import {StreamKeys} from "../cache/CachedKeys";
import {V4HeroApi} from "../apis/V4HeroApi";
import {V4HouseApi} from "../apis/V4HouseApi";
import {V4DepositApi} from "../apis/V4DepositApi";
import {V4HeroStakeRefreshApi} from "../apis/V4HeroStakeRefreshApi";
import {networkToStreamType, normalizeNetwork} from "../BlockchainConfig";
import IMessengerService from "../cache/IMessengerService";
import ILogger from "../services/ILogger";

interface ParsedQuery {
  address: string;
  network: string;
  uid: number;
  forceFresh: boolean;
}

/**
 * V4Handlers — internal sync endpoints used by the Kotlin game server.
 *
 * All three endpoints share the same fire-and-forget shape:
 *   1. Validate required query params + that the network is supported.
 *   2. Respond 200 immediately so the caller doesn't block on the RPC.
 *   3. Do the work via the typed V4 API.
 *   4. On success → publish to the corresponding Redis stream.
 *   5. On failure → log and do NOT publish. The Kotlin side relies on the
 *      absence of a stream message as the failure signal — silently publishing
 *      an empty array caused hero wipes on the old V1/V3 path (see
 *      BlockchainHeroDatabase.query in the server repo).
 */
export class V4Handlers {
  constructor(
    private readonly _heroApi: V4HeroApi,
    private readonly _houseApi: V4HouseApi,
    private readonly _depositApi: V4DepositApi,
    private readonly _heroStakeRefreshApi: V4HeroStakeRefreshApi,
    private readonly _messenger: IMessengerService,
    private readonly _logger: ILogger,
  ) {}

  getHeroInfoV4 = async (req: Request, res: Response): Promise<void> => {
    const parsed = this._parseQuery(req, res, this._heroApi, 'hero');
    if (!parsed) return;
    const {address, network, uid, forceFresh} = parsed;

    res.send({code: ErrorCode.CODE_OK});

    this._heroApi.fetchHeroes(network, address, {bypassCache: forceFresh})
      .then(heroes => {
        this._messenger.send(StreamKeys.AP_BL_SYNC_HERO, {
          uid, type: networkToStreamType(network), data: heroes, forceFresh,
        });
        this._logger.infos(`[v4/hero] uid=${uid} network=${network} published heroes=${heroes.length}`);
      })
      .catch(e => this._logger.errors(`[v4/hero] uid=${uid}`, e));
  };

  getHouseInfoV4 = async (req: Request, res: Response): Promise<void> => {
    const parsed = this._parseQuery(req, res, this._houseApi, 'house');
    if (!parsed) return;
    const {address, network, uid} = parsed;

    res.send({code: ErrorCode.CODE_OK});

    this._houseApi.fetchHouses(network, address)
      .then(houses => {
        this._messenger.send(StreamKeys.AP_BL_SYNC_HOUSE, {
          uid, type: networkToStreamType(network), data: houses,
        });
        this._logger.infos(`[v4/house] uid=${uid} network=${network} published houses=${houses.length}`);
      })
      .catch(e => this._logger.errors(`[v4/house] uid=${uid}`, e));
  };

  getDepositedV4 = async (req: Request, res: Response): Promise<void> => {
    const parsed = this._parseQuery(req, res, this._depositApi, 'deposited');
    if (!parsed) return;
    const {address, network, uid} = parsed;

    res.send({code: ErrorCode.CODE_OK});

    this._depositApi.fetchDeposited(network, address)
      .then(deposited => {
        this._messenger.send(StreamKeys.AP_BL_SYNC_DEPOSIT, {
          uid, type: networkToStreamType(network), data: deposited,
        });
        this._logger.infos(`[v4/deposited] uid=${uid} network=${network} published items=${deposited.length}`);
      })
      .catch(e => this._logger.errors(`[v4/deposited] uid=${uid}`, e));
  };

  /**
   * Client-triggered refresh of a single hero's stake after a stake/unstake tx.
   * Fire-and-forget: validate, ack with code:0, do the on-chain read async,
   * then publish to AP_BL_HERO_STAKE_STR (carrying uid so the consumer can
   * push live to the originating user). On failure: log and skip the
   * broadcast — the scheduler is the failsafe.
   */
  refreshHeroStakeV4 = async (req: Request, res: Response): Promise<void> => {
    const body = req.body ?? {};
    const uidRaw = body.uid;
    const heroIdRaw = body.heroId;
    const networkRaw = body.network;
    const txHash = body.txHash;

    const uid = typeof uidRaw === 'number' ? uidRaw : parseInt(String(uidRaw ?? ''), 10);
    const heroId = typeof heroIdRaw === 'number' ? heroIdRaw : parseInt(String(heroIdRaw ?? ''), 10);
    const networkRawStr = typeof networkRaw === 'string' ? networkRaw : '';
    const network = normalizeNetwork(networkRawStr);

    if (!uid || Number.isNaN(uid)) {
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: 'uid required'});
      return;
    }
    if (!heroId || Number.isNaN(heroId)) {
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: 'heroId required'});
      return;
    }
    if (!networkRawStr) {
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: 'network required'});
      return;
    }
    if (!network || !this._heroStakeRefreshApi.supportsNetwork(network)) {
      this._logger.errors(`[v4/hero-stake/refresh] uid=${uid} unknown network=${networkRawStr}`);
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: `unknown network: ${networkRawStr}`});
      return;
    }
    if (typeof txHash !== 'string' || !/^0x[0-9a-fA-F]{64}$/.test(txHash)) {
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: 'txHash required (0x + 64 hex)'});
      return;
    }

    res.send({code: ErrorCode.CODE_OK});

    this._heroStakeRefreshApi.refreshStake(network, uid, heroId, txHash)
      .then(() => this._logger.infos(`[v4/hero-stake/refresh] uid=${uid} network=${network} hero=${heroId} broadcasted`))
      .catch(e => this._logger.errors(`[v4/hero-stake/refresh] uid=${uid} hero=${heroId}`, e));
  };

  private _parseQuery(
    req: Request,
    res: Response,
    api: {supportsNetwork(network: string): boolean},
    route: string,
  ): ParsedQuery | null {
    const query = req.query;
    const address = query.address as string;
    const networkRaw = query.network as string | undefined;
    const network = normalizeNetwork(networkRaw);
    const uidRaw = query.uid as string | undefined;
    const uid = uidRaw ? parseInt(uidRaw, 10) : NaN;

    if (!uid || Number.isNaN(uid)) {
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: 'uid required'});
      return null;
    }
    if (!address) {
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: 'address required'});
      return null;
    }
    if (!networkRaw) {
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: 'network required'});
      return null;
    }
    if (!network || !api.supportsNetwork(network)) {
      this._logger.errors(`[v4/${route}] uid=${uid} unknown network=${networkRaw}`);
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: `unknown network: ${networkRaw}`});
      return null;
    }
    const forceFreshRaw = (query.forceFresh as string | undefined)?.toLowerCase();
    const forceFresh = forceFreshRaw === '1' || forceFreshRaw === 'true';
    return {address, network, uid, forceFresh};
  }
}
