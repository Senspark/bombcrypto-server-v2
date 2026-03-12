import {Request, Response, NextFunction} from "express";

export function requestLogger(req: Request, res: Response, next: NextFunction) {
    const start = Date.now();
    console.log(`--> ${req.method} ${req.url}`, {
        body: req.body,
        query: req.query,
    });

    let logged = false;
    const logResponse = (body: any) => {
        if (logged) return;
        logged = true;
        const duration = Date.now() - start;
        console.log(`<-- ${req.method} ${req.url} ${res.statusCode} ${duration}ms`, body);
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
}
