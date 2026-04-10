import ILogger from "../services/ILogger";

export default class NullLogger implements ILogger {
    assert(condition: any, message: string): void {
    }

    clone(prefix: string): ILogger {
        throw new Error("Method not implemented.");
    }

    error(err: any): void {
    }

    errors(...err: any[]): void {
    }

    info(message: any): void {
    }

    infos(...message: any[]): void {
    }


}