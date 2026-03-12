import {Request, Response} from "express";
import {ILogger} from "../Services";

const pendingRequests = new Set();
let total = 0;

export function recordRequest(req: Request, res: Response, next: any) {
    const requestId = `${req.url} ${Date.now()}`;
    pendingRequests.add(requestId);
    total++;

    res.on('finish', () => {
        pendingRequests.delete(requestId);
    });

    res.on('close', () => {
        pendingRequests.delete(requestId);
    });

    next();
}

export function monitorPendingRequests(logger: ILogger) {
    const log = () => {
        // logger.info(`Pending requests: ${pendingRequests.size}/${total}`);
    };

    setInterval(log, 10_000);
}