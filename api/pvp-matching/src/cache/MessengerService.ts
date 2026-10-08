import {RedisClientType} from "redis";
import ILogger from "../services/ILogger";
import {IConfig} from "../Config";
import IMessengerService from "./IMessengerService";
import getRedisClient from "./Redis";

type CALL_BACK = (data: any) => void;

export default class MessengerService implements IMessengerService {
    readonly _logger: ILogger;
    private readonly _redis: RedisClientType;
    // A subscribed client can not run other commands, so the bus gets its own connection.
    private readonly _subscriber: RedisClientType;
    private readonly _subscriberReady: Promise<void>;
    private readonly _busListeners = new Map<string, CALL_BACK[]>();
    private readonly _busChannels = new Set<string>();

    constructor(
        logger: ILogger,
        envConfig: IConfig,
    ) {
        this._logger = logger.clone('[MSG]');
        this._redis = getRedisClient(envConfig.redisConnectionString);
        this._subscriber = this._redis.duplicate();
        this._subscriber.on('error', e => this._logger.error(e));
        this._subscriberReady = this._subscriber.connect().then();
    }

    async publishBus(channel: string, type: string, message: any): Promise<boolean> {
        try {
            await this._redis.publish(channel, JSON.stringify({type, data: JSON.stringify(message)}));
            return true;
        } catch (e) {
            this._logger.error(e);
            return false;
        }
    }

    onBus(channel: string, type: string, callback: CALL_BACK): void {
        const key = `${channel}:${type}`;
        this._busListeners.set(key, [...(this._busListeners.get(key) ?? []), callback]);
        if (this._busChannels.has(channel)) {
            return;
        }
        this._busChannels.add(channel);
        this._subscriberReady
            .then(() => this._subscriber.subscribe(channel, raw => this.dispatchBus(channel, raw)))
            .catch(e => this._logger.error(e));
    }

    private dispatchBus(channel: string, raw: string) {
        let type: string;
        let data: any;
        try {
            const envelope = JSON.parse(raw);
            type = envelope.type;
            data = JSON.parse(envelope.data);
        } catch (e) {
            this._logger.error(`Bad message on ${channel}: ${raw}`);
            return;
        }
        for (const callback of this._busListeners.get(`${channel}:${type}`) ?? []) {
            try {
                callback(data);
            } catch (e) {
                this._logger.error(e);
            }
        }
    }
}
