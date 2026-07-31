import cors from 'cors';
import express, {Express, NextFunction, Request, Response} from 'express';
import 'express-async-errors';
import {BlockchainCenterApi} from './BlockchainCenterApi';
import {IConfig} from './Config';
import MessengerService from './cache/MessengerService';
import {normalizeNetwork} from './NativeConfig';
import {NativeService} from './NativeService';
import ILogger from './services/ILogger';
import {StreamHandlers} from './StreamHandlers';

// Business error whose message is safe to return to the caller (still a canned string — never raw).
class BadRequest extends Error {
  constructor(message: string) {
    super(message);
  }
}

export class App {
  public readonly express: Express;
  private readonly _service: NativeService;
  private readonly _logger: ILogger;
  private readonly _httpLogger: ILogger;

  constructor(
    private readonly _config: IConfig,
    logger: ILogger,
  ) {
    this._logger = logger;
    this._httpLogger = logger.clone('[HTTP]');
    this.express = express();
    this._service = new NativeService(
      _config.networks,
      new BlockchainCenterApi(_config.blockchainCenterApi, logger),
      _config.privateKey,
      logger,
    );
    this.setup();
  }

  /**
   * The game server talks to this service over Redis streams only. The HTTP routes below stay for
   * operators — a `curl` against /native/counters is how you tell "the signer is slow" apart from
   * "the stream never delivered".
   */
  startStreamListener(): void {
    const messenger = new MessengerService(this._logger, this._config.redisConnectionString);
    new StreamHandlers(this._logger, this._service, messenger).register();
  }

  private setup() {
    this.express.use(cors());
    this.express.use(express.json());
    this.express.use(express.urlencoded({extended: true}));

    this.express.get('/', (_req, res) => res.send('ap-deposit-native'));
    this.express.get('/health', (_req, res) => res.json({ok: true}));

    // Internal service (not public-facing). The withdraw signature is bounded-harmless: it authorizes
    // only its `user` (the withdraw is msg.sender-bound) up to that user's own on-chain deposit, which
    // the contract re-checks (`allowedCumulative <= deposited[user]`). So no bearer is required.
    this.express.get('/native/counters', this.counters.bind(this));
    this.express.post('/native/withdraw-sign', this.withdrawSign.bind(this));

    this.express.use(this.errorHandler.bind(this));
  }

  private async counters(req: Request, res: Response) {
    const startedAt = Date.now();
    const network = normalizeNetwork(req.query.network as string);
    const userAddress = req.query.userAddress as string;
    if (!network) throw new BadRequest('Unsupported network');
    if (!userAddress) throw new BadRequest('userAddress is required');
    const data = await this._service.getCounters(network, userAddress);
    this._httpLogger.info(
      `GET /native/counters ${network} ${userAddress} -> deposited=${data.deposited} withdrawn=${data.withdrawn} took=${Date.now() - startedAt}ms`,
    );
    res.json({code: 0, message: data});
  }

  private async withdrawSign(req: Request, res: Response) {
    const startedAt = Date.now();
    const network = normalizeNetwork(req.body.network);
    const userAddress = req.body.userAddress as string;
    const allowedCumulative = req.body.allowedCumulative;
    if (!network) throw new BadRequest('Unsupported network');
    if (!userAddress) throw new BadRequest('userAddress is required');
    if (allowedCumulative === undefined || allowedCumulative === null) {
      throw new BadRequest('allowedCumulative is required');
    }
    const data = await this._service.signWithdraw(network, userAddress, allowedCumulative.toString());
    this._httpLogger.info(
      `POST /native/withdraw-sign ${network} ${userAddress} allowed=${allowedCumulative} -> deadline=${data.deadline} took=${Date.now() - startedAt}ms`,
    );
    res.json({code: 0, message: data});
  }

  // Log the raw cause server-side; return only a canned message + non-zero code to the caller.
  private errorHandler(err: Error, req: Request, res: Response, _next: NextFunction) {
    if (err instanceof BadRequest) {
      this._httpLogger.warn(`${req.method} ${req.path} rejected: ${err.message}`);
      res.status(400).json({code: 1, message: err.message});
      return;
    }
    this._httpLogger.errors(`${req.method} ${req.path} failed`, err);
    res.status(500).json({code: 1, message: 'Unable to process the request right now.'});
  }
}
