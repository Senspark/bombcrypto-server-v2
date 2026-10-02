import {BigNumber} from 'ethers';
import {INativeRateMessage, StreamKeys} from './consts/Messages';
import {normalizeNetwork} from './KeeperConfig';
import {NetworkKeeper} from './RateKeeper';
import ILogger from './services/ILogger';
import IMessengerService from './services/IMessengerService';

/**
 * The Redis-stream face of this service: SV_NATIVE_RATE_STR in, nothing out. The game server does not
 * wait on us — a missed message is simply replaced by the next tick's.
 */
export class StreamHandlers {
  private readonly _logger: ILogger;

  constructor(
    logger: ILogger,
    private readonly _keepers: Map<string, NetworkKeeper>,
    private readonly _messenger: IMessengerService,
    private readonly _maxMessageAgeMs: number,
  ) {
    this._logger = logger.clone('[STREAM]');
  }

  register(): void {
    this._messenger.listen(StreamKeys.SV_NATIVE_RATE_STR, (msg: INativeRateMessage) => this.handle(msg));
  }

  handle(msg: INativeRateMessage): void {
    const network = normalizeNetwork(msg?.network);
    const keeper = network ? this._keepers.get(network) : undefined;
    if (!keeper) {
      this._logger.warn(`ignoring message for unsupported network: ${JSON.stringify(msg)}`);
      return;
    }

    let rateWei: BigNumber;
    try {
      rateWei = BigNumber.from(msg.rateWei);
    } catch (e) {
      this._logger.errors(`undecodable rateWei: ${JSON.stringify(msg)}`, e);
      return;
    }
    if (rateWei.lte(0)) {
      this._logger.warn(`ignoring non-positive rate: ${JSON.stringify(msg)}`);
      return;
    }

    const ageMs = Date.now() - Number(msg.at ?? 0);
    if (!Number.isFinite(ageMs) || ageMs > this._maxMessageAgeMs) {
      this._logger.warn(`ignoring stale rate (${Math.round(ageMs / 60000)} min old): ${JSON.stringify(msg)}`);
      return;
    }

    this._logger.info(`<- ${network} nativePerBcoin=${msg.nativePerBcoin} rateWei=${msg.rateWei}`);
    keeper.submit(rateWei);
  }
}
