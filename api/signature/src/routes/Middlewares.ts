import {NextFunction, Request, Response} from 'express';
import * as ErrorCode from '../ErrorCode';
import {CHAIN_NAMES} from '../BlockchainConfig';

export async function getNetworkMiddleware(req: Request, res: Response, next: NextFunction) {
  const dependencies = this.contextConfig.dependencies;
  const locals = res.locals;
  locals.config = dependencies.envConfig;
  locals.logger = dependencies.logger;

  const query = req.query;
  let network = query.network as string;
  if (!network) {
    // Backward.
    if (req.baseUrl.indexOf(`polygon`) !== -1) {
      network = CHAIN_NAMES.POLYGON;
    } else {
      network = dependencies.envConfig.defaultNetwork;
    }
  } else {
    switch (network.toLowerCase()) {
      case 'bsc':
      case 'bnb':
        network = CHAIN_NAMES.BNB;
        break;
      case 'polygon':
        network = CHAIN_NAMES.POLYGON;
        break;
      default:
        network = dependencies.envConfig.defaultNetwork;
        break;
    }
  }
  locals.signatureService = this.contextConfig.signatureServices.get(network);
  locals.validateApi = this.contextConfig.validateApis.get(network);
  next();
}

export function errorHandlerMiddleware(err: any, req: Request, res: Response, next: NextFunction) {
  const locals = res.locals;
  const logger = locals.logger;
  logger?.error(err.message);
  logger?.error(err.stack?.split(`\n`));
  res.status(err.httpCode ?? 500).send({
    code: err.code ?? ErrorCode.CODE_INTERNAL,
    message: err.message,
  });
}

export function checkBearer(req: Request, res: Response, next: NextFunction) {
  const jwtService = this.contextConfig.jwtService;
  const token = req.headers.authorization;
  if (!token || !jwtService.verify(token)) {
    res.sendStatus(401);
    return;
  }
  next();
}
