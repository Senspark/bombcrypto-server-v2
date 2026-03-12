import {ItemMarketConfig} from "./IMarketConfig";
import {SellPool} from "@project/services/Market/IMarketPool";
import {EditData, SellerData} from "@project/services/Market/IUserMarketHelper";

export default interface IMarketDatabaseAccess {
    getGeneralConfig(): Promise<GeneralConfig>;
    getItemMarketConfig(): Promise<ItemMarketConfig>;
    getAllSellItemFromDatabase(): Promise<SellPool>;

    callDatabaseToBuy(data: BuyInDataBase): Promise<void>;
    callDatabaseToSell(data: SellInDataBase): Promise<void>;
    callDatabaseToEdit(editData: EditData): Promise<void>;
    callDatabaseToCancel(cancelData: SellerData): Promise<void>;

    getMyItemMarket(sellerUid: number): Promise<MyItemData[]>
}

export interface GeneralConfig {
    fee: number;
    timeOutMarket: number;
    refreshMinPrice: number;
}


export interface BuyInDataBase{
    buyerUid: number;
    itemId: number;
    rewardType: string;
    itemName: string;
    itemType: number;
    expiration: number;
    listId: number[];
    fee: number;
    fixedQuantity: number;
    fixedPrice: number;
}

export interface SellInDataBase{
    sellerUid: number;
    itemId: number;
    rewardType: string;
    itemType: number;
    quantity: number;
    price: number;
    listId: number[];
    expiration: number;
}

export interface MyItemData {
    itemId: number;
    price: number;
    quantity: number;
    rewardType: string;
    expirationAfter: number;
}
