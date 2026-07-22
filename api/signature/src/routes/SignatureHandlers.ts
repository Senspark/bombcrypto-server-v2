import {ethers} from 'ethers';
import {Request, Response} from 'express';
import {BadRequestError} from '../Error';
import {SignatureService} from '../services/SignatureService';

export default class SignatureHandlers {
  async claimAirdrop(req: Request, res: Response) {
    const service = res.locals.signatureService as SignatureService;
    const userAddress = req.query.userAddress as string;
    const numClaim = Number.parseInt(req.query.numClaim as string);
    const eventId = Number.parseInt(req.query.eventId as string);
    const data = await service.claimAirdrop(userAddress, numClaim, eventId);
    res.sendSuccess(data);
  }

  async claimToken(req: Request, res: Response) {
    const service = res.locals.signatureService as SignatureService;
    const userAddress = req.body.userAddress as string;
    const tokenType: number = req.body.tokenType;
    if (req.body.amount === undefined || req.body.amount === null) {
      throw new BadRequestError('amount is required');
    }
    const amount = ethers.utils.parseEther(req.body.amount.toString());
    const details: string[] = req.body.details || [];
    const data = await service.claimToken(userAddress, tokenType, amount, details);
    res.sendSuccess(data);
  }

  async upgradeHeroShield(req: Request, res: Response) {
    const service = res.locals.signatureService as SignatureService;
    const walletAddress = req.body.wallet_address as string;
    const heroId = req.body.hero_id as number;
    const data = await service.upgradeHeroShieldLevel(walletAddress, heroId);
    res.sendSuccess(data);
  }

}
