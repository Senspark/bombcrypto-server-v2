import {ethers} from "ethers";
import {IBlockchainConfig} from "../BlockchainConfig";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";

const TOTAL_SUPPLY_ABI = [{
  inputs: [],
  name: 'totalSupply',
  outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
  stateMutability: 'view',
  type: 'function'
}];

const BALANCE_OF_ABI = [{
  inputs: [{internalType: 'address', name: 'account', type: 'address'}],
  name: 'balanceOf',
  outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
  stateMutability: 'view',
  type: 'function'
}];

export class CoinApi {
  constructor(
    private readonly _config: IBlockchainConfig,
    private readonly _blockchainCenterApi: BlockchainCenterApi
  ) {}

  async getTotalCoins() {
    const result = await this._blockchainCenterApi.callContract(
      this._config.name,
      this._config.bcoinTokenAddress,
      TOTAL_SUPPLY_ABI,
      'totalSupply',
      []
    );
    const ether = ethers.utils.formatEther(ethers.BigNumber.from(result));
    return parseFloat(ether);
  }

  async getBalance(address: string) {
    const result = await this._blockchainCenterApi.callContract(
      this._config.name,
      this._config.bcoinTokenAddress,
      BALANCE_OF_ABI,
      'balanceOf',
      [address]
    );
    const ether = ethers.utils.formatEther(ethers.BigNumber.from(result));
    return parseFloat(ether);
  }
}
