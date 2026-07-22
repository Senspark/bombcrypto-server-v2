import ILogger from "../services/ILogger";
import IBlockchainCenter from "../services/IBlockchainCenter";
import MonitorCache from "../monitor/MonitorCache";
import {RedisKeys} from "../consts/Consts";

// Resolves a block's unix-seconds timestamp, cached forever (block timestamps are immutable), so any block is
// read from RPC at most once. Cache is one Redis hash per network (field = block) to keep the many entries out
// of the top-level keyspace. Best-effort: a Redis/RPC failure returns null rather than throwing, so callers
// (gap date labels §16, forward ledger §18) degrade gracefully instead of failing the whole operation.
export default class BlockTimeResolver {
    readonly #center: IBlockchainCenter;
    readonly #cache: MonitorCache;
    readonly #logger: ILogger;

    constructor(center: IBlockchainCenter, cache: MonitorCache, logger: ILogger) {
        this.#center = center;
        this.#cache = cache;
        this.#logger = logger.clone('[BlockTime]');
    }

    async resolve(network: string, blockNumber: number): Promise<number | null> {
        const hashKey = `${RedisKeys.MONITOR_BLOCKTIME_PREFIX}${network}`;
        const field = String(blockNumber);
        const cached = await this.#cache.hGet(hashKey, field);
        if (cached !== null) return Number(cached);
        try {
            const ts = await this.#center.getBlockTimestamp(network, blockNumber);
            await this.#cache.hSet(hashKey, field, String(ts));
            return ts;
        } catch (e) {
            this.#logger.error(e);
            return null;
        }
    }
}
