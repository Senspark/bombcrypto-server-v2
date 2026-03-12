import {ILogger, IMarketConfig, IMarketPool} from "@project/Services";
import {IDependencies} from "@project/Dependencies";
import {
    ExpensivePool,
    FixedPool,
    ItemFixed,
    Item,
    OrderData,
    SellData,
    SellerData,
    SellPool,
    itemId,
    ItemSell,
    ExpirationType, ItemFixedConfig, ItemConfig
} from "@project/TypeData";
import {getExpirationType, sortUserPool} from "@project/services-impl/Utils/Market";
import {OrderResult} from "@project/services/Market/IMarketPool";
import MarketRedis from "@project/services-impl/Market/MarketRedis";


const TAG = "[MarketPool]";
export default class MarketPool implements IMarketPool {

    private _isRefreshFixedPool = false;
    constructor(private _dep: IDependencies) {
        this._logger = _dep.logger.clone(TAG);
    }

    getUserPool(): SellPool {
        return this._userPool;
    }
    getFixedPool(): FixedPool {
        return this._fixedPool;
    }
    getExpensivePool(): ExpensivePool {
        return this._expensivePool;
    }

    private _marketConfig: IMarketConfig;

    private readonly _logger: ILogger;
    // Pool của user đăng bán
    private _userPool: SellPool = new Map();
    // Pool cố định dùng khi hết item trong pool user có giá <= max price của item đó
    private _fixedPool: FixedPool = new Map();
    // Pool do user đăng giá cao hơn giá max, pool này sẽ ko bao giờ đc bán, chỉ dùng để hiển thị cho owner thấy trên client
    private _expensivePool: ExpensivePool = new Map();
    private _marketRedis: MarketRedis

    public async Initialize(marketConfig: IMarketConfig, sellPool: SellPool) {
        try {
            this._marketConfig = marketConfig;
            this._marketRedis = new MarketRedis(this._dep, marketConfig);
            await this.createFixedPool(marketConfig);
            this.createUserPool(sellPool, marketConfig);

        } catch (error) {
            this._logger.error(`Failed to create pool: ${error}`);
            throw error;
        }
    }

    // Hàm này dùng để tạo pool cố định cho các item hoặc refill khi qua ngày mới
    private async createFixedPool(marketConfig: IMarketConfig) {
        const fixedPool = await this._marketRedis.getFixedPool();
        if (fixedPool) {
            this._fixedPool = fixedPool;
            return;
        }
        this._logger.info(`Fixed pool not found in redis, creating new fixed pool with ${marketConfig.getItemMarketConfig().size} items`)
        await this.refreshFixedPool();
    }

    public async refreshFixedPool() {
        this._isRefreshFixedPool = true;
        this._logger.info(`Refreshing fixed pool`);
        for (const [itemId, itemConfig] of this._marketConfig.getItemMarketConfig().entries()) {
            await this.createItemFixedPool(itemId, itemConfig);
        }
        this._logger.info(`Fixed pool refreshed`);
        this._isRefreshFixedPool = false;
    }

    private createUserPool(sellPool: SellPool, marketConfig: IMarketConfig) {
        sellPool.forEach((items, itemId) => {
            const userPool: Item[] = [];
            items.forEach(item => {
                const maxPrice = marketConfig.getMaxPriceWithExpiration(itemId, item.expirationAfter);
                if (item.price > maxPrice) {
                    let expensivePool = this._expensivePool.get(item.sellerUid) || [];
                    let expensivePoolItem = expensivePool.find(i =>
                        i.itemId === itemId &&
                        i.price === item.price &&
                        i.expirationAfter === item.expirationAfter
                    );

                    if (!expensivePoolItem) {
                        expensivePoolItem = {
                            itemId,
                            price: item.price,
                            quantity: 0,
                            id: [],
                            expirationAfter: item.expirationAfter,
                            modifyTime: item.modifyTime
                        };
                        expensivePool.push(expensivePoolItem);
                    }

                    expensivePoolItem.quantity += item.quantity;
                    expensivePoolItem.id = [...item.listId, ...expensivePoolItem.id];
                    this._expensivePool.set(item.sellerUid, expensivePool);
                } else {
                    userPool.push(item);
                }
            });
            this._userPool.set(itemId, sortUserPool(userPool));
        });
    }

