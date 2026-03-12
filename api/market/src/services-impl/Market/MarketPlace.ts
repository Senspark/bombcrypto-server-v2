import {IDependencies} from "@project/Dependencies";
import {
    ILogger, IMarketConfig,
    IMarketDatabaseAccess,
    IMarketPlace,
    IMarketPool,
    IQueueTransaction, IRedisDatabase, IScheduler, IUserActivity,
    IUserMarketHelper
} from "@project/Services";
import QueueTransaction from "@project/services-impl/Market/QueueTransaction";
import MarketPool from "@project/services-impl/Market/MarketPool";
import UserMarketHelper from "@project/services-impl/Market/UserMarketHelper";
import {Request, Response} from "express";
import {sleep} from "@project/services-impl/Utils/Time";
import UserActivity from "@project/services-impl/Market/UserActivity";
import {MarketError} from "@project/consts/ErrorMessage";
import {BuyDataResponse, MarketItemConfigResponse, MyItemData, OrderDataResponse} from "@project/TypeData";
import {RedisKeys} from "@project/consts/Consts";
import Report from "@project/services-impl/Market/Report";
import Scheduler from "@project/services-impl/Scheduler";
import {config} from "dotenv";


const TAG = "[MarketPlace]";
export default class MarketPlace implements IMarketPlace {
    private readonly _logger: ILogger
    private readonly _marketPool: IMarketPool;
    private readonly _queueTransaction: IQueueTransaction
    private readonly _userMarketHelper: IUserMarketHelper
    private readonly _userActivity: IUserActivity
    private readonly _redis: IRedisDatabase
    private readonly _report: Report
    private _marketConfig: IMarketConfig;

    private _refreshMinPriceScheduler: IScheduler;
    private _refreshFixedPoolScheduler: IScheduler;

    private readonly _1Day: number = 24 * 60 * 60 * 1000;

    private readonly _interval: number;
    private _refreshMinPrice: number;

    constructor(
        readonly _dep: IDependencies,
        readonly _marketDatabase: IMarketDatabaseAccess) {
        this._logger = _dep.logger.clone(TAG);
        this._redis = _dep.redis;
        this._interval = _dep.envConfig.checkInterval;

        this._queueTransaction = new QueueTransaction(_dep);
        this._marketPool = new MarketPool(_dep);
        this._userActivity = new UserActivity(_dep, this._marketDatabase);
        this._userMarketHelper = new UserMarketHelper(_dep);

        this._report = new Report(_dep, this._marketPool, this._queueTransaction);
    }

    public async Initialize(marketConfig: IMarketConfig) {
        try {
            this._marketConfig = marketConfig;
            const allSellItem = await this._marketDatabase.getAllSellItemFromDatabase();
            await this._marketPool.Initialize(marketConfig, allSellItem);
            this._userActivity.Initialize(marketConfig);
            this._userMarketHelper.Initialize(marketConfig);
            this._queueTransaction.Initialize(marketConfig);
            this._refreshMinPrice = marketConfig.getRefreshMinPrice();


            this.loop().then();

            //Refresh min price every 1 minute
            this._refreshMinPriceScheduler = Scheduler
                .Builder(this._dep.logger, "RefreshMinPrice")
                .setInitialDelay(0)
                .setInterval(this._refreshMinPrice)
                .setCallback(this.refreshMinPrice.bind(this))
                .start()

            //Refresh fixed pool every 24 hours
            this._refreshFixedPoolScheduler = Scheduler
                .Builder(this._dep.logger, "RefreshFixedPool")
                .delayUntilMidnight()
                .setInterval(this._1Day)
                .setCallback(this.refreshFixPool.bind(this))
                .start()

        } catch (error) {
            this._logger.error(`Failed to initialize market place: ${error}`);
            throw error;
        }
    }

