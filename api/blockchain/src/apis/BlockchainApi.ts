import {IBlockchainConfig} from "../BlockchainConfig";
import {ethers} from "ethers";
import {CoinApi} from "../contracts/CoinApi";
import {HeroApi} from "../contracts/HeroApi";
import {HouseApi} from "../contracts/HouseApi";
import {DepositApi} from "../contracts/DepositApi";
import * as Abi from "../Abi";
import * as Utility from "../Utility";
import {InvalidAddressError} from "../Error";
import {IBlockchainApi} from "../IBlockchainApi";
import {IExtendedDecodedDetails, IShortDetails} from "../IDecodedDetails";
import {IHeroStakeApi, StakeAmount} from "../IHeroStakeApi";
import {HeroStakeApiDatabase} from "../database/HeroStakeApi.Database";
import {HeroStakeApiCache} from "../database/HeroStakeApi.Cache";
import IDependencies from "../services/IDependencies";
import HeroSApi from "../contracts/HeroSApi";
import {HeroStakeApiBlockchain} from "../contracts/HeroStakeApiBlockchain";
import {formatNumber} from "../Utility";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";

export default class BlockchainApi implements IBlockchainApi {
  private readonly _coinApi: CoinApi;
  private readonly _heroApi: HeroApi;
  private readonly _houseApi: HouseApi;
  private readonly _depositApi: DepositApi;
  private readonly _heroStakeApi: IHeroStakeApi;
  private readonly _heroStakeBlockchain: HeroStakeApiBlockchain;
  readonly heroSApi: HeroSApi;
  private _latestBlock: number;

  constructor(
    private readonly _dep: IDependencies,
    readonly config: IBlockchainConfig,
    private readonly _blockchainCenterApi: BlockchainCenterApi
  ) {
    // All contract APIs use BlockchainCenterApi
    this._coinApi = new CoinApi(config, this._blockchainCenterApi);
    this._heroApi = new HeroApi(this._dep, config, this._blockchainCenterApi);
    this._houseApi = new HouseApi(this._dep, config, this._blockchainCenterApi);
    this._depositApi = new DepositApi(config, this._blockchainCenterApi);

    const network = config.name;
    this._heroStakeApi = new HeroStakeApiCache(this._dep, new HeroStakeApiDatabase(this._dep, network));
    this._heroStakeBlockchain = new HeroStakeApiBlockchain(
      this._heroStakeApi,
      config,
      `[${network}]`,
      this._blockchainCenterApi
    );

    this.heroSApi = new HeroSApi(this._dep, config, new ethers.utils.Interface(Abi.BHERO_S_ABI), this._blockchainCenterApi);

    this._latestBlock = 0;
  }

  async _checkValidBlock(block: number) {
    if (block <= this._latestBlock) {
      // OK.
      return;
    }
    await this.getLatestBlock();
    if (block <= this._latestBlock) {
      // OK.
      return;
    }
    throw new Error(`${block} > ${this._latestBlock}`);
  }

  async getLatestBlock() {
    const result = await this._blockchainCenterApi.getLatestBlockNumber(this.config.name);
    if (result === null) {
      throw new Error('Failed to get latest block number');
    }
    this._latestBlock = result;
    return this._latestBlock;
  }

  async getLogs(
    fromBlock: number,
    toBlock: number,
    address: string,
    topic: string) {
    if (!Utility.isAddress(address)) {
      throw new InvalidAddressError(address);
    }
    await this._checkValidBlock(toBlock);
    const logs = await this._blockchainCenterApi.getLogs(
      this.config.name,
      address,
      [topic],
      fromBlock,
      toBlock
    );
    // Convert to ethers.providers.Log format for compatibility
    return logs.map(log => ({
      blockNumber: log.blockNumber,
      blockHash: '',
      transactionIndex: 0,
      removed: false,
      address: address,
      data: log.data,
      topics: [topic],
      transactionHash: log.transactionHash,
      logIndex: 0,
    }));
  }

  async getTotalCoins() {
    return await this._coinApi.getTotalCoins();
  }

  async getCoinBalance(address: string) {
    return await this._coinApi.getBalance(address);
  }

  async getHeroOwner(id: number) {
    return await this._heroApi.getOwner(id);
  }

  async getHeroTokenIdCounter() {
    return await this._heroApi.getTokenIdCounter();
  }

  async getHeroStakedAmount(heroId: number): Promise<StakeAmount> {
    return await this._heroStakeApi.getStakedAmount(heroId);
  }

  async refreshHeroStakedAmount(heroId: number, uid?: number): Promise<StakeAmount> {
    const fromBlockchain = await this._heroStakeBlockchain.getStakedAmountBig(heroId);
    const fromData = await this._heroStakeApi.getStakedAmountsBig(heroId);
    let shouldUpdate = fromData.length === 0;

    if (!shouldUpdate) {
      const fromDataValue = fromData[0];
      shouldUpdate = !fromDataValue[0].eq(fromBlockchain[0]) || !fromDataValue[1].eq(fromBlockchain[1]);
    }

    if (shouldUpdate) {
      await this._heroStakeApi.setStakedAmount(heroId, fromBlockchain);
    }
    // Always notify the game server so its DB (and online hero RAM) picks up the
    // on-chain value, even when our own cache already matched (e.g. a manual re-run
    // after the stake was already corrected). Broadcasting only inside `shouldUpdate`
    // would silently skip the sync in exactly that case.
    await this._heroStakeApi.broadcastStakedAmount(heroId, fromBlockchain, uid);
    return [formatNumber(fromBlockchain[0]), formatNumber(fromBlockchain[1])];
  }

  async getHeroDetails(options: any) {
    return await this._heroApi.getDetails(options);
  }

  async getDecodedHeroDetails(options: any): Promise<IExtendedDecodedDetails[] | IShortDetails[]> {
    const details = await this._heroApi.getDecodedDetails(options);
    const ids: number[] = details.map(d => d.id);
    if (ids.length === 0) {
      return details;
    }
    const stakeAmounts = await this._heroStakeApi.getStakedAmounts(...ids);
    for (let i = 0; i < details.length; i++) {
      const bcoin = stakeAmounts[i][0];
      const sen = stakeAmounts[i][1];
      details[i].stake_amount = bcoin;
      details[i].stake_bcoin = bcoin;
      details[i].stake_sen = sen;
    }
    return details;
  }

  async getHouseOwner(id: number) {
    return await this._houseApi.getOwner(id);
  }

  async getHouseTokenIdCounter() {
    return await this._houseApi.getTokenIdCounter();
  }

  async getHouseDetails(options: any) {
    return await this._houseApi.getDetails(options);
  }

  async getDecodedHouseDetails(options: any) {
    return await this._houseApi.getDecodedDetails(options);
  }

  async getDepositedV3(address: string) {
    return await this._depositApi.getDepositedV3(address);
  }

  async getDepositedV2(address: string) {
    return await this._depositApi.getDepositedV2(address);
  }

  async getTokenList() {
    return await this._depositApi.getTokenList();
  }
}
