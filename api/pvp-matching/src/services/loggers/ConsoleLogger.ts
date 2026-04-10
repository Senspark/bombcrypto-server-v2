import {ValidationError} from "../../consts/ServerError";
import ILogger from "../ILogger";

const Reset = "\x1b[0m";
const FgBlack = "\x1b[30m"
const FgRed = "\x1b[31m"
const FgGreen = "\x1b[32m"
const FgYellow = "\x1b[33m"
const FgBlue = "\x1b[34m"
const FgMagenta = "\x1b[35m"
const FgCyan = "\x1b[36m"
const FgWhite = "\x1b[37m"
const FgGray = "\x1b[90m"

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