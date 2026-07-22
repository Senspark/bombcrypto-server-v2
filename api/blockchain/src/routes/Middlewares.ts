import {NextFunction, Request, Response} from "express";
import * as ErrorCode from "../ErrorCode";
import {CHAIN_NAMES, normalizeNetwork} from "../BlockchainConfig";

export async function getNetworkMiddleware(req: Request, res: Response, next: NextFunction) {
  const dependencies = this.contextConfig.dependencies;
  const locals = res.locals;
  locals.config = dependencies.envConfig;
  locals.logger = dependencies.logger;
  locals.messenger = dependencies.messenger;

  const query = req.query;
  const rawNetwork = query.network as string | undefined;
  let network: string;
  if (!rawNetwork) {
    // Backward.
    if (req.baseUrl.indexOf(`polygon`) !== -1) {
      network = CHAIN_NAMES.POLYGON;
    } else {
      network = dependencies.envConfig.defaultNetwork;
    }
  } else {
    network = normalizeNetwork(rawNetwork) ?? dependencies.envConfig.defaultNetwork;
  }
  locals.api = this.contextConfig.apis.get(network);
  next();
}

export function errorHandlerMiddleware(err: any, req: Request, res: Response, next: NextFunction) {
  const locals = res.locals;
  const logger = locals.logger;
  logger.error(err.message, err.stack?.split(`\n`));
  res.status(err.httpCode ?? 500).send({
    code: err.code ?? ErrorCode.CODE_INTERNAL,
    message: err.message,
  });
}