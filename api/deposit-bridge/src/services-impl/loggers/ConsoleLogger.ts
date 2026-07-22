import ILogger from "../../services/ILogger";
import {ValidationError} from "../../consts/ServerError";

const Reset = "\x1b[0m";
const FgRed = "\x1b[31m"
const FgGreen = "\x1b[32m"
const FgYellow = "\x1b[33m"

export default class ConsoleLogger implements ILogger {
    constructor(private readonly prefix: string) {
    }

    info(message: any): void {
        const m = `${this.prefix} ${this.getCurrentTime()} ${message}`
        console.info(`${FgGreen}${m}${Reset}`);
    }

    infos(...message: any[]): void {
        const m = `${this.prefix} ${this.getCurrentTime()}\n${message.join('\n')}`
        console.info(`${FgGreen}${m}${Reset}`);
    }

    warn(message: any): void {
        const m = `${this.prefix} ${this.getCurrentTime()} ${message}`
        console.warn(`${FgYellow}${m}${Reset}`);
    }

    error(message: any): void {
        const m = `${this.prefix} ${this.getCurrentTime()} ${message}`
        console.error(`${FgRed}${m}`);
        if (message instanceof Error) {
            console.error(message.stack);
        }
        console.info(Reset);
    }

    errors(...err: any[]): void {
        console.error(this.prefix, this.getCurrentTime(), ...err);
    }

    assert(condition: any, message: string): void {
        if (!condition) {
            console.error(this.prefix, this.getCurrentTime(), message);
            throw new ValidationError(message);
        }
    }

    clone(prefix: string): ILogger {
        return new ConsoleLogger(prefix);
    }

    private getCurrentTime() {
        return new Date().toLocaleTimeString();
    }
}
