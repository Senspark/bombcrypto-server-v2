import {Item, ItemSell} from "./IMarketPool";
import {OrderDataResponse} from "@project/services/Market/IMarketPlace";
import {IMarketConfig} from "@project/Services";

export default interface IQueueTransaction {
    Initialize(marketConfig: IMarketConfig): void;
    checkTransactionTimeout(): ItemSell[];
    addTransactionForBuy(buyerUid: number, itemSell: ItemSell): void;
    cancelTransaction(buyerUid: number): ItemSell | null
    buyFromTransaction(buyerUid: number): ItemBuy | null;
    isOrdering(sellerUid: number, itemId: number, price: number): boolean

    getOrderItem(): QueueToBuy;
}

export type QueueToBuy = Map<BuyerUid, ItemBuy>;

export interface ItemBuy {
    itemSell: ItemSell
    createdAt: number;
}

export type BuyerUid = number;
export type SellerUid = number;