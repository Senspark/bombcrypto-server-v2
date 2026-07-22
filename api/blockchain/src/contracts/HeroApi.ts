import {NftApi} from "./NftApi";
import {IBlockchainConfig} from "../BlockchainConfig";
import {ethers} from "ethers";
import * as Utility from "../Utility";
import {IExtendedDecodedDetails, IShortDetails} from "../IDecodedDetails";
import IDependencies from "../services/IDependencies";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";

const GET_CLAIMABLE_TOKENS_ABI = [{
  inputs: [{internalType: 'address', name: 'owner', type: 'address'}],
  name: 'getClaimableTokens',
  outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
  stateMutability: 'view',
  type: 'function'
}];

const GET_PENDING_TOKENS_ABI = [{
  inputs: [{internalType: 'address', name: 'owner', type: 'address'}],
  name: 'getPendingTokens',
  outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
  stateMutability: 'view',
  type: 'function'
}];

export class HeroApi extends NftApi {
  constructor(
    dep: IDependencies,
    config: IBlockchainConfig,
    blockchainCenterApi: BlockchainCenterApi
  ) {
    const cacheId = `${config.name}-hero`;
    super(dep, config, blockchainCenterApi, config.bheroTokenAddress, cacheId);
  }

  async getDecodedDetails(options: any): Promise<IExtendedDecodedDetails[] | IShortDetails[]> {
    const detailList = await this.getDetails(options);
    if (detailList.length === 0) {
      return [];
    }
    const mode = options.mode;
    return detailList.filter(e => e !== '0').map((details) => {
      const decodedDetails = Utility.decodeHeroDetails(details);
      const sameData = {
        stake_amount: 0,
        stake_bcoin: 0,
        stake_sen: 0
      };
      return mode === 0
        ? {
          ...decodedDetails,
          details,
          block_details: `${this._config.explorerUrl}/block/${decodedDetails.block_number}`,
          token_transfers: `${this._config.explorerUrl}/token/${this._config.bheroTokenAddress}?a=${decodedDetails.id}`,
          ...sameData
        }
        : {
          details,
          id: decodedDetails.id,
          ...sameData
        };
    });
  }

  async _getClaimable(address: string) {
    const result = await this._blockchainCenterApi.callContract(
      this._config.name,
      this._contractAddress,
      GET_CLAIMABLE_TOKENS_ABI,
      'getClaimableTokens',
      [address]
    );
    return parseInt(ethers.BigNumber.from(result).toString());
  }

  async _getPending(address: string) {
    const result = await this._blockchainCenterApi.callContract(
      this._config.name,
      this._contractAddress,
      GET_PENDING_TOKENS_ABI,
      'getPendingTokens',
      [address]
    );
    return parseInt(ethers.BigNumber.from(result).toString());
  }
}
