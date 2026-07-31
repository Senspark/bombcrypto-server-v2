import {commandOptions, RedisClientType} from 'redis';
import ILogger from '../services/ILogger';
import IMessengerService from '../services/IMessengerService';
import getRedisClient from './Redis';

const BLOCK_TIME = 200;
const READ_MAX_COUNT = 100;

type CALL_BACK = (data: any) => void;

/**
 * Redis-stream transport, same shape as ap-deposit-bridge: non-group XREAD from the latest id, one
 * blocking read loop per stream key.
 */
export default class MessengerService implements IMessengerService {
  private readonly _redis: RedisClientType;
  private readonly _logger: ILogger;
  private readonly _streamListeners = new Map<string, CALL_BACK[]>();

  constructor(logger: ILogger, redisConnectionString: string) {
    this._logger = logger.clone('[MSG]');
    this._redis = getRedisClient(redisConnectionString, logger);
  }

  async send(streamKey: string, message: any): Promise<boolean> {
    try {
      const res = await this._redis.xAdd(streamKey, '*', {data: JSON.stringify(message)});
      return res !== null;
    } catch (e) {
      // A dropped reply leaves the game server's request to time out — say which stream and which
      // correlation, or the two sides can't be lined up afterwards.
      this._logger.errors(`send failed stream=${streamKey} cid=${message?.correlationId ?? '?'}`, e);
      return false;
    }
  }

  listen(streamKey: string, callback: CALL_BACK): void {
    const existing = this._streamListeners.get(streamKey);
    if (existing) {
      existing.push(callback);
      return;
    }
    this._streamListeners.set(streamKey, [callback]);
    this._logger.info(`listening on ${streamKey}`);
    this.createStreamListener(streamKey).then();
  }

  private async createStreamListener(streamKey: string) {
    const options: IStreamReadOptions = {currentId: await this.getLatestId(streamKey)};
    this._logger.info(`${streamKey} starting from id=${options.currentId}`);

    // Deliberately unbounded: the read blocks for BLOCK_TIME and loops. An error here (Redis down)
    // must not end the loop, or the service goes silently deaf until it is restarted.
    for (;;) {
      try {
        await this.awaitNewMessages(streamKey, options);
      } catch (e) {
        this._logger.errors(`read loop error stream=${streamKey} lastId=${options.currentId}`, e);
      }
    }
  }

  private async awaitNewMessages(streamKey: string, options: IStreamReadOptions) {
    const dataArr = await this._redis.xRead(
      commandOptions({isolated: true}),
      [{key: streamKey, id: options.currentId}],
      {COUNT: READ_MAX_COUNT, BLOCK: BLOCK_TIME},
    );
    if (dataArr) {
      await this.consumeData(dataArr as unknown as XReadReply[], options);
    }
  }

  private async consumeData(dataArr: XReadReply[], options: IStreamReadOptions) {
    for (const data of dataArr) {
      for (const messageItem of data.messages) {
        options.currentId = messageItem.id;
        const consumers = this._streamListeners.get(data.name);
        if (!consumers) {
          continue;
        }
        let payload: any;
        try {
          payload = JSON.parse(messageItem.message.data);
        } catch (e) {
          this._logger.errors(`undecodable message stream=${data.name} id=${messageItem.id}`, e);
          continue;
        }
        for (const consumer of consumers) {
          try {
            consumer(payload);
          } catch (e) {
            // A throwing consumer must not kill the read loop or skip the rest of the batch.
            this._logger.errors(`consumer threw stream=${data.name} id=${messageItem.id}`, e);
          }
        }
      }
    }
  }

  private async getLatestId(streamKey: string) {
    try {
      const streamInfo = await this._redis.xInfoStream(streamKey);
      return streamInfo ? streamInfo.lastGeneratedId : '0-0';
    } catch (e) {
      // The stream does not exist until someone writes to it — start from the beginning.
      return '0-0';
    }
  }
}

interface XReadReply {
  name: string;
  messages: {
    id: string;
    message: {[key: string]: string};
  }[];
}

interface IStreamReadOptions {
  currentId: string;
}
