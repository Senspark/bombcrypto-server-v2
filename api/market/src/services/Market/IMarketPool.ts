import {itemId} from "./IMarketConfig";
import {OrderData, SellData, SellerData} from "./IUserMarketHelper";
import {SellerUid} from "./IQueueTransaction";
import {IMarketConfig} from "@project/Services";

export default interface IMarketPool{
    Initialize(marketConfig: IMarketConfig, sellPool: SellPool): Promise<void>;
    getItemForOrder(orderData: OrderData): OrderResult
    getItemBySellerUid(sellerData: SellerData): Item | undefined
    addItemToUserPool(sellData: SellData): boolean
    addItemToPool(itemId: number, item: Item): void
    returnItemToPool(itemSell: ItemSell): void
    getCurrentMinPrice(): Map<itemId, number>
    refreshFixedPool(): Promise<void>

    getUserPool(): SellPool
    getFixedPool(): FixedPool
    getExpensivePool(): ExpensivePool
}

export type SellPool = Map<itemId, Item[]>;
export type FixedPool = Map<itemId, ItemFixedConfig>;
export type ExpensivePool = Map<SellerUid, ItemExpensive[]>;

export interface Item {
    sellerUid: number;
    price: number;
    listId: number[];
    quantity: number;
    expirationAfter: number;
    modifyTime: number;
}

export interface ItemSell{
    items: Item[];
    itemId: itemId;
    totalPrice: number;
    totalQuantity: number;
    fixedQuantity: number;
    expiration: number;
}

export interface OrderResult{
    result: OrderResultType;
    itemSell?: ItemSell;
    totalPrice: number;
    totalQuantity: number;
}

export interface ItemFixed {
    price: number;
    quantity: number;
}
export interface ItemFixedConfig{
    price: number;
    quantity: number;
    price7Days: number;
    price30Days: number;
    quantity7Days: number;
    quantity30Days: number;
}

export interface ItemExpensive {
    itemId: number;
    price: number;
    quantity: number;
    id: number[];
    expirationAfter: number;
    modifyTime: number;
}

export enum ExpirationType {
    NO_EXPIRED = "NO_EXPIRED",
    SEVEN_DAYS = "7_DAYS",
    THIRTY_DAYS = "30_DAYS"
}

export type OrderResultType = "SUCCESS" | "NOT_ENOUGH_GEM" | "SOLD_OUT";