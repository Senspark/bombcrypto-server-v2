import {
    BuyInDataBase,
    EditData, GeneralConfig,
    Item,
    ItemConfig,
    itemId,
    ItemMarketConfig, MyItemData,
    SellerData,
    SellInDataBase,
    SellPool
} from "@project/TypeData";
import {IDatabaseManager, ILogger, IMarketDatabaseAccess} from "@project/Services";
import {toNumberOr, toNumberOrZero} from "@project/services-impl/Utils/Number";
import {IDependencies} from "@project/Dependencies";

const TAG = "[MarketDatabaseAccess]";
export default class MarketDatabaseAccess implements IMarketDatabaseAccess {
    constructor(private _dep: IDependencies) {
        this._database = _dep.database;
        this._logger = _dep.logger.clone(TAG);
    }

    private readonly _database: IDatabaseManager
    private readonly _logger: ILogger

    async getGeneralConfig(): Promise<GeneralConfig> {
        try {
            const sql = `SELECT
                             *
                         FROM game_config
                         WHERE key IN ($1, $2, $3)`;
            const values = ['fee_sell', 'time_out_market', 'refresh_min_price_market'];
            const result = await this._database.query(sql, values);

            const fee = result.rows.find((row: { key: string; }) => row.key === 'fee_sell')?.value;
            const timeOutMarket = result.rows.find((row: { key: string; }) => row.key === 'time_out_market')?.value;
            const refreshMinPrice = result.rows.find((row: { key: string; }) => row.key === 'refresh_min_price_market')?.value;

            this._logger.info(`Loaded fee_sell: ${fee}, time_out_market: ${timeOutMarket}, refresh_min_price_market: ${refreshMinPrice}`);
            return {
                fee,
                timeOutMarket,
                refreshMinPrice };
        } catch (error) {
            this._logger.error(`Failed to load general config: ${error}`);
            throw error;
        }
    }

    async getItemMarketConfig(): Promise<ItemMarketConfig> {
        try {
            const sql = `SELECT 
                                id,
                                 min_price AS min,
                                 max_price AS max,
                                 is_have_expiration,
                                 max_price_7_days AS max7,
                                 max_price_30_days AS max30,
                                 fixed_amount,
                                 name,
                                 type,
                                 reward_type
                               
                     FROM config_item_market_v3
                     WHERE active = 1`;
            const result = await this._database.query(sql, []);

            const itemMarketConfig = new Map<itemId, ItemConfig>();

            for (const row of result.rows) {
                const item: ItemConfig = {
                    min: toNumberOr(row.min, 1),
                    max: toNumberOrZero(row.max),
                    fixedAmount: toNumberOrZero(row.fixed_amount),
                    isHaveExpiration: row.is_have_expiration === 1,
                    max7Days: toNumberOrZero(row.max7),
                    max30Days: toNumberOrZero(row.max30),
                    info: {
                        itemName: row.name,
                        itemType: row.type,
                        rewardType: row.reward_type
                    }
                };
                itemMarketConfig.set(row.id, item);
            }

            this._logger.info(`Loaded item market config success:\n ${JSON.stringify([...itemMarketConfig])}`);
            return itemMarketConfig;
        } catch (error) {
            this._logger.error(`Failed to load grouped item market config: ${error}`);
            throw error;
        }
    }

    async getAllSellItemFromDatabase(): Promise<SellPool> {
        try {
            const sql = `SELECT seller_uid, item_id, price, expiration_after, array_agg(id) as list_id, COUNT(*) as quantity, MAX(modify_date) as modify_date
                     FROM user_market_selling_v3
                     GROUP BY seller_uid, item_id, price, expiration_after`;
            const result = await this._database.query(sql, []);

            const sellPool: SellPool = new Map<itemId, Item[]>();

            for (const row of result.rows) {
                const item: Item = {
                    sellerUid: row.seller_uid,
                    price: row.price,
                    listId: row.list_id, // array of ids in group
                    quantity: parseInt(row.quantity),
                    expirationAfter: row.expiration_after,
                    modifyTime: Math.floor(new Date(row.modify_date).getTime() / 1000)
                };

                if (!sellPool.has(row.item_id)) {
                    sellPool.set(row.item_id, []);
                }

                sellPool.get(row.item_id)!.push(item);
            }
            this._logger.info("Load all sell item from database successfully");
            return sellPool;
        } catch (error) {
            this._logger.error(`Failed to load sell pool: ${error}`);
            throw error;
        }
    }