    // Hàm này dùng để lấy các item có thể mua đc ra chuẩn bị cho user mua
    public getItemForOrder(orderData: OrderData): OrderResult {
        const { itemId, quantity, expiration } = orderData;
        const itemSell: Item[] = [];
        const userPool = this._userPool.get(itemId);

        // Chứa các item đúng itemId nhưng ko đúng expiration để lát push lại vào pool
        const tempItems: Item[] = [];
        let userQuantity = 0;
        let fixedQuantity = 0;
        let totalPrice = 0;

        let remainingQuantity = quantity;

        // Kiểm tra pool user trước
        if (userPool && userPool.length > 0) {
            while (remainingQuantity > 0 && userPool.length > 0) {
                const item = userPool.pop();
                if (!item) {
                    continue;
                }
                //Cùng loại item nhưng khác expiration thì push lại vào pool
                if (item.expirationAfter !== expiration) {
                    tempItems.push(item);
                    continue;
                }

                const takeQuantity = Math.min(item.quantity, remainingQuantity);
                remainingQuantity -= takeQuantity;
                userQuantity += takeQuantity;
                totalPrice += takeQuantity * item.price;

                // Add the item with the taken quantity
                itemSell.push({
                    ...item,
                    quantity: takeQuantity,
                    listId: item.listId.slice(0, takeQuantity),
                });

                // User này còn item nên trả lại pool để bán tiếp cho người khác
                if (item.quantity > takeQuantity) {
                    tempItems.push({
                        ...item,
                        quantity: item.quantity - takeQuantity,
                        listId: item.listId.slice(takeQuantity), // Take the remaining list IDs
                    });
                }
            }

            // Trả lại các item không đúng expiration hoặc còn dư về pool
            userPool.push(...tempItems);
            this._userPool.set(itemId, sortUserPool(userPool));
        }

        // Nếu user pool không đủ item thì lấy từ fixed pool
        if (remainingQuantity > 0 && !this._isRefreshFixedPool) {
            const fixedItems = this.createItemFromFixedPool(itemId, remainingQuantity, expiration);
            if (fixedItems.quantity > 0) {
                itemSell.push({
                    sellerUid: -1, // sellerUid = -1 là item từ fixed pool
                    price: fixedItems.price,
                    quantity: fixedItems.quantity,
                    listId: [],
                    expirationAfter: expiration,
                    modifyTime: 0,
                });
                fixedQuantity += fixedItems.quantity;
                totalPrice += fixedItems.quantity * fixedItems.price;
            }

        }

        if (itemSell.length > 0) {
            const result: ItemSell = {
                items: itemSell,
                itemId,
                fixedQuantity,
                totalQuantity: userQuantity + fixedQuantity,
                totalPrice,
                expiration,
            }
            // Check enough gem
            if(totalPrice > orderData.userGem) {
                this._logger.info(`Not enough gem to buy item ${itemId}`);
                this.returnItemToPool(result)
                return {
                    result: 'NOT_ENOUGH_GEM',
                    totalPrice: totalPrice,
                    totalQuantity:  userQuantity + fixedQuantity,
                }
            }

            return {
                result: 'SUCCESS',
                itemSell: result,
                totalPrice: totalPrice,
                totalQuantity:  userQuantity + fixedQuantity,
            }
        }

        return {
            result: 'SOLD_OUT',
            totalPrice: 0,
            totalQuantity: 0,
        }
    }
    // Hàm này dùng để cho owner của item muốn muốn lấy ra khỏi pool để edit hoăc ko bán nữa
    public getItemBySellerUid(sellerData: SellerData): Item | undefined {
        const {sellerUid, itemId, price, expiration} = sellerData;

        const isExpensive = this._marketConfig.isExpensive(price, itemId, expiration);
        if (isExpensive) {
            const expensiveItems = this._expensivePool.get(sellerUid);

            if (expensiveItems) {
                // Find the exact item that matches itemId, price and expiration
                const expensiveItemIndex = expensiveItems.findIndex(item =>
                    item.itemId === sellerData.itemId &&
                    item.price === price &&
                    item.expirationAfter === expiration
                );

                if (expensiveItemIndex !== -1) {
                    // Remove the item from the expensiveItems array
                    const [expensiveItem] = expensiveItems.splice(expensiveItemIndex, 1);

                    // Update the expensive pool
                    this._expensivePool.set(sellerData.sellerUid, expensiveItems);

                    return {
                        sellerUid: sellerData.sellerUid,
                        price: expensiveItem.price,
                        listId: expensiveItem.id,
                        quantity: expensiveItem.quantity,
                        expirationAfter: expensiveItem.expirationAfter,
                        modifyTime: expensiveItem.modifyTime,
                    };
                }
            }
            return undefined;
        }
        else {
            // Pool của item này
            const userPool = this._userPool.get(itemId) || [];

            // Find the exact item that matches sellerUid, price and expiration
            const foundItemIndex = userPool.findIndex(item =>
                item.sellerUid === sellerData.sellerUid &&
                item.price === price &&
                item.expirationAfter === expiration
            );

            if (foundItemIndex !== -1) {
                // Remove the item from the userPool array
                const [foundItem] = userPool.splice(foundItemIndex, 1);

                // Update the user pool
                this._userPool.set(itemId, userPool);

                return foundItem;
            }

            return undefined;
        }
    }

