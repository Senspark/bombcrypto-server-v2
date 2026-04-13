import {ValidationError} from "../../consts/ServerError";
import ILogger from "../ILogger";

export default class CombineLogger implements ILogger {
    private _logs: string[] = [];

    constructor(
        private readonly _prefix: string,
        private readonly _logger: ILogger,
    ) {
    }

    info(message: any): void {
        this._logs.push(message)
    }

    infos(...message: any[]): void {
        this._logs.push(...message)
    }

    error(err: any): void {
        this._logs.push(err)
    }

    errors(...err: any[]): void {
        this._logs.push(...err)
    }

    assert(condition: any, message: string): void {
        if (!condition) {
            this._logs.push(message)
            throw new ValidationError(message);
        }
    }

    clone(prefix: string): ILogger {
        return new CombineLogger(prefix, this._logger);
    }

    dump() {
        this._logger.info(`${this._prefix} ${this._logs.join(' ++ ')}`)
    }
}