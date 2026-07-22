import {ethers} from 'ethers';
import {Request, Response} from 'express';
import * as ErrorCode from '../ErrorCode';
import {ValidateApi} from '../apis/ValidateApi';
import {SignatureService} from '../services/SignatureService';
import IMessengerService from '../cache/IMessengerService';
import {StreamKeys} from '../cache/StreamKeys';
import ILogger from '../services/ILogger';

export default class ClaimV4Handlers {
  constructor(
    private readonly _messenger: IMessengerService,
    logger: ILogger,
  ) {
    this._logger = logger.clone('[CLAIM_V4]');
  }

  private readonly _logger: ILogger;

  async checkTotalClaimV4(req: Request, res: Response) {
    const api = res.locals.validateApi as ValidateApi;
    const userAddress = req.query.userAddress as string;
    const tokenType = Number.parseInt(req.query.tokenType as string);
    const correlationId = req.query.correlationId as string;

    if (!correlationId || !userAddress || Number.isNaN(tokenType)) {
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: 'correlationId, userAddress, tokenType required'});
      return;
    }
    res.send({code: ErrorCode.CODE_OK});

    api.getTotalClaimedToken(userAddress, tokenType)
      .then(totalClaim => {
        const totalClaimed = totalClaim.gte(ethers.utils.parseUnits('1', 12).toString())
          ? Number.parseFloat(ethers.utils.formatEther(totalClaim.toString()))
          : Number.parseFloat(totalClaim.toString());
        return this._messenger.send(StreamKeys.AP_SIG_CLAIM_CHECK_RESPONSE_STR, {
          correlationId,
          totalClaimed,
        });
      })
      .catch(async e => {
        this._logger.errors(`[check] cid=${correlationId}`, e);
        await this._messenger.send(StreamKeys.AP_SIG_CLAIM_CHECK_RESPONSE_STR, {
          correlationId,
          totalClaimed: 0,
          errorMessage: e?.message ?? 'check failed',
        }).catch(err => this._logger.errors(`[check] publish-error cid=${correlationId}`, err));
      });
  }

  async signClaimV4(req: Request, res: Response) {
    const service = res.locals.signatureService as SignatureService;
    const correlationId = req.query.correlationId as string;
    const userAddress = req.body.userAddress as string;
    const tokenType: number = req.body.tokenType;
    const newTotalClaimedRaw = req.body.newTotalClaimed;
    const details: string[] = req.body.giftDetails || [];

    if (!correlationId || !userAddress || tokenType === undefined || newTotalClaimedRaw === undefined) {
      res.status(400).send({code: ErrorCode.CODE_INTERNAL, message: 'missing required fields'});
      return;
    }
    res.send({code: ErrorCode.CODE_OK});

    const amount = ethers.utils.parseEther(newTotalClaimedRaw.toString());
    service.claimToken(userAddress, tokenType, amount, details)
      .then(result => this._messenger.send(StreamKeys.AP_SIG_CLAIM_SIGN_RESPONSE_STR, {
        correlationId,
        nonce: result.nonce,
        signature: result.signature,
        amount: Number.parseFloat(result.amount),
      }))
      .catch(async e => {
        this._logger.errors(`[sign] cid=${correlationId}`, e);
        await this._messenger.send(StreamKeys.AP_SIG_CLAIM_SIGN_RESPONSE_STR, {
          correlationId,
          nonce: 0,
          signature: '',
          amount: 0,
          errorMessage: e?.message ?? 'sign failed',
        }).catch(err => this._logger.errors(`[sign] publish-error cid=${correlationId}`, err));
      });
  }
}
