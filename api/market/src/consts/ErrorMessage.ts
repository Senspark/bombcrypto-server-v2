export const MarketError = {
    // UNHANDLED_ERROR là lỗi ko biết trước đc bị throw bới catch exception
    UNHANDLED_ERROR: JSON.stringify({message: "Unhandled error", code: 6000}),

    // Các lỗi do dev chủ động check đc và gủi về cho server
    MISSING_DATA: JSON.stringify({message: "Missing data", code: 6001}),
    SOLD_OUT_ERROR: JSON.stringify({message: "This item is sold out", code: 6002}),
    TRANSACTION_TIME_OUT: JSON.stringify({message: "Your purchase order expired \n Please try again", code: 6003}),
    ALREADY_SELL_ITEM: JSON.stringify({message: "You are already selling this item", code: 6004}),
    MARKET_CLOSED: JSON.stringify({message: "Market is closed", code: 6005}),
    ITEM_IN_PROGRESS: JSON.stringify({message: "Item purchasing currently in progress \n please try again later", code: 6006}),
}

