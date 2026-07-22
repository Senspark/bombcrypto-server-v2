import {commandOptions, ErrorReply, RedisClientType} from "redis";
import ILogger from "../services/ILogger";
import {IConfig} from "../Config";
import IMessengerService from "./IMessengerService";
import getRedisClient from "./Redis";

const STREAM_CONSUMER_GROUP = 'ap-pvp-matching';
const STREAM_CONSUMER = 'ap-pvp-matching';
type CALL_BACK = (data: any) => void;

export default class MessengerService implements IMessengerService {
  private readonly _redis: RedisClientType;
  readonly #logger: ILogger;
  readonly #streamListeners = new Map<string, CALL_BACK[]>();

  constructor(
    logger: ILogger,
    envConfig: IConfig
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

  listen(streamKey: string, callback: (message: any) => void): void {
    if (!this.#streamListeners.has(streamKey)) {
      this.#streamListeners.set(streamKey, [callback]);
      this.createStreamListener(streamKey).then();
    } else {
      this.#streamListeners.get(streamKey)!!.push(callback);
    }
  }

  private async createStreamListener(streamKey: string) {
    const loop = true;
    const idPosition = '0';
    const blockTime = 100;
    const readMaxCount = 100;

    try {
      await this._redis.xGroupCreate(streamKey, STREAM_CONSUMER_GROUP, idPosition, {
        MKSTREAM: true
      });
    } catch (e) {
      if (e instanceof ErrorReply) {
        // ignore (group already exists)
      } else {
        this.#logger.error(e);
        return;
      }
    }

    while (loop) {
      try {
        const dataArr = await this._redis.xReadGroup(
          commandOptions({
            isolated: true,
          }),
          STREAM_CONSUMER_GROUP,
          STREAM_CONSUMER,
          [{
            key: streamKey,
            id: '>'
          }],
          {
            COUNT: readMaxCount,
            BLOCK: blockTime
          }
        );

        if (dataArr) {
          for (let data of (dataArr!!)) {
            for (let messageItem of data.messages) {
              const dataKey = messageItem.id;
              const dataValue = JSON.parse(messageItem.message.data);
              this.#streamListeners.get(streamKey)!!.forEach(callback => callback(dataValue));
              await this._redis.xAck(streamKey, STREAM_CONSUMER_GROUP, dataKey);
              // await this._redis.xDel(streamKey, dataKey)
            }
          }
        } else {
          // no new entries
        }
      } catch (e) {
        this.#logger.error(e);
      }
    }
  }
}