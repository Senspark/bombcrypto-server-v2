import {ILogger} from "../../Services";
import crypto from "crypto";
import {Request, Response} from "express";
import {validate} from "uuid";
import {CookieSettings, getCookieSettings} from "../../consts/Consts";
import IpWhitelist from "./IpWhitelist";

export type Token = string | null;

export default class BotSecurityService {
    constructor(
        isProd: boolean,
        isCloud: boolean,
        private readonly _logger: ILogger,
        private readonly _ipWhitelist: IpWhitelist,
    ) {
        this._secure = isCloud;
        this._cookieSettings = getCookieSettings(isProd, isCloud);
    }

    private readonly _secure: boolean;
    private readonly _cookieSettings: CookieSettings;

    /**
     * Chống Bot sử dụng api này (cơ bản)
     * Return hash nếu validation hợp lệ
     * Return null nếu ko hợp lệ
     */
    validateUserFirstRequest(walletAddress: string, req: Request, extLogger: ILogger | undefined = undefined): Token {
        const logger = extLogger || this._logger;
        try {
            const headers = req.headers;

            const accept = headers.accept; // application/json
            const isValidAcceptHeader = accept
                && accept.includes("application/json");
            if (!isValidAcceptHeader) {
                logger.error(`[B1] ${walletAddress} Accept header is invalid ${accept}`);
                return null;
            }

            const acceptEncoding = headers["accept-encoding"]; // gzip, br
            const isValidAcceptEncoding = acceptEncoding
                && acceptEncoding.includes("gzip")
                && acceptEncoding.includes("br");
            if (!isValidAcceptEncoding) {
                logger.error(`[B1] ${walletAddress} Accept Encoding header is invalid ${acceptEncoding}`);
                return null;
            }

            const acceptLanguage = headers["accept-language"] ?? ""; // en-US,en;
            if (!acceptLanguage || acceptLanguage.length < 0) {
                // this._logger.error(`[B1] ${walletAddress} Accept Language header is invalid ${acceptLanguage}`);
                // return null;
            }

            // Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3
            // Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:132.0) Gecko/20100101 Firefox/132.0
            const userAgent = headers["user-agent"] ?? "";
            // TODO: Bỏ kiểm tra userAgent vì khó kiểm soát lỗi

            const cookies = req.cookies;
            const clientId = cookies ? cookies[this._cookieSettings.clientToken] : null;
            // K_CLIENT_TOKEN: Client phải generate UUIDv4 và lưu vào cookie K_CLIENT_TOKEN
            // cookie must contains K_CLIENT_TOKEN & K_CLIENT_TOKEN must be UUID

            if (!validate(clientId)) {
                logger.error(`[B1] ${walletAddress} Client Token is invalid ${clientId}`);
                return null;
            }

            return this.hash(accept, acceptEncoding.toString(), acceptLanguage, userAgent, clientId);
        } catch (e) {
            logger.error(`[B1] ${walletAddress} ${e.message}`);
            return null;
        }
    }

    /**
     * Chống Bot sử dụng api này (cơ bản)
     * Return true nếu K_SERVER_TOKEN hợp lệ
     */
    validateUserSecondRequest(walletAddress: string, req: Request, extLogger: ILogger | undefined = undefined): Boolean {
        const logger = extLogger || this._logger;
        const cookies = req.cookies;
        const serverToken = cookies ? cookies[this._cookieSettings.serverToken] : null;
        if (!serverToken) {
            logger.error(`[B2] ${walletAddress} Server Token is missing`);
            return false;
        }

        const hash = this.validateUserFirstRequest(walletAddress, req);
        if (!hash) {
            logger.error(`[B2] ${walletAddress} Hash is missing`);
            return false;
        }

        const valid = hash === serverToken;
        if (!valid) {
            logger.error(`[B2] ${walletAddress} Hash is invalid`);
        }
        return valid;
    }

    /**
     * Chống Bot sử dụng api này (cơ bản)
     * Return true nếu DappClientToken hợp lệ
     */
    validateDappFirstRequest(userName: string, req: Request, extLogger: ILogger | undefined = undefined): Token {
        const logger = extLogger || this._logger;
        try {
            const headers = req.headers;

            const accept = headers.accept; // application/json
            const isValidAcceptHeader = accept
                && accept.includes("application/json");
            if (!isValidAcceptHeader) {
                logger.error(`[Dapp1] ${userName} Accept header is invalid ${accept}`);
                return null;
            }

            const cookies = req.cookies;
            const clientId = cookies ? cookies[this._cookieSettings.dappClientToken] : null;
            // K_CLIENT_TOKEN: Client phải generate UUIDv4 và lưu vào cookie K_CLIENT_TOKEN
            // cookie must contains K_CLIENT_TOKEN & K_CLIENT_TOKEN must be UUID

            if (!validate(clientId)) {
                logger.error(`[Dapp1] ${userName} Client Token is invalid ${clientId}`);
                return null;
            }

            return this.hash(accept, userName, clientId);
        } catch (e) {
            logger.error(`[Dapp1] ${userName} ${e.message}`);
            return null;
        }
    }

    /**
     * Dùng cho cho dapp login nhanh ko cần nhập lại username, password
     * Return true nếu Api token hợp lệ
     */
    validateDappSecondRequest(userName: string, req: Request, extLogger: ILogger | undefined = undefined): boolean {
        const logger = extLogger || this._logger;
        const cookies = req.cookies;

        const apiToken = cookies ? cookies[this._cookieSettings.dappApiToken] : null;
        if (!apiToken) {
            logger.error(`[Dapp2] ${userName} Api Token is missing`);
            return false;
        }

        const hash = this.validateDappFirstRequest(userName, req);
        if (!hash) {
            logger.error(`[Dapp2] ${userName} Hash is missing`);
            return false;
        }

        const valid = hash === apiToken;
        if (!valid) {
            logger.error(`[Dapp2] ${userName} Hash is invalid`);
        }

        return valid;
    }

    /**
     * Ghi Server Token vào HttpOnly Cookie
     */
    assignServerTokenToCookie(hash: string, expiredSeconds: number, res: Response) {
        this.assignCookie(this._cookieSettings.serverToken, hash, expiredSeconds, res);
    }

    assignRefreshTokenToCookie(token: string, expiredSeconds: number, res: Response) {
        this.assignCookie(this._cookieSettings.refreshToken, token, expiredSeconds, res);
    }

    assignDappApiTokenToCookie(jwt: string, expiredSeconds: number, res: Response) {
        this.assignCookie(this._cookieSettings.dappApiToken, jwt, expiredSeconds, res);
    }

    getRefreshToken(req: Request): string | null {
        return req.cookies ? req.cookies[this._cookieSettings.refreshToken] : null;
    }

    assignCookie(key: string, value: string, expiredSeconds: number, res: Response) {
        const opt = {httpOnly: true};
        const settings = this._cookieSettings;
        if (this._secure) {
            opt['secure'] = settings.secure;
            opt['sameSite'] = settings.sameSite;
            opt['domain'] = settings.domain;
        }
        opt['maxAge'] = expiredSeconds * 1000;
        res.cookie(key, value, opt);
    }

    hash(...args: string[]): string {
        return this.hashSHA256(args.join(''));
    }

    hashSHA256(input: string): string {
        return crypto.createHash('sha256').update(input).digest('hex');
    }
}