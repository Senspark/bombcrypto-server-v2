import {ethers} from 'ethers';
import {Request, Response} from 'express';
import {ValidateApi} from '../apis/ValidateApi';

export default class ValidateHandlers {
  async airdropUser(req: Request, res: Response) {
    const api = res.locals.validateApi as ValidateApi;
    const userAddress = req.query.userAddress as string;
    const eventId = parseInt(req.query.eventId as string);
    const data = await api.checkUserHasClaimed(userAddress, eventId);
    res.sendSuccess(data);
  }

  async checkTotalClaimToken(req: Request, res: Response) {
    const api = res.locals.validateApi as ValidateApi;
    const userAddress = req.query.userAddress as string;
    const tokenType = Number.parseInt(req.query.tokenType as string);
    const totalClaim = await api.getTotalClaimedToken(userAddress, tokenType);
    const data = totalClaim.gte(ethers.utils.parseUnits('1', 12).toString())
      ? ethers.utils.formatEther(totalClaim.toString())
      : totalClaim.toString();
    res.sendSuccess(data);
  }

  async checkHero1Balance(req: Request, res: Response) {
    const api = res.locals.validateApi as ValidateApi;
    const userAddress = req.query.userAddress as string;
    const data = await api.userHasEnoughHero(userAddress);
    res.sendSuccess(data);
  }

  async recoverAddress(req: Request, res: Response) {
    const api = res.locals.validateApi as ValidateApi;
    const hash = req.body.hash as string;
    const message = req.body.message as string;
    const data = await api.recoverAddress(message, hash);
    res.sendSuccess(data);
  }
}
