import {NextFunction, Request, Response} from "express";
import ILogger from "../services/ILogger";

export function errorHandlerMiddleware(logger: ILogger, err: any, res: Response) {
    logger.errors(err.message, err.stack?.split(`\n`));
    res.sendError(err.message, err.httpCode ?? 500);
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