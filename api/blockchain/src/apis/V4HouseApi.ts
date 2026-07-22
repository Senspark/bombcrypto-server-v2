import {IBlockchainConfig} from "../BlockchainConfig";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";
import ILogger from "../services/ILogger";
import * as Utility from "../Utility";

export interface V4HouseData {
  id: number;
  details: string;
}

export interface V4HouseNetworkBindings {
  config: IBlockchainConfig;
  centerApi: BlockchainCenterApi;
}

const GET_TOKEN_DETAILS_BY_OWNER_ABI = [{
  inputs: [{internalType: 'address', name: 'owner', type: 'address'}],
  name: 'getTokenDetailsByOwner',
  outputs: [{internalType: 'uint256[]', name: '', type: 'uint256[]'}],
  stateMutability: 'view',
  type: 'function',
}];

/**
 * V4HouseApi — internal-only house sync for the SYNC_V4 stream.
 *
 * Throws on upstream failure so V4Handlers can skip publishing to the Redis
 * stream. Not cached — house payloads are small and change infrequently, so
 * freshness matters more than dedupe.
 */
export class V4HouseApi {
  constructor(
    private readonly _networks: Map<string, V4HouseNetworkBindings>,
    private readonly _logger: ILogger,
  ) {}

  supportsNetwork(network: string): boolean {
    return this._networks.has(network);
  }

  async fetchHouses(network: string, address: string): Promise<V4HouseData[]> {
    const binding = this._networks.get(network);
    if (!binding) {
      throw new Error(`V4HouseApi: unknown network ${network}`);
    }
    const details = await this._fetchDetailsByOwner(binding, address);
    return details
      .filter(d => d !== '0')
      .map(d => ({id: Utility.decodeHouseDetails(d).id, details: d}));
  }

  private async _fetchDetailsByOwner(binding: V4HouseNetworkBindings, owner: string): Promise<string[]> {
    const result = await binding.centerApi.callContract(
      binding.config.name,
      binding.config.bhouseTokenAddress,
      GET_TOKEN_DETAILS_BY_OWNER_ABI,
      'getTokenDetailsByOwner',
      [owner],
    );
    return (result as string[]).map(e => e.toString());
  }
}
