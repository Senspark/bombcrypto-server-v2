import {BHERO_STAKE_ABI} from "../Abi";
import {BigNumber, ethers} from "ethers";
import {IHeroStakeApi, StakeAmount, StakeAmountBig} from "../IHeroStakeApi";
import {IBlockchainConfig} from "../BlockchainConfig";
import {formatNumber} from "../Utility";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";

/**
 * [heroId:number, [bcoin:BigNumber, sen:BigNumber]]
 */
type Balances = Map<number, QueryBalance>;
type QueryBalance = {
  bcoin: BigNumber | undefined;
  sen: BigNumber | undefined;
};

enum TokenName {Unknown, Bcoin, Sen}

const FnBalanceChangedEventTopic = '0xaeeb0cb16f299136e7e5467ea84217150fe83008833064528f360cde7b7b54c3'
const BLOCK_JUMP_STEP = 1000;

const GET_COIN_BALANCE_V2_ABI = [{
  inputs: [
    {internalType: 'address', name: 'token', type: 'address'},
    {internalType: 'uint256', name: 'heroId', type: 'uint256'}
  ],
  name: 'getCoinBalanceV2',
  outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
  stateMutability: 'view',
  type: 'function'
}];

export class HeroStakeApiBlockchain {
  private readonly _contractAddress: string;
  private readonly _iFace: ethers.utils.Interface;

  constructor(
    private readonly _api: IHeroStakeApi,
    private readonly _blockchainConfig: IBlockchainConfig,
    private readonly _logPrefix: string,
    private readonly _blockchainCenterApi: BlockchainCenterApi,
  ) {
    this._contractAddress = _blockchainConfig.bheroStakeAddress;
    this._iFace = new ethers.utils.Interface(JSON.stringify(BHERO_STAKE_ABI));
  }

  async queryEvents(fromBlock: number, toBlock: number) {
    const balances: Balances = new Map<number, QueryBalance>();
    await this._queryEventsRpc(balances, fromBlock, toBlock);

    // Write to database
    const heroIds = Array.from(balances.keys());
    const stakedAmounts = await this._api.getStakedAmountsBig(...heroIds);

    for (let i = 0; i < heroIds.length; i++) {
      const heroId = heroIds[i];
      const staked = stakedAmounts[i];
      const newBalances = balances.get(heroId)!;
      if (newBalances.bcoin) {
        staked[0] = newBalances.bcoin;
      }
      if (newBalances.sen) {
        staked[1] = newBalances.sen;
      }
      await this._api.setStakedAmount(heroId, staked);
      await this._api.broadcastStakedAmount(heroId, staked);
    }
  }

  async getStakedAmountBig(heroId: number): Promise<StakeAmountBig> {
    return [await this.getStakedBcoinBig(heroId), await this.getStakedSenBig(heroId)];
  }

  async getStakedAmount(heroId: number): Promise<StakeAmount> {
    return [await this.getStakedBcoin(heroId), await this.getStakedSen(heroId)];
  }

  async getStakedBcoinBig(heroId: number): Promise<BigNumber> {
    const result = await this._blockchainCenterApi.callContract(
      this._blockchainConfig.name,
      this._contractAddress,
      GET_COIN_BALANCE_V2_ABI,
      'getCoinBalanceV2',
      [this._blockchainConfig.bcoinTokenAddress, heroId]
    );
    return BigNumber.from(result);
  }

  async getStakedBcoin(heroId: number): Promise<number> {
    return formatNumber(await this.getStakedBcoinBig(heroId));
  }

  async getStakedSenBig(heroId: number) {
    const result = await this._blockchainCenterApi.callContract(
      this._blockchainConfig.name,
      this._contractAddress,
      GET_COIN_BALANCE_V2_ABI,
      'getCoinBalanceV2',
      [this._blockchainConfig.senTokenAddress, heroId]
    );
    return BigNumber.from(result);
  }

  async getStakedSen(heroId: number) {
    return formatNumber(await this.getStakedSenBig(heroId));
  }

  private async _getCoinBalanceV2(tokenAddress: string, heroId: number): Promise<BigNumber> {
    const result = await this._blockchainCenterApi.callContract(
      this._blockchainConfig.name,
      this._contractAddress,
      GET_COIN_BALANCE_V2_ABI,
      'getCoinBalanceV2',
      [tokenAddress, heroId]
    );
    return BigNumber.from(result);
  }

  private async _queryEventsRpc(balances: Balances, fromBlock: number, toBlock: number): Promise<number> {
    let logs: {blockNumber: number; data: string; transactionHash: string}[] = [];
    let fromB = fromBlock;
    let toB = Math.min(toBlock, fromBlock + BLOCK_JUMP_STEP);

    while (fromB <= toBlock) {
      this.info(`Query from ${fromB} to ${toB} (${toB - fromB} blocks)`);

      const apiLogs = await this._blockchainCenterApi.getLogs(
        this._blockchainConfig.name,
        this._blockchainConfig.bheroStakeAddress,
        [FnBalanceChangedEventTopic],
        fromB,
        toB
      );

      logs = logs.concat(apiLogs);
      fromB = toB + 1;
      toB = Math.min(toBlock, fromB + BLOCK_JUMP_STEP);
    }

    if (logs.length === 0) {
      this.info("No logs found");
      return 0;
    }

    // Convert to ethers.providers.Log format for parsing
    const ethersLogs: ethers.providers.Log[] = logs.map(log => ({
      blockNumber: log.blockNumber,
      blockHash: '',
      transactionIndex: 0,
      removed: false,
      address: this._blockchainConfig.bheroStakeAddress,
      data: log.data,
      topics: [FnBalanceChangedEventTopic],
      transactionHash: log.transactionHash,
      logIndex: 0,
    }));

    const decodedLogs = ethersLogs.map(log => this._iFace.parseLog(log));
    for (const decodedLog of decodedLogs) {
      const [tokenAddress, heroId, amount] = decodedLog.args;
      const tokenName = this.getTokenName(tokenAddress);
      if (tokenName === TokenName.Unknown) {
        this.warn(`Unknown token address: ${tokenAddress}`);
        continue;
      }
      const balance = await this._getCoinBalanceV2(tokenAddress, heroId);

      this.info(`HeroId: ${heroId}, balance (${tokenName}): ${balance.toString()}`);
      const queryBalance: QueryBalance = {
        bcoin: tokenName === TokenName.Bcoin ? balance : undefined,
        sen: tokenName === TokenName.Sen ? balance : undefined,
      };
      this.adjustBalance(balances, heroId.toNumber(), queryBalance);
    }

    return decodedLogs.length;
  }

  private adjustBalance(balances: Balances, heroId: number, amount: QueryBalance) {
    if (!balances.has(heroId)) {
      balances.set(heroId, amount);
    } else {
      const cur = balances.get(heroId)!;
      if (amount.bcoin) {
        cur.bcoin = amount.bcoin;
      }
      if (amount.sen) {
        cur.sen = amount.sen;
      }
    }
  }

  private info(msg: string) {
    console.info(`${this._logPrefix} ${msg}`);
  }

  private warn(msg: string) {
    console.warn(`${this._logPrefix} ${msg}`);
  }

  private getTokenName(tokenAddress: string): TokenName {
    switch (tokenAddress.toLowerCase()) {
      case this._blockchainConfig.bcoinTokenAddress:
        return TokenName.Bcoin;
      case this._blockchainConfig.senTokenAddress:
        return TokenName.Sen;
      default:
        return TokenName.Unknown;
    }
  }
}
