import {
  CODE_ERROR,
  CODE_OK,
  INativeRequest,
  INativeResult,
  RequestKind,
  SAFE_ERROR,
  StreamKeys,
} from './consts/Messages';
import {normalizeNetwork} from './NativeConfig';
import {NativeService} from './NativeService';
import ILogger from './services/ILogger';
import IMessengerService from './services/IMessengerService';

/**
 * The Redis-stream face of this service: one request in on SV_DEPNATIVE_REQUEST_STR, one result out on
 * AP_DEPNATIVE_RESULT_STR, correlated by `correlationId`.
 *
 * Every path must answer. The game server is awaiting that correlationId with a timeout, so a silent
 * drop costs it the full wait; a fast error reply lets it fail immediately with a reason.
 */
export class StreamHandlers {
  private readonly _logger: ILogger;

  constructor(
    logger: ILogger,
    private readonly _service: NativeService,
    private readonly _messenger: IMessengerService,
  ) {
    this._logger = logger.clone('[STREAM]');
  }

  register(): void {
    this._messenger.listen(StreamKeys.SV_DEPNATIVE_REQUEST_STR, (msg: INativeRequest) => {
      this.handle(msg).catch((e) => this._logger.errors('unhandled dispatch failure', e));
    });
  }

  private async handle(req: INativeRequest): Promise<void> {
    const startedAt = Date.now();
    const cid = req?.correlationId;
    if (!cid) {
      // Nothing to reply to — log the whole payload, it is the only trace that will exist.
      this._logger.errors('request without correlationId, dropping', JSON.stringify(req));
      return;
    }

    const tag = `cid=${cid} kind=${req.kind} uid=${req.uid} net=${req.network}`;
    this._logger.info(`<- ${tag} wallet=${req.wallet}`);

    const network = normalizeNetwork(req.network);
    if (!network) {
      await this.reply(cid, {code: CODE_ERROR, errorMessage: 'Unsupported network'}, tag, startedAt);
      return;
    }
    if (!req.wallet) {
      await this.reply(cid, {code: CODE_ERROR, errorMessage: 'Missing wallet'}, tag, startedAt);
      return;
    }

    try {
      switch (req.kind) {
        case RequestKind.COUNTERS: {
          const counters = await this._service.getCounters(network, req.wallet);
          this._logger.info(`   ${tag} counters deposited=${counters.deposited} withdrawn=${counters.withdrawn}`);
          await this.reply(cid, {code: CODE_OK, ...counters}, tag, startedAt);
          return;
        }
        case RequestKind.WITHDRAW_SIGN: {
          if (!req.allowedCumulative) {
            await this.reply(cid, {code: CODE_ERROR, errorMessage: 'Missing allowedCumulative'}, tag, startedAt);
            return;
          }
          const signed = await this._service.signWithdraw(network, req.wallet, req.allowedCumulative);
          this._logger.info(
            `   ${tag} signed allowed=${req.allowedCumulative} deadline=${signed.deadline} contract=${signed.contractAddress}`,
          );
          await this.reply(cid, {code: CODE_OK, ...signed}, tag, startedAt);
          return;
        }
        default:
          await this.reply(cid, {code: CODE_ERROR, errorMessage: 'Unsupported request'}, tag, startedAt);
          return;
      }
    } catch (e) {
      // The raw cause stays here; the game server only ever sees SAFE_ERROR.
      this._logger.errors(`!! ${tag} failed after ${Date.now() - startedAt}ms`, e);
      await this.reply(cid, {code: CODE_ERROR, errorMessage: SAFE_ERROR}, tag, startedAt);
    }
  }

  private async reply(
    correlationId: string,
    body: Omit<INativeResult, 'correlationId'>,
    tag: string,
    startedAt: number,
  ): Promise<void> {
    const tookMs = Date.now() - startedAt;
    const result: INativeResult = {correlationId, ...body};
    const sent = await this._messenger.send(StreamKeys.AP_DEPNATIVE_RESULT_STR, result);
    const outcome = body.code === CODE_OK ? 'ok' : `error "${body.errorMessage}"`;
    if (sent) {
      this._logger.info(`-> ${tag} ${outcome} took=${tookMs}ms`);
    } else {
      // The caller will now sit until its own timeout — worth its own line, not a silent false.
      this._logger.warn(`-> ${tag} ${outcome} took=${tookMs}ms BUT publish failed, caller will time out`);
    }
  }
}
