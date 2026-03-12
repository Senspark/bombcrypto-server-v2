import IMarketConfig, {itemId, ItemMarketConfig} from "./IMarketConfig";
import {Request} from "express";
import {BuyerUid, SellerUid} from "./IQueueTransaction";

export default interface IUserMarketHelper{
    Initialize(marketConfig: IMarketConfig): void;
    getDataOrder(req: Request): OrderData | null;
    getDataCancelOrder(req: Request): BuyerUid | null;
    getDataBuyRequest(req: Request): BuyerUid | null;
    getDataSellRequest(req: Request): SellData | null;
    getDataEditRequest(req: Request): EditData | null;
    getDataCancelRequest(req: Request): SellerData | null;

    getUserUid(req: Request): number;
}

export interface OrderData {
    buyerUid: number;
    itemId: itemId;
    quantity: number;
    expiration: number;
    userGem: number;
}

export interface SellData {
    sellerUid: number;
    itemId: itemId;
    quantity: number;
    listId: number[];
    price: number;
    expiration: number;
    isExpensive: boolean;
    modifyDate: number;
}

export interface EditData {
    sellerData: SellerData;
    sellData: SellData;
}

export interface SellerData {
    sellerUid: number;
    itemId: number;
    price: number;
    expiration: number;
}