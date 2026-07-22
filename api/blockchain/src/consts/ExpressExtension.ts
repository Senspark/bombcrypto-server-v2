import {Response} from 'express';
import ILogger from "../services/ILogger";
import * as ErrorCode from "../ErrorCode";

declare module 'express-serve-static-core' {
  interface Response {
    sendSuccess(data: any): void;

    sendError(message: string, errCode?: number): void;

    sendGenericError(): void;
  }
}

export default function extendResponse(logger: ILogger, res: Response) {
  res.sendSuccess = function (data: any) {
    const result: IResponse = {
      code: ErrorCode.CODE_OK,
      message: data
    };
    res.send(result);
  };

  res.sendError = function (message: string, errCode?: number) {
    const result: IResponse = {
      code: errCode ?? ErrorCode.CODE_INTERNAL,
      message: message ?? 'ERROR'
    };
    this.status(400).send(result)
  }

  res.sendGenericError = function () {
    const result: IResponse = {
      code: ErrorCode.CODE_INTERNAL,
      message: 'ERROR'
    };
    this.status(400).send(result)
  }
}

export interface IResponse {
  code: number;
  message: string;
}