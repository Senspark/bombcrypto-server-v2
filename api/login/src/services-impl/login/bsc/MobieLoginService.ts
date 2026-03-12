import JwtService from "../../JwtService";
import {ILogger, IRedisDatabase} from "../../../Services";
import {JwtExpired, JwtTokenInfo, RedisConfig} from "../../../consts/Consts";
import RsaEncryption from "../../encrypt/RsaEncryption";
import AesEncryption from "../../encrypt/AesEncryption";
import IObfuscate from "../../encrypt/IObfuscate";
import {AppendBytesObfuscate} from "../../encrypt/AppendBytesObfuscate";
import IAutoExpireMap from "../../utils/IAutoExpireMap";
import RedisExpireMap from "../../utils/RedisExpireMap";
import ErrorTracking, {ErrorCodes} from "../../utils/ErrorTracking";
import {JWTPayload} from "jose";
import {randNumber} from "../../../utils/Random";
import {emptyString} from "../../../utils/String";
import DatabaseAccess from "../../DatabaseAccess";

import {IDependencies} from "../../../DependenciesInjector";

type WalletAddress = string;
type KeyPair = [publicKey: string, privateKey: string];

type JWT_AUTH = string;
const K_APPENDED_BYTES = 16;

export default class MobileLoginService {
    constructor(
        private readonly _dep: IDependencies,
        private readonly _privateKeyRedisKey: string,
        private readonly _jwtRefreshRedisKey: string,
    ) {
        this._logger = _dep.logger.clone('[MOBILE_LOGIN]');
        this._redis = _dep.redis;
        this._databaseAccess = _dep.generalServices.databaseAccess;
        this._jwtService = _dep.generalServices.jwtService;
        this._errorTracking = new ErrorTracking(this._logger)
        this._encryptor = new AesEncryption();
        this._encryptor.importKey(_dep.envConfig.aesSecret);
        this._rsa = new RedisExpireMap(this._logger, this._redis, _privateKeyRedisKey, ['', ''] as KeyPair);
        this._deobfuscate = new AppendBytesObfuscate(K_APPENDED_BYTES);
    }


    private readonly _logger: ILogger;
    private readonly _encryptor: AesEncryption;
    private readonly _rsa: IAutoExpireMap<WalletAddress, KeyPair>;
    private readonly _deobfuscate: IObfuscate;
    private readonly _redis: IRedisDatabase;
    private readonly _databaseAccess: DatabaseAccess;
    private readonly _jwtService: JwtService;
    private readonly _errorTracking: ErrorTracking;

    public async refreshJwt(userName: string): Promise<JwtTokenInfo | null> {
        if (!userName) {
            return null;
        }
        const jwt = await this.generateJwt(userName);
        if (!jwt) {
            return null;
        }
        return {
            jwt: jwt.jwt,
            refreshToken: emptyString(),
            walletAddress: userName
        }
    }

    private async verifyJwtAndGetData(jwt: string): Promise<JWTPayload | null> {
        const payload = await this._jwtService.verifyToken(jwt);
        if (!payload) {
            return null;
        }
        return payload;
    }

    public async verifyLoginData(userName: string, loginData: string): Promise<UserInfoData> {
        if (loginData == null || loginData.length === 0) {
            throw new Error(`${userName} invalid login data`);
        }

        const rsaKeys = await this._rsa.get(userName);
        if (!rsaKeys) {
            this._errorTracking.markError(userName, ErrorCodes.MissingPrivateKey);
            throw new Error(`${userName} private key not found`);
        }

        const rsa = new RsaEncryption(this._dep.envConfig.rsaDelimiter);
        rsa.importPrivateKey(rsaKeys[1]);

        let obj = this.decryptData(rsa, userName, loginData);

        const currentAesKey = this._deobfuscate.deobfuscate(obj.aesKey);
        this._logger.info(`${userName} decrypted aes key: ${currentAesKey}`);

        const jwtPayLoad = await this.verifyJwtAndGetData(obj.encryptedJwt);

        if (!jwtPayLoad) {
            this._errorTracking.markError(userName, ErrorCodes.JwtExpired);
            throw new Error(`${userName} Invalid jwt payload ${obj.encryptedJwt}`);
        }

        return {
            aesKey: currentAesKey,
            extraData: obj.extraData,
            userName: userName,
            jwtPayload: jwtPayLoad
        };
    }

    public async generateJwt(userName: string): Promise<JwtTokenInfoAccount | null> {
        const refreshToken = `${Date.now()}${randNumber()}`;
        const jwt = await this.createJwtAuth(userName);
        if (!jwt) {
            return null;
        }
        const success = await this._redis.hashes.addWithTTL(
            this._jwtRefreshRedisKey,
            new Map().set(refreshToken, userName), RedisConfig.K_REFRESH_TOKEN_EXPIRED);
        if (success) {
            return {
                jwt: jwt,
                refreshToken: refreshToken,
            };
        }
        this._logger.error(`${userName} Failed to save refresh token to redis`);
        return null;
    }

    private async createJwtAuth(userName: string, expired: string = JwtExpired.K_JWT_AUTH_EXPIRED): Promise<JWT_AUTH> {
        const data: JwtPayloadGuest = {
            userName: userName
        }
        return await this._jwtService.buildCreateToken(expired)(data);
    }

    private decryptData(rsa: RsaEncryption, walletAddress: WalletAddress, loginData: string) {
        try {
            const decrypted = rsa.decrypt(loginData);
            return JSON.parse(decrypted) as LoginData;
        } catch (e) {
            this._errorTracking.markError(walletAddress, ErrorCodes.DecryptError);
            throw new Error(`${walletAddress} failed to decrypt login data ${loginData}`);
        }
    }

    public async queryDatabaseAccount(userName: string, password: string): Promise<UserAccount | null> {
        return await this._databaseAccess.checkAccountSenspark(userName, password);
    }


    async checkGuestAccount(userName: string): Promise<number | null> {
        return await this._databaseAccess.checkGuestAccount(userName);
    }
}

export type UserAccount = {
    uid: number;
    userName: string;
    nickName: string | null;
    email: string | null;
    typeAccount: string;
    createAt: Date;
    address: string;
    isUserFi: boolean;
}

type LoginData = {
    aesKey: string,
    encryptedJwt: string,
    extraData: string
}

export type JwtTokenInfoAccount = {
    jwt: string
    refreshToken: string
}


type JwtPayloadGuest = {
    userName: string;
};

export type UserInfoData = {
    aesKey: string,
    extraData: string,
    userName: string,
    jwtPayload: JWTPayload
}
