import {IDatabaseManager, ILogger, IMarketConfig, IMarketDatabaseAccess} from "@project/Services";
import {IDependencies} from "@project/Dependencies";
import {ExpirationType, GeneralConfig, IItemGeneralInfo, itemId, ItemMarketConfig} from "@project/TypeData";
import {getExpirationType} from "@project/services-impl/Utils/Market";
import {toNumberOr} from "@project/services-impl/Utils/Number";


export default class MarketConfig implements IMarketConfig {
    constructor(private _dep: IDependencies, private _marketDatabase: IMarketDatabaseAccess) {
        this._database = _dep.database;
        this._logger = _dep.logger.clone("[MarketConfig]");
    }

    isExpensive(price: number, itemId: number, expiration: number): boolean {
        return price > this.getMaxPriceWithExpiration(itemId, expiration);
    }

    getItemInfo(itemId: itemId): IItemGeneralInfo {
        const itemConfig = this._itemMarketConfig.get(itemId);
        if (!itemConfig) {
            throw new Error(`Item config not found for itemId: ${itemId}`);
        }
        return itemConfig.info;
    }

    getItemMarketConfig(): ItemMarketConfig {
        return this._itemMarketConfig;
    }

    getMaxPriceWithExpiration(itemId: number, expiration: number): number {
        const config = this._itemMarketConfig.get(itemId);
        if (!config) {
            throw new Error(`Item config not found for itemId: ${itemId}`);
        }
        if(!config.isHaveExpiration){
            return config.max;
        }
        const type = getExpirationType(expiration);
        switch (type) {
            case ExpirationType.SEVEN_DAYS:
                return config.max7Days;
            case ExpirationType.THIRTY_DAYS:
                return config.max30Days;
            default:
                return config.max;
        }
    }

    getMaxPrice(itemId: number): number {
        const config = this._itemMarketConfig.get(itemId);
        if (!config) {
            throw new Error(`Item config not found for itemId: ${itemId}`);
        }
        return Math.max(config.max, config.max7Days, config.max30Days);
    }

    getMinPriceFixedItem(itemId: number): number {
        const config = this._itemMarketConfig.get(itemId);
        if (!config) {
            throw new Error(`Item config not found for itemId: ${itemId}`);
        }
        const expirationPrice = Math.min(config.max7Days, config.max30Days);
        return Math.max(config.max, expirationPrice);
    }

    getMinPrice(itemId: number): number {
        const config = this._itemMarketConfig.get(itemId);
        if (!config) {
            throw new Error(`Item config not found for itemId: ${itemId}`);
        }
        return config.min;
    }

    getFee(): number {
        return this._generalConfig.fee;
    }

    getTimeOutMarket(): number {
        return this._generalConfig.timeOutMarket;
    }
    getRefreshMinPrice(): number {
        return this._generalConfig.refreshMinPrice;
    }

    private readonly _database: IDatabaseManager
    private readonly _logger: ILogger

    private readonly _defaultFee: number = 0.2;
    private readonly _defaultRefreshMinPrice: number = 60;
    private readonly _defaultTimeOutMarket: number = 60;
    private _generalConfig: GeneralConfig;
    private _itemMarketConfig: ItemMarketConfig = new Map();
    private _isLoaded: boolean = false;

    public async initialize(): Promise<void> {
        try {
            if (this._isLoaded) {
                return;
            }
            this._isLoaded = true;

            this._generalConfig = await this._loadGeneralConfig();
            this._itemMarketConfig = await this._loadItemMarketConfig();

        } catch (error) {
            this._logger.error(`Failed to load config: ${error}`);
            this._isLoaded = false;
            throw error;
        }
    }

    private async _loadGeneralConfig(): Promise<GeneralConfig> {
        let generalConfig = await this._marketDatabase.getGeneralConfig();
        generalConfig.fee = toNumberOr(generalConfig.fee, this._defaultFee)
        generalConfig.refreshMinPrice = toNumberOr(generalConfig.refreshMinPrice, this._defaultRefreshMinPrice) * 1000;
        generalConfig.timeOutMarket = toNumberOr(generalConfig.timeOutMarket, this._defaultTimeOutMarket) * 1000;
        return generalConfig;
    }

    private async _loadItemMarketConfig(): Promise<ItemMarketConfig> {
        return await this._marketDatabase.getItemMarketConfig();
    }
}