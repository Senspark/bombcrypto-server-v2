import {BigNumber, Contract, providers, utils, Wallet} from 'ethers';
import {KeeperNetworkConfig} from './KeeperConfig';
import ILogger from './services/ILogger';

const BHERO_ABI = ['function design() view returns (address)'];

// Only what this service touches. setNativeRate is not in the client's HeroDesign ABI; its selector
// (0x286bcc80) was confirmed in the deployed implementation, and it is gated by DESIGNER_ROLE.
const DESIGN_ABI = [
  'function getNativeRate() view returns (uint256)',
  'function setNativeRate(uint256 value)',
  'function DESIGNER_ROLE() view returns (bytes32)',
  'function hasRole(bytes32 role, address account) view returns (bool)',
];

// Below this the keeper cannot pay for even a few writes; worth a warning on every check.
const LOW_GAS_BALANCE = utils.parseEther('0.01');

export interface KeeperOptions {
  minChangePercent: number;
  maxChangePercent: number;
  dryRun: boolean;
}

export interface NetworkStatus {
  network: string;
  designAddress?: string;
  onChainRateWei?: string;
  lastTargetWei?: string;
  lastTxHash?: string;
  lastWriteAt?: number;
  lastError?: string;
}

/**
 * One per network. Writes are serialized: while a tx is in flight, newer targets overwrite each other
 * and only the latest one is applied afterwards, so a slow chain never queues up stale rates.
 */
export class NetworkKeeper {
  private readonly _logger: ILogger;
  private readonly _provider: providers.JsonRpcProvider;
  private readonly _wallet: Wallet;
  private _design?: Contract;
  private _busy = false;
  private _pending?: BigNumber;
  readonly status: NetworkStatus;

  constructor(
    private readonly _config: KeeperNetworkConfig,
    privateKey: string,
    private readonly _options: KeeperOptions,
    logger: ILogger,
  ) {
    this._logger = logger.clone(`[${_config.network}]`);
    this._provider = new providers.StaticJsonRpcProvider(_config.rpcUrl, _config.chainId);
    this._wallet = new Wallet(privateKey, this._provider);
    this.status = {network: _config.network};
  }

  /** Resolves HeroDesign and reports, once, anything that would make every write fail. */
  async init(): Promise<void> {
    const bhero = new Contract(this._config.bheroAddress, BHERO_ABI, this._provider);
    const designAddress: string = await bhero.design();
    this._design = new Contract(designAddress, DESIGN_ABI, this._wallet);
    this.status.designAddress = designAddress;

    const rate: BigNumber = await this._design.getNativeRate();
    this.status.onChainRateWei = rate.toString();
    this._logger.info(`design=${designAddress} keeper=${this._wallet.address} onChainRate=${utils.formatEther(rate)}`);

    try {
      const role: string = await this._design.DESIGNER_ROLE();
      const allowed: boolean = await this._design.hasRole(role, this._wallet.address);
      if (!allowed) {
        this._logger.warn(`keeper ${this._wallet.address} does NOT hold DESIGNER_ROLE — every setNativeRate will revert`);
      }
    } catch (e) {
      this._logger.errors('could not check DESIGNER_ROLE', e);
    }
    await this.warnIfLowGas();

    // A rate that arrived while HeroDesign was still being resolved was kept, not dropped.
    this.kick();
  }

  /**
   * Queue a new target rate; applied now if idle, otherwise right after the write in flight. Before
   * init() has resolved HeroDesign the target is only kept, and init() applies it.
   */
  submit(rateWei: BigNumber): void {
    this._pending = rateWei;
    this.kick();
  }

  private kick(): void {
    if (this._design && this._pending && !this._busy) {
      this.drain().catch((e) => this._logger.errors('drain failed', e));
    }
  }

  private async drain(): Promise<void> {
    this._busy = true;
    try {
      while (this._pending) {
        const target = this._pending;
        this._pending = undefined;
        try {
          await this.apply(target);
          this.status.lastError = undefined;
        } catch (e) {
          this.status.lastError = e instanceof Error ? e.message : String(e);
          this._logger.errors(`failed to apply rate ${utils.formatEther(target)}`, e);
        }
      }
    } finally {
      this._busy = false;
    }
  }

