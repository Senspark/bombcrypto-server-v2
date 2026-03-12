import {SimpleIntervalJob, Task, ToadScheduler} from "toad-scheduler";
import {ILogger} from "../../Services";
import IAutoExpireMap from "./IAutoExpireMap";

export default class LocalExpireMap<K, V> implements IAutoExpireMap<K, V> {

    constructor(
        private readonly _logger: ILogger,
        private readonly _scheduler: ToadScheduler,
    ) {
        const task = new Task('AutoExpireMap', this.autoMoveExpiredKeys.bind(this), (e: Error) => {
            this._logger.error(`Error: ${e.message}`);
        });
        const job = new SimpleIntervalJob({minutes: 1}, task);
        this._scheduler.addSimpleIntervalJob(job);
    }

    private readonly _map: Map<K, ExpireValue<V>> = new Map();

    add(key: K, value: V, expireSeconds: number): Promise<void> {
        this._map.set(key, new ExpireValue(value, Date.now() + expireSeconds * 1000));
        return Promise.resolve();
    }

    get(key: K): Promise<V | null> {
        const expireValue = this._map.get(key);
        if (!expireValue) {
            return Promise.resolve(null);
        }

        if (expireValue.expireTime < Date.now()) {
            this._map.delete(key);
            return Promise.resolve(null);
        }

        return Promise.resolve(expireValue.value);
    }

    extendExpireTime(key: K, expireSeconds: number): Promise<void> {
        const v = this._map.get(key);
        if (v) {
            this._map.set(key, new ExpireValue(v.value, Date.now() + expireSeconds * 1000));
        }
        return Promise.resolve();
    }

    remove(key: K): Promise<void> {
        this._map.delete(key);
        return Promise.resolve();
    }

    private autoMoveExpiredKeys() {
        this._map.forEach((value, key) => {
            if (value.expireTime < Date.now()) {
                this._map.delete(key);
            }
        });
    }
}

class ExpireValue<V> {
    constructor(
        public readonly value: V,
        public readonly expireTime: number,
    ) {
    }
}