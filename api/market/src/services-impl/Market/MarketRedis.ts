import {IDependencies} from "@project/Dependencies";
import {ILogger, IMarketConfig, IRedisDatabase} from "@project/Services";
import {FixedPool, ItemFixedConfig, itemId} from "@project/TypeData";
import {RedisKeys} from "@project/consts/Consts";
import {toNumberOrZero} from "@project/services-impl/Utils/Number";

const TAG = "[MarketRedis]";
export default class MarketRedis{
    private readonly _logger: ILogger;
    private readonly _redis: IRedisDatabase;
    private readonly _marketConfig: IMarketConfig;

    constructor(private _dep: IDependencies, marketConfig: IMarketConfig) {
        this._logger = _dep.logger.clone(TAG);
        this._redis = _dep.redis;
        this._marketConfig = marketConfig;
    }


    public async getFixedPool(): Promise<FixedPool | null> {
        try {
            const fixedPool = await this._redis.hashes.read(RedisKeys.MARKET_FIXED_POOL);
            if (!fixedPool || fixedPool.size === 0) {
                this._logger.error("Failed to get fixed pool from Redis");
                return null;
            }
            const result = new Map<itemId, ItemFixedConfig>();
            const itemsToRemove: string[] = [];

            // Check if market config is available
            if (!this._marketConfig) {
                this._logger.error("Market config not set, returning all items without validation");
                fixedPool.forEach((value, key) => {
                    const itemConfig: ItemFixedConfig = JSON.parse(value);
                    result.set(toNumberOrZero(key), itemConfig);
                });
                return result;
            }

            // Process each item, checking against market config
            fixedPool.forEach((value, key) => {
                const numericKey = toNumberOrZero(key);
                const itemConfig: ItemFixedConfig = JSON.parse(value);

                // Check if item exists in market config
                if (this._marketConfig.getItemMarketConfig().has(numericKey)) {
                    result.set(numericKey, itemConfig);
                } else {
                    // Item doesn't exist in market config, add to removal list
                    itemsToRemove.push(key);
                    this._logger.info(`Item ${numericKey} found in Redis but not in market config, will be removed`);
                }
            });

            // Remove items that don't exist in market config
            if (itemsToRemove.length > 0) {
                await this.removeItemsFixed(itemsToRemove);
            }

            return result;
        } catch (error) {
            this._logger.error(`Failed to parse fixed pool data: ${error}`);
            return null;
        }
    }

    private async removeItemsFixed(itemIds: string[]) {
        try {
            await this._redis.hashes.remove(RedisKeys.MARKET_FIXED_POOL, itemIds);
            this._logger.info(`Removed ${itemIds.length} items from Redis that no longer exist in market config`);
        } catch (error) {
            this._logger.error(`Failed to remove items from fixed pool: ${error}`);
        }
    }

    public async removeItemFixed(itemId: itemId) {
        try {
            await this._redis.hashes.remove(RedisKeys.MARKET_FIXED_POOL, [itemId.toString()]);
        } catch (error) {
            this._logger.error(`Failed to remove item fixed config: ${error}`);
        }
    }

    public async setItemFixed(itemId: itemId, itemConfig: ItemFixedConfig) {
        try {
            const data = new Map<string, string>();
            data.set(itemId.toString(), JSON.stringify(itemConfig));

            await this._redis.hashes.add(RedisKeys.MARKET_FIXED_POOL, data);
        } catch (error) {
            this._logger.error(`Failed to set item fixed config: ${error}`);
        }
    }
}