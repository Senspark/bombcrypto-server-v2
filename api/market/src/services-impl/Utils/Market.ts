import {ExpirationType, Item, SellPool} from "@project/services/Market/IMarketPool";

export function getExpirationType(expiration: number): ExpirationType {
    switch (expiration) {
        case 0:
            return ExpirationType.NO_EXPIRED;
        case 604800: // 7 days in seconds
            return ExpirationType.SEVEN_DAYS;
        case 2592000: // 30 days in seconds
            return ExpirationType.THIRTY_DAYS;
        default:
            this._logger.error(`Invalid expiration type: ${expiration}`);
            return ExpirationType.NO_EXPIRED;
    }
}

// Xếp theo giá giảm dần, item thấp nhất sẽ đc mua trước
// Nếu ngang giá thì xếp theo thời gian sửa đổi
// index 0 là item giá cao nhất, index cuối cùng là item giá rẻ nhất
export function sortUserPool(userPool: Item[]): Item[] {
    if (userPool.length > 0) {
        userPool.sort((a, b) => {
            if (a.price === b.price) {
                return b.modifyTime - a.modifyTime; // Sort by modifyTime if prices are equal
            }
            return b.price - a.price; // Sort by price
        });
    }
    return userPool;
}