    // Hàm này dùng để thêm item vào pool của user và expensive pool nếu giá của item lớn hơn max
    public addItemToUserPool(sellData: SellData): boolean {
        const { itemId, sellerUid, price, listId, quantity, expiration, isExpensive, modifyDate } = sellData;

        if (isExpensive) {
            // Get the user's expensive pool
            const expensivePool = this._expensivePool.get(sellerUid) || [];

            // Find existing item with matching criteria
            let targetItem = expensivePool.find(item =>
                item.itemId === itemId &&
                item.expirationAfter === expiration &&
                item.price === price
            );

            if (!targetItem) {
                // Create new item if not found
                targetItem = {
                    itemId,
                    price,
                    quantity,
                    id: listId,
                    expirationAfter: expiration,
                    modifyTime: modifyDate,
                };
                expensivePool.push(targetItem);
            } else {
                // Update existing item
                targetItem.id = [...targetItem.id, ...listId];
                targetItem.quantity += quantity;
            }

            this._expensivePool.set(sellerUid, expensivePool);
        }
        else {
            let userPool = this._userPool.get(itemId) || [];

            // Check if an item with the same attributes already exists
            const existingItemIndex = userPool.findIndex(item =>
                item.sellerUid === sellerUid &&
                item.price === price &&
                item.expirationAfter === expiration
            );

            if (existingItemIndex !== -1) {
                // Update existing item
                const existingItem = userPool[existingItemIndex];
                existingItem.quantity += quantity;
                existingItem.listId = [...existingItem.listId, ...listId];
                existingItem.modifyTime = modifyDate;
            } else {
                // Add new item
                userPool.push({
                    sellerUid,
                    price,
                    listId,
                    quantity,
                    expirationAfter: expiration,
                    modifyTime: modifyDate,
                });
            }

            userPool = sortUserPool(userPool);
            this._userPool.set(itemId, userPool);
        }
        return true;
    }

