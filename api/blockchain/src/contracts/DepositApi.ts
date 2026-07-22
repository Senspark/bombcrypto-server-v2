import {ethers} from "ethers";
import {IBlockchainConfig} from "../BlockchainConfig";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";

const GET_DEPOSITED_V3_ABI = [{
  inputs: [{internalType: 'address', name: 'user', type: 'address'}],
  name: 'getDepositedV3',
  outputs: [{internalType: 'uint256[]', name: '', type: 'uint256[]'}],
  stateMutability: 'view',
  type: 'function'
}];

const GET_DEPOSITED_V2_ABI = [{
  inputs: [{internalType: 'address', name: 'user', type: 'address'}],
  name: 'getDepositedV2',
  outputs: [{internalType: 'uint256[]', name: '', type: 'uint256[]'}],
  stateMutability: 'view',
  type: 'function'
}];

const GET_TOKEN_LIST_ABI = [{
  inputs: [],
  name: 'getTokenList',
  outputs: [{internalType: 'address[]', name: '', type: 'address[]'}],
  stateMutability: 'view',
  type: 'function'
}];

export class DepositApi {
  constructor(
    private readonly _config: IBlockchainConfig,
    private readonly _blockchainCenterApi: BlockchainCenterApi
  ) {}

  async getDepositedV3(address: string) {
    const result = await this._blockchainCenterApi.callContract(
      this._config.name,
      this._config.depositAddress,
      GET_DEPOSITED_V3_ABI,
      'getDepositedV3',
      [address]
    );
    // Result is an array of strings (BigNumber serialized as decimal strings)
    return (result as string[]).map((e) => parseFloat(ethers.utils.formatEther(ethers.BigNumber.from(e))));
  }

  /**
   * Not used anymore, use v3 instead
   * @param address
   */
  async getDepositedV2(address: string) {
    const result = await this._blockchainCenterApi.callContract(
      this._config.name,
      this._config.depositAddress,
      GET_DEPOSITED_V2_ABI,
      'getDepositedV2',
      [address]
    );
    // Result is an array of strings (BigNumber serialized as decimal strings)
    return (result as string[]).map((e) => parseFloat(ethers.utils.formatEther(ethers.BigNumber.from(e))));
  }

  async getTokenList() {
    const result = await this._blockchainCenterApi.callContract(
      this._config.name,
      this._config.depositAddress,
      GET_TOKEN_LIST_ABI,
      'getTokenList',
      []
    );
    return (result as string[]).map((e) => e.toLowerCase());
  }
}
