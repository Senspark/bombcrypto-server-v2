import {ILogger} from "../../Services";
import {ValidationError} from "../../consts/ServerError";

export default class CombineLogger implements ILogger {
    constructor(
        private readonly _prefix: string,
        private readonly _logger: ILogger,
    ) {
    }

    private _logs: string[] = [];

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