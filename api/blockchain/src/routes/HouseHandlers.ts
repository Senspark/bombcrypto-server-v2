import {IBlockchainApi} from "../IBlockchainApi";
import * as ErrorCode from "../ErrorCode";
import {Request, Response} from "express";
import {StreamKeys} from "../cache/CachedKeys";

export async function getHouseInfo(req: Request, res: Response) {
  const locals = res.locals;
  const blockchainApi: IBlockchainApi = locals.api;
  const messenger = locals.messenger;
  const logger = locals.logger;
  
  const query = req.query;
  const address = query.address;
  const id = query.id;
  const details = query.details;
  const mode = parseInt(query.mode as string ?? 0);
  const clearCache = query.clear != undefined && query.clear == "true";
  const userId = query.uid && parseInt(query.uid as string) as number;
  
  const result = await blockchainApi.getDecodedHouseDetails({
    address, id, details, mode, clearCache
  });
  res.send({
    code: ErrorCode.CODE_OK,
    message: result,
  });

  if (userId) {
    messenger.send(StreamKeys.AP_BL_SYNC_HOUSE, {
      uid: userId,
      type: (query.network as string)?.toUpperCase(),
      data: result,
    }).catch(e => {
      logger.error(e);
    });
  }
}

export async function getHouseDetails(req: Request, res: Response) {
  const locals = res.locals;
  const blockchainApi: IBlockchainApi = locals.api;
  const query = req.query;
  const address = query.address;
  const id = query.id !== undefined ? parseInt(query.id as string) : undefined;
  const clearCache = query.clear != undefined && query.clear == "true";
  const result = await blockchainApi.getHouseDetails({
    address, id, clearCache
  });
  res.send({
    code: ErrorCode.CODE_OK,
    message: result,
  });
}

export async function getHouseOwner(req: Request, res: Response) {
  const locals = res.locals;
  const blockchainApi: IBlockchainApi = locals.api;
  const query = req.query;
  const id = parseInt(query.id as string);
  const result = await blockchainApi.getHouseOwner(id);
  res.send({
    code: ErrorCode.CODE_OK,
    message: result,
  });
}