  private async apply(target: BigNumber): Promise<void> {
    const design = this._design;
    if (!design) {
      throw new Error('HeroDesign not resolved yet');
    }
    this.status.lastTargetWei = target.toString();

    // Always compare against the chain, not our last write: someone may have set it by hand.
    const current: BigNumber = await design.getNativeRate();
    this.status.onChainRateWei = current.toString();

    const changePercent = percentChange(current, target);
    if (current.gt(0) && changePercent < this._options.minChangePercent) {
      this._logger.info(
        `skip ${utils.formatEther(current)} -> ${utils.formatEther(target)} ` +
          `(${changePercent.toFixed(2)}%): below ${this._options.minChangePercent}%`,
      );
      return;
    }

    // Clamped, not refused: the game server already clamps each tick, so a bigger gap means the chain
    // fell behind (keeper down, rate set by hand) or a bad message. Stepping keeps a bad value from
    // landing whole, and a real gap still closes over the next ticks instead of blocking forever.
    let next = target;
    if (current.gt(0) && changePercent > this._options.maxChangePercent) {
      next = clampStep(current, target, this._options.maxChangePercent);
      this._logger.warn(
        `target ${utils.formatEther(target)} is ${changePercent.toFixed(2)}% from the chain; ` +
          `stepping to ${utils.formatEther(next)} (max ${this._options.maxChangePercent}%)`,
      );
    }
    const label = `${utils.formatEther(current)} -> ${utils.formatEther(next)} (${percentChange(current, next).toFixed(2)}%)`;
    if (this._options.dryRun) {
      this._logger.info(`DRY_RUN would set ${label}`);
      return;
    }

    // estimateGas first: a missing role or a paused contract fails here, before any gas is spent.
    await design.estimateGas.setNativeRate(next);
    const tx = await design.setNativeRate(next, await this.feeOverrides());
    this._logger.info(`sent ${label} tx=${tx.hash}`);
    // Bounded: a tx stuck in the mempool must not hold this network's queue forever. The next rate
    // that arrives re-reads the chain, so a tx that lands late is simply compared against.
    const receipt = await withTimeout<providers.TransactionReceipt>(tx.wait(1), TX_WAIT_TIMEOUT_MS, `tx ${tx.hash} not mined in time`);
    if (receipt.status !== 1) {
      throw new Error(`tx ${tx.hash} reverted`);
    }
    this.status.onChainRateWei = next.toString();
    this.status.lastTxHash = tx.hash;
    this.status.lastWriteAt = Date.now();
    this._logger.info(`applied ${label} tx=${tx.hash} block=${receipt.blockNumber}`);
    await this.warnIfLowGas();
  }

  /**
   * EIP-1559 fees from the node instead of ethers v5's defaults: those hardcode a 1.5 gwei tip, which
   * Polygon (minimum ~25-30 gwei) rejects or leaves pending forever. BSC answers with its own low tip.
   */
  private async feeOverrides(): Promise<{maxFeePerGas: BigNumber; maxPriorityFeePerGas: BigNumber} | {gasPrice: BigNumber}> {
    const block = await this._provider.getBlock('latest');
    const baseFee = block.baseFeePerGas;
    if (!baseFee) {
      return {gasPrice: await this._provider.getGasPrice()};
    }
    let tip: BigNumber;
    try {
      tip = BigNumber.from(await this._provider.send('eth_maxPriorityFeePerGas', []));
    } catch {
      // Older nodes: a legacy gas price already includes the tip the network expects.
      tip = (await this._provider.getGasPrice()).sub(baseFee);
    }
    if (tip.lt(0)) {
      tip = BigNumber.from(0);
    }
    // Twice the base fee rides out a few full blocks; only base + tip is actually paid.
    return {maxFeePerGas: baseFee.mul(2).add(tip), maxPriorityFeePerGas: tip};
  }

  private async warnIfLowGas(): Promise<void> {
    try {
      const balance = await this._wallet.getBalance();
      if (balance.lt(LOW_GAS_BALANCE)) {
        this._logger.warn(`keeper gas balance is low: ${utils.formatEther(balance)} — top up ${this._wallet.address}`);
      }
    } catch (e) {
      this._logger.errors('could not read keeper balance', e);
    }
  }
}

const TX_WAIT_TIMEOUT_MS = 5 * 60_000;

function withTimeout<T>(promise: Promise<T>, ms: number, message: string): Promise<T> {
  let timer: NodeJS.Timeout | undefined;
  const timeout = new Promise<never>((_, reject) => {
    timer = setTimeout(() => reject(new Error(message)), ms);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

/** Move from `current` toward `target` by at most `maxPercent` of `current`. */
export function clampStep(current: BigNumber, target: BigNumber, maxPercent: number): BigNumber {
  const maxDelta = current.mul(Math.round(maxPercent * 100)).div(10_000);
  return target.gt(current) ? current.add(maxDelta) : current.sub(maxDelta);
}

/** |b - a| / a in percent; Infinity when a is zero. */
export function percentChange(a: BigNumber, b: BigNumber): number {
  if (a.isZero()) {
    return Number.POSITIVE_INFINITY;
  }
  // Basis points of basis points keep 4 decimals without leaving integer math.
  const diff = b.sub(a).abs();
  return diff.mul(1_000_000).div(a).toNumber() / 10_000;
}
