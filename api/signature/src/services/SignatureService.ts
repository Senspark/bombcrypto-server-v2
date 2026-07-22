import {BigNumber, ethers} from 'ethers';
import {BadRequestError} from '../Error';
import {ValidateApi} from '../apis/ValidateApi';

export class SignatureService {
  constructor(
    private readonly _validateApi: ValidateApi,
  ) {
  }

  /** @note Only works on BSC. */
  async claimAirdrop(userAddress: string, numClaim: number, eventId: number) {
    if (!(userAddress && numClaim.toString() && eventId.toString())) {
      throw new BadRequestError('Bad request');
    }
    if (await this._validateApi.checkUserHasClaimed(userAddress, eventId)) {
      throw new BadRequestError('User has claimed');
    }
    const listNFTExisted = await this._validateApi.checkExistedNFTAirdrop();
    if (!(numClaim > 0 && numClaim <= listNFTExisted.length)) {
      throw new BadRequestError('NFT not available');
    }

    const userNonce = await this._validateApi.getTransactionCount(userAddress);
    return {
      nonce: userNonce,
      signature: await this._validateApi.genSignatureByEcrec(
        ['address', 'uint256', 'uint256', 'uint256', 'address'],
        [userAddress, numClaim, eventId, userNonce, this._validateApi.claimAirdropAddress],
      ),
    };
  }

  async claimToken(
    userAddress: string,
    tokenType: number,
    amount: BigNumber,
    details: string[],
  ) {
    if (!(userAddress && tokenType.toString() && amount)) {
      throw new BadRequestError('Bad request');
    }
    const userNonce = await this._validateApi.getTransactionCount(userAddress);
    const totalClaimed = await this._validateApi.getTotalClaimedToken(userAddress, tokenType);
    return {
      nonce: userNonce,
      amount: ethers.utils.formatEther(amount.toString()),
      signature: await this._validateApi.genSignatureByEcrec(
        ['address', 'uint256', 'uint256', 'uint256', 'string', 'uint256', 'address'],
        [
          userAddress,
          tokenType,
          amount,
          userNonce,
          details.join(''),
          totalClaimed.toString(),
          this._validateApi.claimManageAddress,
        ],
      ),
    };
  }

  async upgradeHeroShieldLevel(userAddress: string, heroId: number) {
    if (!userAddress || !heroId) {
      throw new BadRequestError('Bad request');
    }
    const userNonce = await this._validateApi.getTransactionCount(userAddress);
    return {
      nonce: userNonce,
      signature: await this._validateApi.genSignatureByEcrec(
        ['address', 'uint256', 'uint256'],
        [userAddress, heroId, userNonce],
      ),
    };
  }
}
