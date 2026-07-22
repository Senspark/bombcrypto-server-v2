import MonitorCache from "./MonitorCache";
import {Chain, monitorCellKey, RedisKeys, TokenSymbol} from "../consts/Consts";

// Lets the sweep drop exactly the monitor snapshots its work invalidated, without knowing about MonitorService
// (which is built later). A deposit/withdraw on (chain, symbol) only affects that one cell + the health lag;
// the per-chain meta (enable flags / fee) is never touched here. Wiring this instead of MonitorService keeps
// the indexer → monitor dependency one-way (no cycle).
export interface IMonitorInvalidator {
    invalidateCell(chain: Chain, symbol: TokenSymbol): Promise<void>;
    invalidateHealth(): Promise<void>;
}

export default class MonitorInvalidator implements IMonitorInvalidator {
    readonly #cache: MonitorCache;

    constructor(cache: MonitorCache) {
        this.#cache = cache;
    }

    invalidateCell(chain: Chain, symbol: TokenSymbol): Promise<void> {
        return this.#cache.delete(monitorCellKey(chain, symbol));
    }

    invalidateHealth(): Promise<void> {
        return this.#cache.delete(RedisKeys.MONITOR_INDEXER_STATUS);
    }
}
