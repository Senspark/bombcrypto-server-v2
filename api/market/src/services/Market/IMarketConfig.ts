export default interface IMarketConfig {
    initialize(): Promise<void>;
    getFee(): number;
    getTimeOutMarket(): number;
    getRefreshMinPrice(): number;
    getItemMarketConfig(): ItemMarketConfig;
    getMaxPriceWithExpiration(itemId: number, expiration: number): number;
    getMaxPrice(itemId: number): number
    getMinPriceFixedItem(itemId: number): number
    getMinPrice(itemId: number): number;
    getItemInfo(itemId: itemId): IItemGeneralInfo;
    isExpensive(price: number, itemId: number, expiration: number): boolean;
}

export interface ItemConfig {
    min: number;
    max: number;
    isHaveExpiration: boolean;
    max7Days: number;
    max30Days: number;
    fixedAmount: number;
    info: IItemGeneralInfo;
}
export interface IItemGeneralInfo{
    itemName: string;
    itemType: number;
    rewardType: string;
}
export type ItemMarketConfig = Map<itemId, ItemConfig>;
export type itemId = number;
