import {Request, Response, NextFunction} from "express";
import {ILogger} from "../Services";
import {DeferLogger} from "../services-impl/loggers/DeferLogger";

const SKIP_PATH_KEYWORDS = ['check_server', 'refresh'];

function shouldSkip(req: Request): boolean {
    if (req.method === 'OPTIONS') return true;
    const path = req.path.toLowerCase();
    return SKIP_PATH_KEYWORDS.some(keyword => path.includes(keyword));
}

function stringify(value: any): string {
    if (value === undefined) return '';
    if (typeof value === 'string') return value;
    try {
        return JSON.stringify(value);
    } catch {
        return String(value);
    }
}

export function createRequestLogger(logger: ILogger) {
    // DeferLogger makes every emit fire-and-forget so logging stays off the
    // request's critical path and can never break it.
    const deferred = new DeferLogger(logger);

    return function requestLogger(req: Request, res: Response, next: NextFunction) {
        if (shouldSkip(req)) {
            return next();
        }

        const start = Date.now();

        // Cheap interception: only capture the response body reference here,
        // keep the critical path free of any stringify/log work.
        let responseBody: any;
        const originalJson = res.json.bind(res);
        res.json = (body: any) => {
            responseBody = body;
            return originalJson(body);
        };
        const originalSend = res.send.bind(res);
        res.send = (body: any) => {
            responseBody = body;
            return originalSend(body);
        };

        // Log only after the response is fully flushed to the client.
        res.on('finish', () => {
            const duration = Date.now() - start;
            const requestMeta = stringify({body: req.body, query: req.query});
            const responseMeta = stringify(responseBody);

            deferred.info(`--> ${req.method} ${req.url} ${requestMeta}`);
            deferred.info(`<-- ${req.method} ${req.url} ${res.statusCode} ${duration}ms ${responseMeta}`);
        });

        next();
    };
}
