export default interface ILogger {
  info(message: any): void;

  infos(...message: any[]): void;

  error(err: any): void;

  errors(...err: any[]): void;

  assert(condition: any, message: string): void;

  clone(prefix: string): ILogger;
}