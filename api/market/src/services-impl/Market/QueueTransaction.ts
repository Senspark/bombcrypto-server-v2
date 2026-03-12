import {ILogger, IMarketConfig, IQueueTransaction} from "@project/Services";
import {ItemBuy, QueueToBuy, ItemSell, OrderDataResponse} from "@project/TypeData";
import {IDependencies} from "@project/Dependencies";

const TAG = "[QueueTransaction]";
export default class QueueTransaction implements IQueueTransaction {
    private _timeOut: number;
    private _queueToBuy: QueueToBuy;
    private _oldItemBuy: ItemBuy[];
    private _logger: ILogger;

    constructor(private readonly _dep: IDependencies) {
        this._queueToBuy = new Map();
        this._oldItemBuy = [];
        this._logger = _dep.logger.clone(TAG);
    }

    getOrderItem(): QueueToBuy {
        return this._queueToBuy;
    }

    Initialize(marketConfig: IMarketConfig): void {
        this._timeOut = marketConfig.getTimeOutMarket();
    }


    // Mỗi transaction chỉ đc giữ tối đa 60 giây
    public checkTransactionTimeout(): ItemSell[] {
        let itemTimeOut: ItemSell[] = [];
        const now = Date.now();
        for (const [buyerUid, itemBuy] of this._queueToBuy.entries()) {
            if (now - itemBuy.createdAt > this._timeOut) {
                this._queueToBuy.delete(buyerUid);
                itemTimeOut.push(itemBuy.itemSell);
            }
        }
        // Kiểm tra các item trong oldItemBuy để trả về cho pool
        if(this._oldItemBuy.length > 0) {
            for (const item of this._oldItemBuy) {
                itemTimeOut.push(item.itemSell);
            }
            this._oldItemBuy = [];
        }
        return itemTimeOut;
    }

    // User đặt hàng, giữ transaction trong 60 giây
    public addTransactionForBuy(buyerUid: number, itemSell: ItemSell): void {
        if (this._queueToBuy.has(buyerUid)) {
            // user này đã có transaction đang chờ mua, giờ phải hủy transaction cũ
            // Trường hợp này xảy ra khi user đặt hàng xong f5 lại vào đặt hàng tiếp khi chưa timeout item cũ
            this._logger.info(`User ${buyerUid} already has transaction, cancel old transaction`);
            // Đánh dấu item cũ để trả về cho pool
            const oldItemBuy = this._queueToBuy.get(buyerUid);
            if (oldItemBuy) {
                this._oldItemBuy.push(oldItemBuy);
            }
            this._queueToBuy.delete(buyerUid);

        }
        const now = Date.now();
        const itemBuy: ItemBuy = {
            itemSell: itemSell,
            createdAt: now,
        };

        this._queueToBuy.set(buyerUid, itemBuy);
    }

    // User huỷ đăt hàng
    public cancelTransaction(buyerUid: number): ItemSell | null {
        const itemBuy = this._queueToBuy.get(buyerUid);
        if (!itemBuy) {
            this._logger.info(`User ${buyerUid} transaction not found or already timeout`);
            return null;
        }
        const itemSell = itemBuy.itemSell;
        this._queueToBuy.delete(buyerUid);

        return itemSell;
    }

    // User mua
    public buyFromTransaction(buyerUid: number): ItemBuy | null {
        const itemBuy = this._queueToBuy.get(buyerUid);
        if (!itemBuy) {
            this._logger.info(`User ${buyerUid} transaction not found or already timeout`);
            return null;
        }
        this._queueToBuy.delete(buyerUid);
        return itemBuy;
    }

    public isOrdering(sellerUid: number, itemId: number, price: number): boolean {
        for (const [_, itemBuy] of this._queueToBuy.entries()) {
            if (itemBuy.itemSell.itemId === itemId &&
                itemBuy.itemSell.items.some(item => item.sellerUid === sellerUid && item.price === price)) {
                return true;
            }
        }
        return false;
    }

}

