import {RedisClientType} from "redis";
import ILogger from "../services/ILogger";

export interface ICachedSnapshot<T> {
    data: T;
    fetchedAt: number; // epoch ms the snapshot was read from chain
}

// Redis-backed snapshot cache for the slow on-chain monitor reads. A page load serves whatever is cached; the
// "Refresh" button forces a re-read that overwrites it. `ttlMs` is a safety expiry so data can't go infinitely
// stale if nobody refreshes (0 = keep forever). Every Redis op is best-effort: a Redis failure degrades to a
// live chain read rather than an error.
export default class MonitorCache {
    readonly #redis: RedisClientType;
    readonly #logger: ILogger;
    readonly #ttlMs: number;

    constructor(redis: RedisClientType, logger: ILogger, ttlMs: number) {
        this.#redis = redis;
        this.#logger = logger.clone('[MonitorCache]');
        this.#ttlMs = ttlMs;
    }

    async read<T>(key: string): Promise<ICachedSnapshot<T> | null> {
        try {
            const raw = await this.#redis.get(key);
            return raw ? (JSON.parse(raw) as ICachedSnapshot<T>) : null;
        } catch (e) {
            this.#logger.error(e);
            return null; // treat as a miss → caller reads the chain
        }
    }

    async write<T>(key: string, snapshot: ICachedSnapshot<T>): Promise<void> {
        try {
            const payload = JSON.stringify(snapshot);
            if (this.#ttlMs > 0) {
                await this.#redis.set(key, payload, {PX: this.#ttlMs});
            } else {
                await this.#redis.set(key, payload);
            }
        } catch (e) {
            this.#logger.error(e);
        }
    }

    // Drop a cached key so the next read recomputes it. Used by the sweep to invalidate just the (chain, token)
    // cell that changed. Best-effort: a Redis failure only means the stale snapshot lingers until its TTL.
    async delete(key: string): Promise<void> {
        try {
            await this.#redis.del(key);
        } catch (e) {
            this.#logger.error(e);
        }
    }

    // Immutable numerous values (e.g. block timestamps) — kept in ONE hash per group instead of one key each,
    // so Redis isn't flooded with keys. No TTL (immutable). Best-effort like the rest.
    async hGet(key: string, field: string): Promise<string | null> {
        try {
            return (await this.#redis.hGet(key, field)) ?? null;
        } catch (e) {
            this.#logger.error(e);
            return null;
        }
    }

    async hSet(key: string, field: string, value: string): Promise<void> {
        try {
            await this.#redis.hSet(key, field, value);
        } catch (e) {
            this.#logger.error(e);
        }
    }
}