    public async orderItem(req: Request, res: Response) {
        try {
            // Lấy thông tin đặt hàng từ request
            const orderData = this._userMarketHelper.getDataOrder(req);
            if (orderData == null) {
                this._logger.error("Missing order data in order request");
                return res.sendError(MarketError.MISSING_DATA);
            }

            // Lấy item cần mua ra khỏi pool
            const itemSell = this._marketPool.getItemForOrder(orderData)

            if (itemSell.result == 'SOLD_OUT') {
                this._logger.error(`Item ${orderData.itemId} is sold out`);
                return res.sendError(MarketError.SOLD_OUT_ERROR);
            }

            if(itemSell.result == 'NOT_ENOUGH_GEM') {
                this._logger.error(`Not enough gem to buy item ${orderData.itemId}`);
                const orderResponse: OrderDataResponse = {
                    isOrderSuccess: false,
                    totalPrice: itemSell.totalPrice,
                    totalQuantity: itemSell.totalQuantity,
                }
                return res.sendSuccess(orderResponse);
            }
            if(itemSell.itemSell == null) {
                this._logger.error(`Not found item ${orderData.itemId} in pool`);
                return res.sendError(MarketError.UNHANDLED_ERROR);
            }

            // Đặt hàng
            this._queueTransaction.addTransactionForBuy(orderData.buyerUid, itemSell.itemSell);

            // Data trả về cho server để hiển thị client
            const orderResponse: OrderDataResponse = {
                isOrderSuccess: true,
                totalPrice: itemSell.totalPrice,
                totalQuantity: itemSell.totalQuantity,
            }
            return res.sendSuccess(orderResponse);
        }
        catch (error) {
            this._logger.error(`Unhandled error in order request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async cancelOrder(req: Request, res: Response) {
        try{
            const buyerUid = this._userMarketHelper.getDataCancelOrder(req);

            if (buyerUid == null) {
                this._logger.error("Missing buyer uid in cancel order request");
                return res.sendError(MarketError.MISSING_DATA);
            }

            // Huỷ đặt hàng
            const itemSell = this._queueTransaction.cancelTransaction(buyerUid);

            if (itemSell == null) {
                this._logger.info(`Transaction has timeout for buyer ${buyerUid} cancel`);
                return res.sendSuccess(true);
            }

            // Trả item về pool
            this._marketPool.returnItemToPool(itemSell);

            return res.sendSuccess(true);
        } catch (error) {
            this._logger.error(`Unhandled error in cancelling order request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async buy(req: Request, res: Response) {
        try {
            const buyerUid = this._userMarketHelper.getDataBuyRequest(req);

            if (buyerUid == null) {
                this._logger.error("Missing buyer uid in buy request");
                return res.sendError(MarketError.MISSING_DATA);
            }

            // Mua item từ danh sách đã đặt hàng
            const itemBuy = this._queueTransaction.buyFromTransaction(buyerUid);

            if (itemBuy == null) {
                this._logger.info(`Transaction has timeout for buyer ${buyerUid} buy`);
                return res.sendError(MarketError.TRANSACTION_TIME_OUT);
            }

            try {
                // Cập nhật db và trừ tiền, cộng item
                await this._userActivity.buy(buyerUid, itemBuy.itemSell);

                // Data trả về cho server số lượng item đc mua trong fixed pool
                const buyResponse: BuyDataResponse = {
                    itemId: itemBuy.itemSell.itemId,
                    itemType: this._marketConfig.getItemInfo(itemBuy.itemSell.itemId).itemType,
                    fixedQuantity: itemBuy.itemSell.fixedQuantity,
                    totalQuantity: itemBuy.itemSell.totalQuantity,
                    expiration: itemBuy.itemSell.expiration
                }

                return res.sendSuccess(buyResponse);
            } catch (dbError) {
                // Gọi db fail , trả item lại cho pool
                this._logger.error(`Failed to process buy operation in database: ${dbError}`);
                this._marketPool.returnItemToPool(itemBuy.itemSell);
                return res.sendError(MarketError.UNHANDLED_ERROR);
            }
        }
        catch (error) {
            this._logger.error(`Unhandled error in buy request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async sell(req: Request, res: Response) {
        try{
            // Lấy thông tin bán hàng từ request
            const sellData = this._userMarketHelper.getDataSellRequest(req);

            if (sellData == null) {
                this._logger.error("Missing sell data in sell request");
                return res.sendError(MarketError.MISSING_DATA);
            }

            //item này đang đc đặt hàng, ko cho sell thêm
            if(this._queueTransaction.isOrdering(sellData.sellerUid, sellData.itemId, sellData.price)){
                return res.sendError(MarketError.ITEM_IN_PROGRESS);
            }

            // Allow sell same item with different price
            // const canSell = this._marketPool.checkCanSell(sellData);
            // // Loại item này đã đc user này đăng bán rồi, chỉ có thể edit hoặc cancel
            // if (!canSell) {
            //     this._logger.info(`User ${sellData.sellerUid} is already sell item ${sellData.itemId}`);
            //     return res.sendError(MarketError.ALREADY_SELL_ITEM);
            // }
            // Cập nhật db
            await this._userActivity.sell(sellData)

            const isAddSuccess = this._marketPool.addItemToUserPool(sellData);
            if (!isAddSuccess) {
                this._logger.error(`User ${sellData.sellerUid} sell item ${sellData.itemId} fail`);
                return res.sendError(MarketError.UNHANDLED_ERROR);
            }

            return res.sendSuccess(true);
        }
        catch (error) {
            this._logger.error(`Unhandled error in sell request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async edit(req: Request, res: Response) {
        try{
            const editData = this._userMarketHelper.getDataEditRequest(req);

            if (editData == null) {
                this._logger.error("Missing edit data in edit request");
                return res.sendError(MarketError.MISSING_DATA);
            }

            //item này đang đc đặt hàng, ko cho edit, price here is old price
            if(this._queueTransaction.isOrdering(editData.sellerData.sellerUid, editData.sellerData.itemId, editData.sellerData.price)){
                return res.sendError(MarketError.ITEM_IN_PROGRESS);
            }

            // Lấy ra loại item này đang bán của user này
            const itemSell = this._marketPool.getItemBySellerUid(editData.sellerData);


            if (itemSell === undefined) {
                this._logger.info(`Item ${editData.sellerData.itemId} is sold out`);
                return res.sendError(MarketError.SOLD_OUT_ERROR);
            }
            const isAddMore = editData.sellData.quantity - itemSell.quantity

            if(isAddMore < 0) {
                // Nếu số lượng giảm thì phải xóa bớt listId
                editData.sellData.listId = itemSell.listId.slice(0, editData.sellData.quantity);
            }
            // Ko cho edit nhiều số lượng hơn
            // else if(isAddMore > 0) {
            //     // Thêm các itemId mới vào db
            //     editData.sellData.listId.push(...itemSell.listId)
            // }
            else{
                // Nếu số lượng không đổi thì không cần làm gì cả
                editData.sellData.listId = itemSell.listId;
            }

            try {
                // edit data đưa cho db cập nhập là ds các item sẽ đc bán, db ko quan tâm có những cái nào đang bán hay ko
                // flow là gỡ hết loại item này đang bán của user và thêm vào toàn bộ item trong edit data
                // Cập nhật db
                await this._userActivity.edit(editData);

                // Thêm vào pool item mới sau khi user đã edit
                this._marketPool.addItemToUserPool(editData.sellData);
                
                return res.sendSuccess(true);
            } catch (error) {
                // Nếu xảy ra lỗi trong quá trình edit, thêm lại item ban đầu vào pool
                this._logger.error(`Error during edit operation: ${error}, adding back original item to pool`);
                // Add the original item back to the pool
                this._marketPool.addItemToPool(editData.sellData.itemId, itemSell);

                return res.sendError(MarketError.UNHANDLED_ERROR);
            }
        }
        catch (error) {
            this._logger.error(`Unhandled error in edit request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async cancel(req: Request, res: Response) {
        try{
            const sellerData = this._userMarketHelper.getDataCancelRequest(req);

            if (sellerData == null) {
                this._logger.error("Missing seller data in cancel request");
                return res.sendError(MarketError.MISSING_DATA);
            }

            //item này đang đc đặt hàng, ko cho cancel
            if(this._queueTransaction.isOrdering(sellerData.sellerUid, sellerData.itemId, sellerData.price)){
                return res.sendError(MarketError.ITEM_IN_PROGRESS);
            }

            // Lấy ra loại item này đang bán khỏi pool để huỷ
            const itemSell = this._marketPool.getItemBySellerUid(sellerData);

            if (itemSell === undefined) {
                this._logger.info(`Item ${sellerData.itemId} is sold out`);
                return res.sendError(MarketError.SOLD_OUT_ERROR);
            }
            try {
                // Cập nhật db
                await this._userActivity.cancel(sellerData);
            }
            catch (error) {
                this._logger.error(`Error during cancel operation: ${error}, adding back original item to pool`);
                // Add the original item back to the pool
                this._marketPool.addItemToPool(sellerData.itemId, itemSell);
                return res.sendError(MarketError.UNHANDLED_ERROR);
            }

            return res.sendSuccess(true);
        }
        catch (error) {
            this._logger.error(`Unhandled error in cancel request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async getConfig(req: Request, res: Response): Promise<void> {
        try {
            const config = this._marketConfig.getItemMarketConfig()
            const marketConfigResponse: MarketItemConfigResponse[] = [];
            for (const [itemId, itemConfig] of config.entries()) {
                marketConfigResponse.push({
                    itemId: itemId,
                    minPrice: itemConfig.min,
                    isNoExpiration: !itemConfig.isHaveExpiration
                });
            }
            return res.sendSuccess(marketConfigResponse);
        }
        catch (error) {
            this._logger.error(`Unhandled error in get config request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }


    public async getMyItemMarket(req: Request, res: Response): Promise<void> {
        try {
            const uid = this._userMarketHelper.getUserUid(req);
            const myItem = await this._marketDatabase.getMyItemMarket(uid);
            return res.sendSuccess(myItem);
        }
        catch (error) {
            this._logger.error(`Unhandled error in get my item request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }

    }


    // Mỗi giây kiểm tra tất cả các transaction đang chuẩn bị mua có cái nào hết hạn ko
    private async loop() {
        const foreverLoop = true;
        while (foreverLoop) {
            await sleep(this._interval);
            try {
                const timedOutItems = this._queueTransaction.checkTransactionTimeout();
                if (timedOutItems.length > 0) {
                    this._logger.info(`${timedOutItems.length} transaction has timed out`);
                    for (const item of timedOutItems) {
                        this._marketPool.returnItemToPool(item);
                    }
                }
            } catch (error) {
                this._logger.error(`Error during transaction timeout check: ${error}`);
            }
        }
    }

    private async refreshMinPrice() {
        try {
            const currentMinPrice = this._marketPool.getCurrentMinPrice();
            const serializedMinPrice = JSON.stringify(Object.fromEntries(currentMinPrice));
            await this._redis.fields.set(RedisKeys.MARKET_MIN_PRICE, serializedMinPrice);
        } catch (error) {
            this._logger.error(`Error refreshing min price: ${error}`);
        }
    };

    private async refreshFixPool() {
        try {
            this._logger.info("Refreshing fixed pool");
            await this._marketPool.refreshFixedPool();
        } catch (error) {
            this._logger.error(`Error refreshing fixed pool: ${error}`);
        }
    };


    // For test

    public async getSelling(req: Request, res: Response) {
        try {
            const selling = this._report.getSelling();

            if (selling) {
                // Convert the Map to a plain object
                const serializedSelling = Object.fromEntries(selling);
                return res.sendSuccess(JSON.stringify(serializedSelling));
            } else {
                // Handle the case where selling is null
                this._logger.info("Selling data is null");
                return res.sendSuccess({});
            }
        } catch (error) {
            this._logger.error(`Unhandled error in get selling request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async getOrdering(req: Request, res: Response) {
        try {
            const ordering = this._report.getOrdering();

            if (ordering) {
                // Convert the Map to a plain object
                const serializedOrdering = Object.fromEntries(ordering);
                return res.sendSuccess(JSON.stringify(serializedOrdering));
            } else {
                // Handle the case where ordering is null
                this._logger.info("Ordering data is null");
                return res.sendSuccess({});
            }
        } catch (error) {
            this._logger.error(`Unhandled error in get ordering request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async getExpensive(req: Request, res: Response) {
        try {
            const expensive = this._report.getExpensive();

            if (expensive) {
                // Convert the Map to a plain object
                const serializedExpensive = Object.fromEntries(expensive);
                return res.sendSuccess(JSON.stringify(serializedExpensive));
            } else {
                // Handle the case where expensive is null
                this._logger.info("Expensive data is null");
                return res.sendSuccess({});
            }
        } catch (error) {
            this._logger.error(`Unhandled error in get expensive request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async getFixed(req: Request, res: Response) {
        try {
            const fixed = this._report.getFixed();

            if (fixed) {
                // Convert the Map to a plain object
                const serializedFixed = Object.fromEntries(fixed);
                return res.sendSuccess(JSON.stringify(serializedFixed));
            } else {
                // Handle the case where fixed is null
                this._logger.info("Fixed data is null");
                return res.sendSuccess({});
            }
        } catch (error) {
            this._logger.error(`Unhandled error in get fixed request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }
}