    async callDatabaseToBuy(data: BuyInDataBase): Promise<void> {
        try {
            const updateSql = `CALL sp_buy_item_market_v3($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`;

            const updateValues = [
                data.buyerUid,
                data.itemId,
                data.itemType,
                data.rewardType,
                data.listId,
                data.fee,

                // for log
                data.expiration,
                data.itemName,
                data.fixedQuantity,
                data.fixedPrice,
            ];
            this._logger.info(`CALL sp_buy_item_market_v3: ${JSON.stringify(updateValues)}`);
            await this._database.query(updateSql, updateValues);
        } catch (error) {
            this._logger.error(`Failed to buy item (query+update): ${error}`);
            throw error;
        }
    }

    async callDatabaseToSell(data: SellInDataBase): Promise<void> {
        try {
            const sql = `CALL sp_sell_item_market_v3(
            $1, $2, $3, $4, $5, $6, $7
        )`;

            const values = [
                data.sellerUid,
                data.itemType,
                data.itemId,
                data.price,
                data.rewardType,
                data.listId,
                data.expiration,
            ];
            this._logger.info(`CALL sp_sell_item_market_v3 : ${JSON.stringify(data)}`);
            await this._database.query(sql, values);
        } catch (error) {
            this._logger.error(`Failed to call sp_sell_item_market_v3: ${error}`);
            throw error;
        }
    }

    async callDatabaseToEdit(editData: EditData): Promise<void> {
        try {
            const sql = `CALL sp_edit_item_market_v3(
            $1, $2, $3, $4, $5, $6
        )`;

            const values = [
                editData.sellerData.sellerUid,
                editData.sellData.itemId,
                editData.sellData.price, // new price
                editData.sellerData.price, // old price
                editData.sellData.listId,
                editData.sellerData.expiration,
            ];
            this._logger.info(`CALL sp_edit_item_market_v3: ${JSON.stringify(values)}`);

            await this._database.query(sql, values);
        } catch (error) {
            this._logger.error(`Failed to call sp_edit_item_market_v3: ${error}`);
            throw error;
        }
    }

    async callDatabaseToCancel(cancel: SellerData): Promise<void> {
        try {
            const sql = `CALL sp_cancel_item_market_v3(
            $1, $2, $3, $4
        )`;

            const values = [
                cancel.sellerUid,
                cancel.itemId,
                cancel.expiration,
                cancel.price
            ];
            this._logger.info(`CALL sp_edit_cancel_market_v3: ${JSON.stringify(values)}`);

            await this._database.query(sql, values);
        } catch (error) {
            this._logger.error(`Failed to call sp_cancel_item_market_v3: ${error}`);
            throw error;
        }
    }

    async getMyItemMarket(sellerUid: number): Promise<MyItemData[]> {
        try {
            const sql = `SELECT
                             item_id,
                             price,
                             reward_type,
                             expiration_after,
                             COUNT(*) as quantity
                         FROM user_market_selling_v3
                         WHERE seller_uid = $1
                         GROUP BY item_id, price, reward_type, expiration_after`;

            const values = [sellerUid];
            const result = await this._database.query(sql, values);

            const myItemsData: MyItemData[] = result.rows.map(row => ({
                itemId: row.item_id,
                price: parseFloat(row.price),
                rewardType: row.reward_type,
                expirationAfter: parseInt(row.expiration_after),
                quantity: parseInt(row.quantity)
            }));

            return myItemsData;
        } catch (error) {
            this._logger.error(`Failed to get user market items: ${error}`);
            throw error;
        }
    }
}