    // Hàm này dùng để trả item đã hết thời gian order hoặc user cancel vào đúng pool ban đầu
    public returnItemToPool(itemSell: ItemSell): void {
        const { items, itemId } = itemSell;

        // Return items to user pool
        if (items.length > 0) {
            const userPool = this._userPool.get(itemId) || [];

            items.forEach(item => {
                if (item.sellerUid === -1 && !this._isRefreshFixedPool) {
                    // Return to fixed pool
                    const fixedPoolItem = this._fixedPool.get(itemId);
                    this.addItemToFixedPool(fixedPoolItem, item, itemId);
                } else {
                    // Find if item with same characteristics already exists
                    const existingItemIndex = userPool.findIndex(poolItem =>
                        poolItem.sellerUid === item.sellerUid &&
                        poolItem.price === item.price &&
                        poolItem.expirationAfter === item.expirationAfter
                    );

                    if (existingItemIndex !== -1) {
                        // Merge with existing item
                        const existingItem = userPool[existingItemIndex];
                        existingItem.quantity += item.quantity;
                        // Combine listIds if needed
                        existingItem.listId = [...existingItem.listId, ...item.listId];
                        // Update modifyTime if newer
                        if (item.modifyTime > existingItem.modifyTime) {
                            existingItem.modifyTime = item.modifyTime;
                        }
                    } else {
                        // Add as new item
                        userPool.push({
                            sellerUid: item.sellerUid,
                            price: item.price,
                            listId: item.listId,
                            quantity: item.quantity,
                            expirationAfter: item.expirationAfter,
                            modifyTime: item.modifyTime,
                        });
                    }
                }
            });

            this._userPool.set(itemId, sortUserPool(userPool));
        }
    }

    // Hàm này dùng để tạo các item từ fixed pool khi pool user ko đủ
    private createItemFromFixedPool(itemId: itemId, quantity: number, expiration: number): ItemFixed {
        const FixedItemConfig = this._fixedPool.get(itemId);
        if (!FixedItemConfig) {
            this._logger.error(`Item ${itemId} not found in fixed pool`);
            return {
                price: 0,
                quantity: 0,
            }
        }
        const expirationType = getExpirationType(expiration);
        const fixedItem =  this.getFixedItemBaseOnExpirationType(expirationType, FixedItemConfig, quantity);
        this._marketRedis.setItemFixed(itemId, FixedItemConfig).then()
        return fixedItem;
    }

    private addItemToFixedPool(fixedPoolItem: ItemFixedConfig | undefined, item: Item, itemId: number): void {
        const expirationType = getExpirationType(item.expirationAfter);

        if (!fixedPoolItem) {
            // If the fixed pool item does not exist, initialize it
            this._fixedPool.set(itemId, {
                price: 0,
                quantity: 0,
                price7Days: 0,
                price30Days: 0,
                quantity7Days: 0,
                quantity30Days: 0,
            });
        }
        fixedPoolItem = this._fixedPool.get(itemId)!!;

        // Update the existing fixed pool item based on expiration type
        switch (expirationType) {
            case ExpirationType.SEVEN_DAYS:
                fixedPoolItem.quantity7Days += item.quantity;
                fixedPoolItem.price7Days = item.price;
                break;
            case ExpirationType.THIRTY_DAYS:
                fixedPoolItem.quantity30Days += item.quantity;
                fixedPoolItem.price30Days = item.price;
                break;
            default:
                fixedPoolItem.quantity += item.quantity;
                fixedPoolItem.price = item.price;
                break;
        }
        this._marketRedis.setItemFixed(itemId, fixedPoolItem).then();
        this._fixedPool.set(itemId, fixedPoolItem);
    }

    // Nếu item có thời gian 7 và 30 ngày thì số lượng sẽ chia đôi
    private async createItemFixedPool(itemId: number, itemConfig: ItemConfig): Promise<void> {
       const fixedPoolItem: ItemFixedConfig = {
            price: itemConfig.max,
            quantity: itemConfig.isHaveExpiration ? 0 :itemConfig.fixedAmount,
            price7Days: itemConfig.max7Days,
            quantity7Days: itemConfig.isHaveExpiration ? itemConfig.fixedAmount / 2 : 0 ,
            price30Days: itemConfig.max30Days,
            quantity30Days: itemConfig.isHaveExpiration ? itemConfig.fixedAmount / 2 : 0,
       }
       this._fixedPool.set(itemId, fixedPoolItem);
       await this._marketRedis.setItemFixed(itemId, fixedPoolItem);
    }

