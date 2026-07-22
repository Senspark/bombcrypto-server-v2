import {IBlockchainConfig} from "../BlockchainConfig";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";
import {HeroStakeApiBlockchain} from "../contracts/HeroStakeApiBlockchain";
import {IHeroStakeApi} from "../IHeroStakeApi";
import ILogger from "../services/ILogger";

export interface V4HeroStakeRefreshNetworkBindings {
  config: IBlockchainConfig;
  centerApi: BlockchainCenterApi;
  stakeBlockchainApi: HeroStakeApiBlockchain;
  stakeStorageApi: IHeroStakeApi;
}

const RECEIPT_POLL_ATTEMPTS = 2;
const RECEIPT_POLL_GAP_MS = 3000;

/**
 * Client-triggered hero stake refresh.
 *
 * Flow: wait briefly for tx receipt → read on-chain stake (bcoin + sen) →
 * persist to DB+Redis → broadcast to AP_BL_HERO_STAKE_STR with originating uid
 * so the Kotlin consumer can route a live push to that user.
 *
 * Throws on any failure (tx never mined within budget, RPC error, etc.). The
 * caller logs and drops — the scheduler is the failsafe.
 */
export class V4HeroStakeRefreshApi {
  constructor(
    private readonly _networks: Map<string, V4HeroStakeRefreshNetworkBindings>,
    private readonly _logger: ILogger,
  ) {}

  supportsNetwork(network: string): boolean {
    return this._networks.has(network);
  }

  async refreshStake(network: string, uid: number, heroId: number, txHash: string): Promise<void> {
    const binding = this._networks.get(network);
    if (!binding) {
      throw new Error(`V4HeroStakeRefreshApi: unknown network ${network}`);
    }

    const mined = await this._waitForReceipt(binding, txHash);
    if (!mined) {
      throw new Error(`tx not mined within budget: ${txHash}`);
    }

    const staked = await binding.stakeBlockchainApi.getStakedAmountBig(heroId);
    await binding.stakeStorageApi.setStakedAmount(heroId, staked);
    await binding.stakeStorageApi.broadcastStakedAmount(heroId, staked, uid);
  }

  private async _waitForReceipt(binding: V4HeroStakeRefreshNetworkBindings, txHash: string): Promise<boolean> {
    for (let i = 0; i < RECEIPT_POLL_ATTEMPTS; i++) {
      const receipt = await binding.centerApi.getTransactionReceipt(binding.config.name, txHash);
      if (receipt) {
        return true;
      }
      if (i < RECEIPT_POLL_ATTEMPTS - 1) {
        await sleep(RECEIPT_POLL_GAP_MS);
      }
    }
    return false;
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}
