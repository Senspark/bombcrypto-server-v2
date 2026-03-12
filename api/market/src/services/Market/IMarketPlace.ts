import {ItemMarketConfig} from "@project/services/Market/IMarketConfig";
import {Request, Response} from "express";
import {IMarketConfig} from "@project/Services";
import {MyItemData} from "@project/services/Market/IMarketDatabaseAccess";

export default interface IMarketPlace{
    Initialize(marketConfig: IMarketConfig): void;
    orderItem(req: Request, res: Response): Promise<void>;
    cancelOrder(req: Request, res: Response): Promise<void>;
    buy(req: Request, res: Response): Promise<void>;
    sell(req: Request, res: Response): Promise<void>;
    edit(req: Request, res: Response): Promise<void>;
    cancel(req: Request, res: Response): Promise<void>;
    getConfig(req: Request, res: Response): Promise<void>;
    getMyItemMarket(req: Request, res: Response): Promise<void>;


    getSelling(req: Request, res: Response): Promise<void>;
    getOrdering(req: Request, res: Response): Promise<void>;
    getExpensive(req: Request, res: Response): Promise<void>;
    getFixed(req: Request, res: Response): Promise<void>;
}

export interface OrderDataResponse {
    isOrderSuccess: boolean;
    totalQuantity: number;
    totalPrice: number;
}

// Chứa số lượng item đc bán từ fixed pool nên server cần tạo các item này và thêm vào db
export interface BuyDataResponse {
    itemId: number;
    itemType: number;
    fixedQuantity: number;
    totalQuantity: number;
    expiration: number;
}

export interface MarketItemConfigResponse{
    itemId: number;
    minPrice: number;
    isNoExpiration: boolean;
}
