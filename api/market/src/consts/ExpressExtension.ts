import {NextFunction, Request, Response} from 'express';

declare module 'express-serve-static-core' {
    interface Response {
        sendRawJson(data: any): void;

        sendSuccess(data: any): void;

        sendSuccessWithHeaders(data: any, additionHeaders: Record<string, string> | null): void;

        sendError(err: string, statusCode?: number): void;

        sendGenericError(): void;
    }
}

export default function extendResponse(res: Response) {
    res.sendRawJson = (data: any) => {
        res.setHeader('Content-Type', 'application/json');
        res.status(200).json(data);
    }

    res.sendSuccessWithHeaders = function (data: any, additionHeaders: Record<string, string> | null = null) {
        const result: IResponse = {
            success: true,
            error: "",
            message: data
        };
        this.setHeader('Content-Type', 'application/json');
        try {
            if (additionHeaders) {
                for (const key in additionHeaders) {
                    this.setHeader(key, additionHeaders[key]);
                }
            }
        } catch (e) {
            console.error(e);
        }
        this.status(200).json(result)
    };

    res.sendSuccess = (data: any) => res.sendSuccessWithHeaders(data, null);

    res.sendError = function (err: string, statusCode?: number) {
        const result: IResponse = {
            success: false,
            error: err,
            message: null
        };
        this.setHeader('Content-Type', 'application/json');
        this.status(statusCode || 400).send(result)
    }

    res.sendGenericError = function () {
        res.sendError("Something Wrong", 400);
    }
}

export function lowercaseQuery(req: Request, res: Response, next: NextFunction) {
    req.query = Object.keys(req.query).reduce((acc, key) => {
        acc[key.toLowerCase()] = req.query[key];
        return acc;
    }, {} as { [key: string]: any });
    next();
}

export interface IResponse {
    success: boolean;
    error: {};
    message: string | null;
}