import {ethers} from "ethers";
import {BaseError} from "../Error";
import * as Config from "../Config";
import IDependencies from "../services/IDependencies";
import {CacheStatus} from "../consts/Consts";
import {IBlockchainConfig} from "../BlockchainConfig";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";

export interface IDetailsRequest {
  address?: string;
  id?: number;
  details?: string;
  clearCache?: boolean;
}

const TOKEN_ID_COUNTER_ABI = [{
  inputs: [],
  name: 'tokenIdCounter',
  outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
  stateMutability: 'view',
  type: 'function'
}];

const OWNER_OF_ABI = [{
  inputs: [{internalType: 'uint256', name: 'tokenId', type: 'uint256'}],
  name: 'ownerOf',
  outputs: [{internalType: 'address', name: '', type: 'address'}],
  stateMutability: 'view',
  type: 'function'
}];

const GET_TOKEN_DETAILS_BY_OWNER_ABI = [{
  inputs: [{internalType: 'address', name: 'owner', type: 'address'}],
  name: 'getTokenDetailsByOwner',
  outputs: [{internalType: 'uint256[]', name: '', type: 'uint256[]'}],
  stateMutability: 'view',
  type: 'function'
}];

const TOKEN_DETAILS_ABI = [{
  inputs: [{internalType: 'uint256', name: 'tokenId', type: 'uint256'}],
  name: 'tokenDetails',
  outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
  stateMutability: 'view',
  type: 'function'
}];

export class NftApi {
  constructor(
    protected readonly _dep: IDependencies,
    protected readonly _config: IBlockchainConfig,
    protected readonly _blockchainCenterApi: BlockchainCenterApi,
    protected readonly _contractAddress: string,
    /**
     * Key để phân biệt cho redis
     */
    protected readonly _cacheId: string,
  ) {
  }

  async getTokenIdCounter() {
    const result = await this._blockchainCenterApi.callContract(
      this._config.name,
      this._contractAddress,
      TOKEN_ID_COUNTER_ABI,
      'tokenIdCounter',
      []
    );
    return ethers.BigNumber.from(result).toNumber();
  }

  async getOwner(id: number) {
    try {
      const result = await this._blockchainCenterApi.callContract(
        this._config.name,
        this._contractAddress,
        OWNER_OF_ABI,
        'ownerOf',
        [id]
      );
      return result as string;
    } catch (ex) {
      if (ex instanceof BaseError) {
        this._dep.logger.errors(
          `getOwner(${id}): ${ex.message}`,
          ex.stack?.split(`\n`)
        );
      }
      return Config.ZERO_ADDRESS;
    }
  }

  async _getCachedDetailsByOwner(address: string): Promise<string[]> {
    const cached = await this._dep.cachedNftDetails.getDetailsByOwner(this._cacheId, address);
    if (cached.status === CacheStatus.NotExist) {
      return this._getDetailsByOwner(address);
    }
    if (cached.status === CacheStatus.Expired) {
      this._getDetailsByOwner(address).then(); // chạy ngầm
    }
    return cached.details; // trả về details cũ
  }

  async _getDetailsByOwner(address: string): Promise<string[]> {
    try {
      const result = await this._blockchainCenterApi.callContract(
        this._config.name,
        this._contractAddress,
        GET_TOKEN_DETAILS_BY_OWNER_ABI,
        'getTokenDetailsByOwner',
        [address]
      );
      // Result is an array of string (BigNumber serialized as decimal strings)
      return (result as string[]).map((item) => item.toString());
    } catch (e) {
      this._dep.logger.error(e);
      return [];
    }
  }

  async _getCachedDetailsById(id: number): Promise<string> {
    const cached = await this._dep.cachedNftDetails.getDetailsById(this._cacheId, id);
    if (cached.status === CacheStatus.NotExist) {
      return this._getDetailsById(id);
    }
    if (cached.status === CacheStatus.Expired) {
      this._getDetailsById(id).then() // chạy ngầm
    }
    return cached.details.length > 0 ? cached.details[0] : "0"; // trả về details cũ
  }

  async _getDetailsById(id: number): Promise<string> {
    try {
      const result = await this._blockchainCenterApi.callContract(
        this._config.name,
        this._contractAddress,
        TOKEN_DETAILS_ABI,
        'tokenDetails',
        [id]
      );
      // Result is a string (BigNumber serialized as decimal string)
      return result.toString();
    } catch (e) {
      this._dep.logger.error(e);
      return "0";
    }
  }

  async _verifyOwner(id: number, walletAddress: string): Promise<boolean> {
    const cachedOwner = await this._dep.cachedNftOwner.getCachedOwnerOfNftId(this._cacheId, id);
    if (cachedOwner.status === CacheStatus.Valid) {
      return cachedOwner.walletAddress === walletAddress;
    } else {
      // Silent run
      (async () => {
        const owner = await this.getOwner(id);
        if (owner != Config.ZERO_ADDRESS) {
          this._dep.cachedNftOwner.setCachedOwnerOfNftId(this._cacheId, id, owner);
        }
      })().then().catch(console.error);

      // Tạm thời cho phép sử dụng trước khi kiểm tra
      return true;
    }
  }

  async getDetails(options: IDetailsRequest): Promise<string[]> {
    // FIXME: tạm thời ko cần cache details (vì details sẽ bị thay đổi khi repair shield)
    options.clearCache = true;

    if (options.address !== undefined) {
      return options.clearCache
        ? (await this._getDetailsByOwner(options.address))
        : (await this._getCachedDetailsByOwner(options.address));
    }
    if (options.id !== undefined) {
      return [options.clearCache
        ? (await this._getDetailsById(options.id))
        : await this._getCachedDetailsById(options.id)];
    }
    if (options.details !== undefined) {
      const details: string = options.details;
      return [details];
    }
    return [];
  }
}
