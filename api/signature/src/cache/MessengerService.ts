import {RedisClientType} from 'redis';
import ILogger from '../services/ILogger';
import {IConfig} from '../Config';
import IMessengerService from './IMessengerService';
import getRedisClient from './Redis';

export default class MessengerService implements IMessengerService {
  private readonly _redis: RedisClientType;
  readonly #logger: ILogger;

  constructor(
    logger: ILogger,
    envConfig: IConfig,
  ) {
    this.#logger = logger.clone('[MSG]');
    this._redis = getRedisClient(envConfig.redisConnectionString);
  }

  async send(streamKey: string, message: any): Promise<boolean> {
    try {
      const res = await this._redis.xAdd(streamKey, '*', {data: JSON.stringify(message)});
      return res !== null;
    } catch (e) {
      this.#logger.error(e);
      return false;
    }
  }
}
