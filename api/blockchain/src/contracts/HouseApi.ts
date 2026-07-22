import {NftApi} from "./NftApi";
import {IBlockchainConfig} from "../BlockchainConfig";
import * as Utility from "../Utility";
import IDependencies from "../services/IDependencies";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";

export class HouseApi extends NftApi {
  constructor(
    dep: IDependencies,
    config: IBlockchainConfig,
    blockchainCenterApi: BlockchainCenterApi
  ) {
    super(dep, config, blockchainCenterApi, config.bhouseTokenAddress, `${config.name}-house`);
  }

  async getDecodedDetails(options: any) {
    const detailList = await this.getDetails(options);
    const mode = options.mode;
    return detailList.map((details) => {
      if (details === '0') {
        throw Error(`Invalid details: ${JSON.stringify(detailList)}`);
      }
      const decodedDetails = Utility.decodeHouseDetails(details);
      return mode === 0
        ? Object.assign(
          {
            details,
            block_details: `${this._config.explorerUrl}/block/${decodedDetails.block_number}`,
            token_transfers: `${this._config.explorerUrl}/token/${this._config.bhouseTokenAddress}?a=${decodedDetails.id}`,
          },
          decodedDetails
        )
        : {details, id: decodedDetails.id};
    });
  }
}
