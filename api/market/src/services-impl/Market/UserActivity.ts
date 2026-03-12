import {ILogger, IMarketConfig, IMarketDatabaseAccess, IUserActivity} from "@project/Services";
import {ItemSell} from "@project/services/Market/IMarketPool";
import {IDependencies} from "@project/Dependencies";
import { BuyInDataBase, EditData, SellData, SellerData, SellInDataBase} from "@project/TypeData";
import {toNumberOrZero} from "@project/services-impl/Utils/Number";

const TAG = "[UserActivity]";
export default class UserActivity implements IUserActivity {
    private readonly _logger: ILogger;
    private _marketConfig: IMarketConfig;

    constructor(
        private readonly _dep: IDependencies,
        private readonly _marketDatabase: IMarketDatabaseAccess) {
        this._logger = _dep.logger.clone(TAG);
    }

    async buy(buyerUid: number, data: ItemSell): Promise<void> {
        try {
            if (data.items.length === 0) {
                throw new Error("No items to buy.");
            }
            const info = this._marketConfig.getItemInfo(data.itemId);
            const fee = this._marketConfig.getFee();
            const fixedPrice = this._marketConfig.getMinPriceFixedItem(data.itemId)

            const buyData: BuyInDataBase = {
                buyerUid: buyerUid,
                itemId: data.itemId,
                rewardType: info.rewardType,
                itemName: info.itemName,
                itemType: info.itemType,
                expiration: data.expiration,
                listId: data.items.flatMap(item => item.listId),
                fee: fee,
                fixedQuantity: data.fixedQuantity,
                fixedPrice: fixedPrice,
            };

            await this._marketDatabase.callDatabaseToBuy(buyData);
            this._logger.info(`Successfully processed buy request for itemId: ${data.itemId}`);
        } catch (error) {
            this._logger.error(`Failed to process buy request: ${error.message}`);
            throw error;
        }
    }

    async sell(sellData: SellData): Promise<void> {
        try {
            const itemInfo = this._marketConfig.getItemInfo(sellData.itemId);

            const sellInDataBase: SellInDataBase = {
                sellerUid: sellData.sellerUid,
                itemId: sellData.itemId,
                rewardType: itemInfo.rewardType,
                itemType: itemInfo.itemType,
                quantity: sellData.quantity,
                price: sellData.price,
                listId: sellData.listId,
                expiration: sellData.expiration,
            };

            await this._marketDatabase.callDatabaseToSell(sellInDataBase);
            this._logger.info(`Successfully processed sell request for itemId: ${sellData.itemId}`);
        } catch (error) {
            this._logger.error(`Failed to process sell request: ${error.message}`);
            throw error;
        }
    }

    async edit(editData: EditData): Promise<void> {
        try {

            await this._marketDatabase.callDatabaseToEdit(editData);
            this._logger.info(`Successfully processed edit request for itemId: ${editData.sellerData.itemId}`);
        } catch (error) {
            this._logger.error(`Failed to process edit request: ${error.message}`);
            throw error;
        }
    }

    async cancel(sellerData: SellerData): Promise<void> {
        try {
            await this._marketDatabase.callDatabaseToCancel(sellerData);
            this._logger.info(`Successfully processed cancel request for itemId: ${sellerData.itemId}`);
        } catch (error) {
            this._logger.error(`Failed to process cancel request: ${error.message}`);
            throw error;
        }
    }

    public Initialize(marketConfig: IMarketConfig) {
        this._marketConfig = marketConfig;
    }


}