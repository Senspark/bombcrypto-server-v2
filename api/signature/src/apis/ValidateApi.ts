import {BigNumber, ethers} from 'ethers';
import {
  CHECK_EXISTED_NFT_AIRDROP_ABI,
  CHECK_USER_CLAIM_ABI,
  ERC721_BALANCE_OF_ABI,
  GET_TOTAL_CLAIM_ABI,
} from '../Abi';
import {CHAIN_NAMES, IBlockchainConfig} from '../BlockchainConfig';
import {BadRequestError} from '../Error';
import IDependencies from '../services/IDependencies';
import {hashMessage} from '../Utility';
import {BlockchainCenterApi} from '../providers/BlockchainCenterApi';

export class ValidateApi {
  private readonly _network: string;
  private readonly _config: IBlockchainConfig;
  private readonly _center: BlockchainCenterApi;
  private readonly _deployer: ethers.Wallet;

  get claimAirdropAddress(): string {
    return this._config.claimAirdropAddress;
  }

  get claimManageAddress(): string {
    return this._config.claimManageAddress;
  }

  get network(): string {
    return this._network;
  }

  constructor(dep: IDependencies, config: IBlockchainConfig, center: BlockchainCenterApi) {
    this._network = config.name;
    this._config = config;
    this._center = center;
    // The signer only needs the private key — signMessage runs locally, no RPC needed.
    this._deployer = new ethers.Wallet(dep.envConfig.privateKey);
  }

  async genSignatureByEcrec(types: string[], values: unknown[]) {
    const message = hashMessage(types, values);
    return this._deployer.signMessage(message);
  }

  async getTransactionCount(userAddress: string): Promise<number> {
    return this._center.getTransactionCount(this._network, userAddress);
  }

  /** @note Only works on BSC. */
  async checkUserHasClaimed(userAddress: string, eventId: number): Promise<boolean> {
    if (!(userAddress && eventId.toString())) {
      throw new BadRequestError('Bad request');
    }
    return await this._center.callContract(
      this._network,
      this._config.claimAirdropAddress,
      CHECK_USER_CLAIM_ABI,
      'checkUserClaim',
      [userAddress, eventId],
    );
  }

  /** @note Only works on BSC. */
  async checkExistedNFTAirdrop(): Promise<unknown[]> {
    const result = await this._center.callContract(
      this._network,
      this._config.claimAirdropAddress,
      CHECK_EXISTED_NFT_AIRDROP_ABI,
      'checkExistedNFTAirdrop',
      [],
    );
    return Array.isArray(result) ? result : [];
  }

  /** @note Only works on Polygon. */
  async getTotalClaimedToken(userAddress: string, tokenType: number): Promise<BigNumber> {
    if (!(userAddress && tokenType.toString())) {
      throw new BadRequestError('Bad request');
    }
    const result = await this._center.callContract(
      this._network,
      this._config.claimManageAddress,
      GET_TOTAL_CLAIM_ABI,
      'getTotalClaim',
      [userAddress, tokenType],
    );
    return BigNumber.from(result);
  }

  /** @note Only works on Polygon. Reads BHero balance on BSC via the center api. */
  async userHasEnoughHero(userAddress: string): Promise<boolean> {
    const raw = await this._center.callContract(
      CHAIN_NAMES.BNB,
      this._config.bheroBscAddress,
      ERC721_BALANCE_OF_ABI,
      'balanceOf',
      [userAddress],
    );
    const heroUserBalance = BigNumber.from(raw ?? 0);
    return heroUserBalance.gte(BigNumber.from(15));
  }

  async recoverAddress(message: string, hash: string): Promise<string> {
    return ethers.utils.verifyMessage(message, hash);
  }
}