    getFixedItemBaseOnExpirationType(type: ExpirationType, itemFixed: ItemFixedConfig, quantity: number): ItemFixed {
        const getItemDetails = (price: number, availableQuantity: number, updateQuantity: (newQuantity: number) => void): ItemFixed => {
            const itemCanCreate = Math.min(availableQuantity, quantity);
            updateQuantity(Math.max(0, availableQuantity - itemCanCreate));
            return {
                price,
                quantity: itemCanCreate,
            };
        };

        switch (type) {
            case ExpirationType.SEVEN_DAYS:
                return getItemDetails(itemFixed.price7Days, itemFixed.quantity7Days, newQuantity => itemFixed.quantity7Days = newQuantity);
            case ExpirationType.THIRTY_DAYS:
                return getItemDetails(itemFixed.price30Days, itemFixed.quantity30Days, newQuantity => itemFixed.quantity30Days = newQuantity);
            default:
                return getItemDetails(itemFixed.price, itemFixed.quantity, newQuantity => itemFixed.quantity = newQuantity);
        }
    }

    // Hàm này dùng để thêm trực tiếp một Item vào pool
    public addItemToPool(itemId: number, item: Item): void {
        const isExpensive = this._marketConfig.isExpensive(item.price, itemId, item.expirationAfter);

        if (isExpensive) {
            // Get the user's expensive pool
            const expensivePool = this._expensivePool.get(item.sellerUid) || [];

            // Find existing item with matching criteria
            let targetItem = expensivePool.find(i =>
                i.itemId === itemId &&
                i.expirationAfter === item.expirationAfter &&
                i.price === item.price
            );

            if (!targetItem) {
                // Create new item if not found
                targetItem = {
                    itemId,
                    price: item.price,
                    quantity: item.quantity,
                    id: item.listId,
                    expirationAfter: item.expirationAfter,
                    modifyTime: item.modifyTime,
                };
                expensivePool.push(targetItem);
            } else {
                // Update existing item
                targetItem.quantity += item.quantity;
                targetItem.id = item.listId;
                targetItem.modifyTime = item.modifyTime;
            }

            this._expensivePool.set(item.sellerUid, expensivePool);
        }
        else {
            let userPool = this._userPool.get(itemId) || [];

            // Check if an item with the same attributes already exists
            const existingItemIndex = userPool.findIndex(i => 
                i.sellerUid === item.sellerUid &&
                i.price === item.price &&
                i.expirationAfter === item.expirationAfter
            );

            if (existingItemIndex !== -1) {
                // Update existing item
                const existingItem = userPool[existingItemIndex];
                existingItem.quantity += item.quantity;
                existingItem.listId = item.listId;
                existingItem.modifyTime = item.modifyTime;
            } else {
                // Add new item
                userPool.push(item);
            }

            userPool = sortUserPool(userPool);
            this._userPool.set(itemId, userPool);
        }
    }

    public getCurrentMinPrice(): Map<itemId, number> {
        const currentMinPrice = new Map();

        const itemConfig = this._marketConfig.getItemMarketConfig();
        // Iterate over all items in itemConfig
        for (const [itemId, _] of itemConfig.entries()) {
            const userPool = this._userPool.get(itemId);

            if (userPool && userPool.length > 0) {
                // Find the minimum price in the userPool for the current itemId
                const minPrice = Math.min(...userPool.map(item => item.price));
                currentMinPrice.set(itemId, minPrice);
            } else {
                // If no items are found in the userPool, get the minPrice from marketConfig
                const minPriceFromConfig = this._marketConfig.getMinPriceFixedItem(itemId);
                currentMinPrice.set(itemId, minPriceFromConfig);
            }
        }
        return currentMinPrice;
    }
}

