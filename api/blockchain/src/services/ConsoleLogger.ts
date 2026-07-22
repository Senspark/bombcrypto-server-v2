import ILogger from "../services/ILogger";
import {ValidationError} from "../consts/ServerError";

export default class ConsoleLogger implements ILogger {
  constructor(private readonly prefix: string) {
  }

  info(message: any): void {
    console.info(this.prefix, this.getCurrentTime(), message);
  }

  infos(...message: any[]): void {
    console.info(this.prefix, this.getCurrentTime(), ...message);
  }

  error(message: any): void {
    console.error(this.prefix, this.getCurrentTime(), message);
    if (message instanceof Error) {
      console.error(message.stack);
    }
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