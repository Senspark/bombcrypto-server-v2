import {EditData, ItemSell, SellData, SellerData} from "@project/TypeData";
import {IMarketConfig} from "@project/Services";

export default interface IUserActivity {
    Initialize(marketConfig: IMarketConfig) : void;
    buy(buyerUid: number, data: ItemSell): Promise<void>;
    sell(sellData: SellData): Promise<void>;
    edit(data: EditData): Promise<void>;
    cancel(data: SellerData): Promise<void>;
}