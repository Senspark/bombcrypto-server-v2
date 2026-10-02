export default interface ILogger {
  info(message: any): void;

  warn(message: any): void;

  /** Logs the message, and the stack too when given an Error. */
  error(err: any): void;

  errors(...err: any[]): void;

  /** A child logger whose lines carry `prefix` — one tag per subsystem, e.g. [STREAM], [SIGN]. */
  clone(prefix: string): ILogger;
}
