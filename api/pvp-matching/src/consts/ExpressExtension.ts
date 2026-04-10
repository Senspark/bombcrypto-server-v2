import {Response} from 'express';
import ILogger from "../services/ILogger";
import {IErrorResponseSchema, ISuccessResponseSchema} from "./ResponseSchema";

declare module 'express-serve-static-core' {
    interface Response {
        sendSuccess(data: any): void;

        sendError(message: string, errCode?: number): void;

        sendGenericError(): void;
    }
}

export default function extendResponse(logger: ILogger, res: Response) {
    res.sendSuccess = function (data: any) {
        const result: ISuccessResponseSchema = {
            status: 'success',
            data: data,
            message: null
        };
        res.send(result);
    };

    res.sendError = function (message: string, errCode?: number) {
        const result: IErrorResponseSchema = {
            status: 'error',
            error: {
                code: errCode ?? 500,
                message: message ?? 'Something went wrong.'
            }
        };
        this.status(errCode ?? 500).send(result)
    }

    res.sendGenericError = function (errCode?: number) {
        const result: IErrorResponseSchema = {
            status: 'error',
            error: {
                code: errCode ?? 500,
                message: 'Something went wrong.'
            }
        };
        this.status(errCode ?? 500).send(result)
    }
}