import {ILogger} from "../../Services";

/**
 * Số lỗi trong 1 đơn vị thời gian được tính là BAN
 */
const ERROR_COUNT_THRESHOLD = 15;

/**
 * Khung thời gian để xem xét số lỗi
 */
const ERROR_TIME_THRESHOLD = 1000 * 60 * 5; // 5 minutes

/**
 * Sẽ đếm số lần lỗi của 1 user & đưa ra quyết đinh
 */
export default class ErrorTracking {

    constructor(logger: ILogger) {
        this._logger = logger.clone('[UBM]');
    }

    private readonly _logger: ILogger;
    private readonly _errorMap: Map<string, Map<ErrorCodes, number[]>> = new Map();

    markError(walletAddress: string, errorCode: ErrorCodes) {
        // const storage = this._errorMap.get(walletAddress) || new Map();
        // const histories = storage.get(errorCode) || [];
        // histories.push(Date.now());
        // storage.set(errorCode, histories);
        // this._errorMap.set(walletAddress, storage);
    }

    isSuitableToBanned(walletAddress: string) {
        return false;
        // const storage = this._errorMap.get(walletAddress);
        // if (!storage) {
        //     return false;
        // }
        // const now = Date.now();
        // for (const [errorCode, histories] of storage) {
        //     const count = histories.filter(h => now - h < ERROR_TIME_THRESHOLD).length;
        //     if (count > ERROR_COUNT_THRESHOLD) {
        //         this._logger.info(`User ${walletAddress} is suitable to banned because of error code ${errorCode}`);
        //         return true;
        //     }
        // }
        // return false;
    }

    clearData(walletAddress: string) {
        this._errorMap.delete(walletAddress);
    }
}

export enum ErrorCodes {
    Unknown,
    DecryptError,
    MissingPrivateKey,
    SameAesKeyUsed,
    JwtExpired,
}