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
  async verifyCreateRock(data: CreateRockRequest): Promise<string[] | undefined> {
    const receipt = await this._blockchainCenterApi.getTransactionReceipt(this._config.name, data.tx);
    if (!receipt) {
      throw new Error(`[${this._config.name}] Transaction receipt ${data.tx} not found`);
    }
    this.#logger.info(`[${this._config.name}] Received receipt for ${data.tx}`);
    this.#logger.assert(receipt.status == 1, `Transaction ${data.tx} failed`);
    this.#logger.assert(receipt.blockNumber >= this.LimitCreateRockBlockNumber, `Transaction ${data.tx} too old`);

    const {wallet, heroIds, heroDetails} = this.#extractCreateRock(data.tx, receipt);

    // The on-chain CreateRock `owner` is authoritative for the wallet — this replaces the
    // old `tx.from` check, which was the relayer (not the user) for a delegated tx.
    this.#logger.assert(wallet.toLowerCase() == data.wallet_address.toLowerCase(), `Transaction ${data.tx} not from ${data.wallet_address}`);

    this.#logger.info(`[${this._config.name}] Burned listIdHero: [${heroIds.join(',')}]`);
    this.#logger.assert(heroIds.length == data.hero_ids.length, `List hero length not match`);
    for (let i = 0; i < heroIds.length; i++) {
      this.#logger.assert(heroIds[i] == data.hero_ids[i], `List hero not match`);
    }
    // Undefined for burns older than the CreateRockDetails contract upgrade — the caller then has
    // to fall back to its own hero data. Any assert above throws, so reaching here means verified.
    return heroDetails;
  }

  async getBurnedHeroData(tx: string): Promise<BurnedHeroData | undefined> {
    const receipt = await this._blockchainCenterApi.getTransactionReceipt(this._config.name, tx);
    if (!receipt) {
      throw new Error(`Transaction receipt ${tx} not found`);
    }
    this.#logger.assert(receipt.status == 1, `Transaction ${tx} failed`);

    const {wallet, heroIds, heroDetails} = this.#extractCreateRock(tx, receipt);
    return {
      tx,
      wallet_address: wallet,
      hero_ids: heroIds,
      hero_details: heroDetails,
    };
  }

  /**
   * Scan the receipt for `CreateRock` events emitted by the BHeroS contract and return
   * the burning wallet + all burned hero ids. Aggregates across multiple events (a
   * delegated tx can batch several executions) and requires them to share one owner.
   */
  #extractCreateRock(tx: string, receipt: ITransactionReceiptResult): { wallet: string; heroIds: number[]; heroDetails?: string[] } {
    const contract = this._config.bheroSAddress.toLowerCase();
    const createRockTopic = this._interface.getEventTopic('CreateRock').toLowerCase();
    const createRockDetailsTopic = this._interface.getEventTopic('CreateRockDetails').toLowerCase();

    let wallet: string | undefined;
    const heroIds: number[] = [];
    const heroDetails: string[] = [];
    for (const log of receipt.logs) {
      if (log.address.toLowerCase() != contract) continue;
      const topic = log.topics[0]?.toLowerCase();
      if (topic != createRockTopic && topic != createRockDetailsTopic) continue;

      const parsed = this._interface.parseLog({topics: log.topics, data: log.data});
      const owner = parsed.args['owner'] as string;

      if (!wallet) {
        wallet = owner;
      } else {
        this.#logger.assert(wallet.toLowerCase() == owner.toLowerCase(), `Transaction ${tx} has CreateRock events from multiple wallets`);
      }

      if (topic == createRockTopic) {
        for (const id of parsed.args['listIdHero'] as BigNumber[]) heroIds.push(id.toNumber());
      } else {
        for (const details of parsed.args['listDetails'] as BigNumber[]) heroDetails.push(details.toString());
      }
    }

    this.#logger.assert(wallet, `Transaction ${tx} has no CreateRock event for contract ${this._config.bheroSAddress}`);
    this.#logger.assert(heroIds.length > 0, `Transaction ${tx} has no burned heroes`);
    // Burns older than the CreateRockDetails upgrade carry no details at all. A partial set means
    // the two events disagree — refuse it rather than credit rock for a subset.
    this.#logger.assert(
      heroDetails.length == 0 || heroDetails.length == heroIds.length,
      `Transaction ${tx} has ${heroDetails.length} hero details for ${heroIds.length} burned heroes`,
    );
    return {wallet: wallet!, heroIds, heroDetails: heroDetails.length > 0 ? heroDetails : undefined};
  }
}

export type CreateRockRequest = {
  tx: string;
  wallet_address: string;
  hero_ids: number[];
}

export type BurnedHeroData = CreateRockRequest & {
  // Raw on-chain hero details, one per hero id in the same order. Undefined for burns older than
  // the CreateRockDetails contract upgrade.
  hero_details?: string[];
}
