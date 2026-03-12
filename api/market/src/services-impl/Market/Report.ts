import {
    ILogger,
    IMarketConfig,
    IMarketDatabaseAccess,
    IMarketPool,
    IQueueTransaction,
    IUserActivity
} from "@project/Services";
import {ExpensivePool, FixedPool, ItemSell, SellPool} from "@project/services/Market/IMarketPool";
import {IDependencies} from "@project/Dependencies";
import {BuyInDataBase, EditData, QueueToBuy, SellData, SellerData, SellInDataBase} from "@project/TypeData";

const TAG = "[Report]";
export default class Report {
    private readonly _logger: ILogger;

    constructor(
        private readonly _dep: IDependencies,
        private readonly _marketPool: IMarketPool,
        private readonly _queueOrder: IQueueTransaction
    ) {
        this._logger = _dep.logger.clone(TAG);
    }

    public getSelling(): SellPool | null {
        return  this._marketPool.getUserPool();
    }

    public  getOrdering(): QueueToBuy | null {
        return this._queueOrder.getOrderItem();
    }

    public  getExpensive(): ExpensivePool | null {
        return this._marketPool.getExpensivePool();
    }

    public  getFixed(): FixedPool | null {
        return this._marketPool.getFixedPool();
    }
}