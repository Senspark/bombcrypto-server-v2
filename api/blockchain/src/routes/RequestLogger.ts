import {NextFunction, Request, Response} from "express";
import ILogger from "../services/ILogger";

export function createRequestLogger(logger: ILogger) {
  return function requestLogger(req: Request, res: Response, next: NextFunction) {
    const start = Date.now();
    logger.info(`--> ${req.method} ${req.originalUrl} ${JSON.stringify({query: req.query, body: req.body})}`);

    let logged = false;
    const logResponse = (body: any) => {
      if (logged) return;
      logged = true;
      const duration = Date.now() - start;
      logger.info(`<-- ${req.method} ${req.originalUrl} ${res.statusCode} ${duration}ms ${JSON.stringify(body)}`);
    };

    const originalJson = res.json.bind(res);
    res.json = (body: any) => {
      logResponse(body);
      return originalJson(body);
    };

    const originalSend = res.send.bind(res);
    res.send = (body: any) => {
      logResponse(body);
      return originalSend(body);
    };

    next();
  };
}
