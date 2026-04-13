import {Response} from 'express';
import ILogger from "../services/ILogger";

declare module 'express-serve-static-core' {
    interface Response {
        sendSuccess(data: any): void;

        sendError(message: string, errCode?: number): void;

        sendGenericError(): void;
    }
}

declare module 'express-session' {
    interface SessionData {
        redirectAfterLogin?: string;
        hasPermission: boolean;
        userName: string;
    }
}

export {};

export default function extendResponse(logger: ILogger, res: Response) {
    res.sendSuccess = function (data: any) {
        const result: IResponse = {
            success: true,
            error: "",
            message: data
        };
        this.status(200).json(result)
    };

    res.sendError = function (message: string, errCode?: number) {
        const result: IResponse = {
            success: false,
            error: message,
            message: ""
        };
        logger.error(message);
        this.status(errCode || 400).send(result)
    }

    res.sendGenericError = function () {
        const result: IResponse = {
            success: false,
            error: "Something wrong",
            message: ""
        };
        this.status(400).send(result)
    }
}

export interface IResponse {
    success: boolean;
    error: string;
    message: string;
}