import ILogger from './services/ILogger';

const Reset = '\x1b[0m';
const FgRed = '\x1b[31m';
const FgGreen = '\x1b[32m';
const FgYellow = '\x1b[33m';

export default class ConsoleLogger implements ILogger {
  constructor(private readonly prefix: string) {}

  info(message: any): void {
    console.info(`${FgGreen}${this.stamp(message)}${Reset}`);
  }

  warn(message: any): void {
    console.warn(`${FgYellow}${this.stamp(message)}${Reset}`);
  }

  error(err: any): void {
    console.error(`${FgRed}${this.stamp(err instanceof Error ? err.message : err)}`);
    // The class and the cause chain, not just the message: on some runtimes the message is empty and
    // the bare text tells you nothing about what actually failed.
    if (err instanceof Error) {
      console.error(`${err.name}: ${err.stack}`);
      let cause = (err as Error & {cause?: unknown}).cause;
      while (cause instanceof Error) {
        console.error(`caused by ${cause.name}: ${cause.message}`);
        cause = (cause as Error & {cause?: unknown}).cause;
      }
    }
    console.info(Reset);
  }

  errors(...err: any[]): void {
    console.error(this.prefix, timeNow(), ...err);
  }

  clone(prefix: string): ILogger {
    return new ConsoleLogger(`${this.prefix}${prefix}`);
  }

  private stamp(message: any): string {
    return `${this.prefix} ${timeNow()} ${message}`;
  }
}

function timeNow(): string {
  return new Date().toISOString();
}
