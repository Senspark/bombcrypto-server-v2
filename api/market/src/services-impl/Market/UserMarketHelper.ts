import {ILogger, IMarketConfig, IMarketDatabaseAccess, IUserMarketHelper} from "@project/Services";
import {IDependencies} from "@project/Dependencies";
import {
    BuyerUid,
    EditData,
    ItemMarketConfig,
    OrderData,
    SellData,
    SellerData,
    itemId,
    SellerUid
} from "@project/TypeData";
import {toNumberOrZero, validateData, validateNumberList} from "@project/services-impl/Utils/Number";
import {Request} from "express";


const TAG = "[UserMarketHelper]";
export default class UserMarketHelper implements IUserMarketHelper {
    private readonly _logger: ILogger
    private _marketConfig: IMarketConfig;

    constructor(
        private readonly _dep: IDependencies) {
        this._logger = _dep.logger.clone(TAG);
    }

    getUserUid(req: Request): number {
        const uid = req.body.uid;
        validateData("[GET UID]", this._logger, uid);
        return uid;
    }

    public Initialize(marketConfig: IMarketConfig) {
        this._marketConfig = marketConfig;
    }

    public getDataOrder(req: Request): OrderData | null {
        try{
            const buyerUid = req.body.buyerUid;
            const itemId = req.body.itemId;
            const quantity = req.body.quantity;
            const userGem = toNumberOrZero(req.body.userGem);
            const expiration = toNumberOrZero(req.body.expiration);

            // Đảm bảo các giá trị là hợp lệ
            validateData("[ORDER]", this._logger, buyerUid, itemId, quantity);

            return {
                buyerUid: buyerUid,
                itemId: itemId,
                quantity: quantity,
                expiration: expiration,
                userGem: userGem
            };
        }
        catch (error) {
            return null;
        }

    }

    public getDataCancelOrder(req: Request): BuyerUid | null {
        try {
            const buyerUid = req.body.buyerUid;

            validateData("[CANCEL ORDER]", this._logger, buyerUid);
            return buyerUid;
        }
        catch (error) {
            return null;
        }

    }

    public getDataBuyRequest(req: Request): BuyerUid | null {
        try{
            const buyerUid = req.body.buyerUid;

            validateData("[BUY]", this._logger,  buyerUid);
            return buyerUid;
        }
        catch (error) {
            throw error
        }
    }

    public getDataSellRequest(req: Request): SellData | null {
        try {
            const { sellerUid, itemId, quantity, price, listId, expiration, isExpensive, modifyDate }
                = this.extractSellData("[SELL]", req);

            return {
                sellerUid: sellerUid,
                itemId: itemId,
                quantity: quantity,
                price: price,
                listId: listId,
                expiration: expiration,
                isExpensive: isExpensive,
                modifyDate: modifyDate
            }
        }
        catch (error) {
            return null;
        }
    }

    public getDataEditRequest(req: Request): EditData {
        const { sellerUid, itemId, quantity, price, listId, expiration, isExpensive, modifyDate }
            = this.extractSellData("[EDIT]", req);

        const oldPrice = req.body.oldPrice;
        validateData("[EDIT]",this._logger, oldPrice);

        return {
            sellerData: {sellerUid, itemId, price: oldPrice, expiration},
            sellData: {sellerUid, itemId, listId, quantity, price, expiration, isExpensive, modifyDate},
        };
    }

    public getDataCancelRequest(req: Request): SellerData {
        const sellerUid = req.body.sellerUid;
        const itemId = req.body.itemId;
        const price = req.body.price;
        const expiration = toNumberOrZero(req.body.expiration);

        validateData("[CANCEL]", this._logger, sellerUid, itemId);

        return {
            sellerUid,
            itemId,
            price,
            expiration
        };
    }

    private checkPrice(sellerUid: number, price: number, itemId: itemId, expiration: number): boolean {
        const maxPrice = this._marketConfig.getMaxPriceWithExpiration(itemId, expiration);
        const minPrice = this._marketConfig.getMinPrice(itemId);
        if (!maxPrice || !minPrice) {
            this._logger.error(`Item ${itemId} not found in config`);
            throw new Error();
        }
        if (price < minPrice) {
            this._logger.error(`UserItem ${sellerUid} sell item ${itemId} price ${price} is less than min price ${minPrice}`);
            throw new Error();
        }
        return price > maxPrice;
    }

    private extractSellData(caller: string, req: Request): { sellerUid: number, itemId: itemId, quantity: number, price: number, listId: number[], expiration: number, isExpensive: boolean, modifyDate: number } {
        const sellerUid = req.body.sellerUid;
        const itemId = req.body.itemId;
        const quantity = req.body.quantity;
        const price = req.body.price;
        const listId = req.body.listId;
        const expiration = toNumberOrZero(req.body.expiration);
        const modifyDate = req.body.modifyDate;

        // Đảm bảo các giá trị là hợp lệ
        validateData(caller, this._logger, sellerUid, itemId, quantity, price);
        // Đảm bảo listId là một mảng số hợp lệ
        validateNumberList(caller, this._logger, listId);

        const isExpensive = this.checkPrice(sellerUid, price, itemId, expiration);

        return { sellerUid, itemId, quantity, price, listId, expiration, isExpensive, modifyDate };
    }
}
