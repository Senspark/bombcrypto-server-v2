import IDependencies from "../services/IDependencies";
import {CHAIN_NAMES, IBlockchainConfig} from "../BlockchainConfig";
import {BigNumber, ethers} from "ethers";
import ILogger from "../services/ILogger";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";
import {ITransactionReceiptResult} from "../providers/BlockchainCenterApi";

/**
 * Giới hạn createRock chỉ được allow sau những block này
 */
const CREATE_ROCK_MIN_BLOCK_NUMBER_BSC = 39708741;
const CREATE_ROCK_MIN_BLOCK_NUMBER_POLYGON = 58293344;

export default class HeroSApi {
  constructor(
    private readonly _dep: IDependencies,
    private readonly _config: IBlockchainConfig,
    private readonly _interface: ethers.utils.Interface,
    private readonly _blockchainCenterApi: BlockchainCenterApi
  ) {
    this.#logger = _dep.logger.clone('[HERO_S]');
    if (this._dep.envConfig.isProduction) {
      this.LimitCreateRockBlockNumber = this._config.name == CHAIN_NAMES.BNB ? CREATE_ROCK_MIN_BLOCK_NUMBER_BSC : CREATE_ROCK_MIN_BLOCK_NUMBER_POLYGON;
    }
  }

  readonly #logger: ILogger;
  readonly LimitCreateRockBlockNumber: number = 0;

  /**
   * Verify a createRock burn against a client-supplied {tx, wallet_address, hero_ids}.
   *
   * The source of truth is the `CreateRock(address indexed owner, uint256 numRock,
   * uint256[] listIdHero)` event emitted by the BHeroS contract itself — NOT the
   * top-level transaction. This is what makes it work for both a direct createRock
   * call AND a call routed through an account-abstraction / delegation wrapper
   * (e.g. MetaMask Smart Account `redeemDelegations`), where `tx.to` is the
   * DelegationManager and `tx.from` is a relayer, not the user. Because a log can only
   * be emitted by the contract at that address, matching (log.address, topic0) makes
   * the event authentic regardless of who submitted the tx.
   */
  async verifyCreateRock(data: CreateRockRequest) {
    const receipt = await this._blockchainCenterApi.getTransactionReceipt(this._config.name, data.tx);
    if (!receipt) {
      throw new Error(`[${this._config.name}] Transaction receipt ${data.tx} not found`);
    }
    this.#logger.info(`[${this._config.name}] Received receipt for ${data.tx}`);
    this.#logger.assert(receipt.status == 1, `Transaction ${data.tx} failed`);
    this.#logger.assert(receipt.blockNumber >= this.LimitCreateRockBlockNumber, `Transaction ${data.tx} too old`);

    const {wallet, heroIds} = this.#extractCreateRock(data.tx, receipt);

    // The on-chain CreateRock `owner` is authoritative for the wallet — this replaces the
    // old `tx.from` check, which was the relayer (not the user) for a delegated tx.
    this.#logger.assert(wallet.toLowerCase() == data.wallet_address.toLowerCase(), `Transaction ${data.tx} not from ${data.wallet_address}`);

    this.#logger.info(`[${this._config.name}] Burned listIdHero: [${heroIds.join(',')}]`);
    this.#logger.assert(heroIds.length == data.hero_ids.length, `List hero length not match`);
    for (let i = 0; i < heroIds.length; i++) {
      this.#logger.assert(heroIds[i] == data.hero_ids[i], `List hero not match`);
    }
    return true;
  }

  async getBurnedHeroData(tx: string): Promise<BurnedHeroData | undefined> {
    const receipt = await this._blockchainCenterApi.getTransactionReceipt(this._config.name, tx);
    if (!receipt) {
      throw new Error(`Transaction receipt ${tx} not found`);
    }
    this.#logger.assert(receipt.status == 1, `Transaction ${tx} failed`);

    const {wallet, heroIds} = this.#extractCreateRock(tx, receipt);
    return {
      tx,
      wallet_address: wallet,
      hero_ids: heroIds,
    };
  }

  /**
   * Scan the receipt for `CreateRock` events emitted by the BHeroS contract and return
   * the burning wallet + all burned hero ids. Aggregates across multiple events (a
   * delegated tx can batch several executions) and requires them to share one owner.
   */
  #extractCreateRock(tx: string, receipt: ITransactionReceiptResult): { wallet: string; heroIds: number[] } {
    const contract = this._config.bheroSAddress.toLowerCase();
    const createRockTopic = this._interface.getEventTopic('CreateRock').toLowerCase();

    let wallet: string | undefined;
    const heroIds: number[] = [];
    for (const log of receipt.logs) {
      if (log.address.toLowerCase() != contract) continue;
      if (log.topics[0]?.toLowerCase() != createRockTopic) continue;

      const parsed = this._interface.parseLog({topics: log.topics, data: log.data});
      const owner = parsed.args['owner'] as string;
      const listIdHero = parsed.args['listIdHero'] as BigNumber[];

      if (!wallet) {
        wallet = owner;
      } else {
        this.#logger.assert(wallet.toLowerCase() == owner.toLowerCase(), `Transaction ${tx} has CreateRock events from multiple wallets`);
      }
      for (const id of listIdHero) heroIds.push(id.toNumber());
    }

    this.#logger.assert(wallet, `Transaction ${tx} has no CreateRock event for contract ${this._config.bheroSAddress}`);
    this.#logger.assert(heroIds.length > 0, `Transaction ${tx} has no burned heroes`);
    return {wallet: wallet!, heroIds};
  }
}

export type CreateRockRequest = {
  tx: string;
  wallet_address: string;
  hero_ids: number[];
}

export type BurnedHeroData = CreateRockRequest;
