import {IBlockchainConfig} from "../BlockchainConfig";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";
import ILogger from "../services/ILogger";
import {ethers} from "ethers";

export interface V4DepositData {
  type: string;
  value: number;
}

export interface V4DepositNetworkBindings {
  config: IBlockchainConfig;
  centerApi: BlockchainCenterApi;
}

const GET_DEPOSITED_V3_ABI = [{
  inputs: [{internalType: 'address', name: 'user', type: 'address'}],
  name: 'getDepositedV3',
  outputs: [{internalType: 'uint256[]', name: '', type: 'uint256[]'}],
  stateMutability: 'view',
  type: 'function',
}];

const GET_TOKEN_LIST_ABI = [{
  inputs: [],
  name: 'getTokenList',
  outputs: [{internalType: 'address[]', name: '', type: 'address[]'}],
  stateMutability: 'view',
  type: 'function',
}];

/**
 * V4DepositApi — internal-only deposit sync for the SYNC_V4 stream.
 *
 * Throws on upstream failure so V4Handlers can skip publishing to the Redis
 * stream. Balances are fetched fresh each call (small payload, freshness
 * matters); the per-network token list is cached in-memory indefinitely
 * because it rarely changes.
 */
export class V4DepositApi {
  constructor(
    private readonly _networks: Map<string, V4DepositNetworkBindings>,
    private readonly _logger: ILogger,
  ) {}

  private readonly _tokenListCache = new Map<string, string[]>();

  supportsNetwork(network: string): boolean {
    return this._networks.has(network);
  }

  async fetchDeposited(network: string, address: string): Promise<V4DepositData[]> {
    const binding = this._networks.get(network);
    if (!binding) {
      throw new Error(`V4DepositApi: unknown network ${network}`);
    }
    const tokenList = await this._getTokenList(network, binding);
    const balances = await this._fetchDepositedBalances(binding, address);
    if (tokenList.length !== balances.length) {
      throw new Error(
        `Token list length (${tokenList.length}) does not match deposited balances length (${balances.length})`,
      );
    }
    const byType: { [key: string]: number } = {};
    tokenList.forEach((token, i) => {
      const type = binding.config.addressToSymbol[token];
      if (!type) {
        return;
      }
      byType[type] = (byType[type] ?? 0) + balances[i];
    });
    return Object.entries(byType).map(([type, value]) => ({type, value}));
  }

  private async _fetchDepositedBalances(binding: V4DepositNetworkBindings, address: string): Promise<number[]> {
    const result = await binding.centerApi.callContract(
      binding.config.name,
      binding.config.depositAddress,
      GET_DEPOSITED_V3_ABI,
      'getDepositedV3',
      [address],
    );
    return (result as string[]).map(e => parseFloat(ethers.utils.formatEther(ethers.BigNumber.from(e))));
  }

  private async _getTokenList(network: string, binding: V4DepositNetworkBindings): Promise<string[]> {
    const cached = this._tokenListCache.get(network);
    if (cached && cached.length > 0) {
      return cached;
    }
    const result = await binding.centerApi.callContract(
      binding.config.name,
      binding.config.depositAddress,
      GET_TOKEN_LIST_ABI,
      'getTokenList',
      [],
    );
    const list = (result as string[]).map(e => e.toLowerCase());
    if (list.length > 0) {
      this._tokenListCache.set(network, list);
    }
    return list;
  }
}
