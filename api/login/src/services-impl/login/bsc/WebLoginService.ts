import JwtService from "../../JwtService";
import {ILogger, IRedisDatabase} from "../../../Services";
import {JwtExpired, JwtPayload, JwtPayloadAccount, JwtTokenInfo, RedisConfig} from "../../../consts/Consts";
import RsaEncryption from "../../encrypt/RsaEncryption";
import AesEncryption from "../../encrypt/AesEncryption";
import IObfuscate from "../../encrypt/IObfuscate";
import {AppendBytesObfuscate} from "../../encrypt/AppendBytesObfuscate";
import IAutoExpireMap from "../../utils/IAutoExpireMap";
import RedisExpireMap from "../../utils/RedisExpireMap";
import ErrorTracking, {ErrorCodes} from "../../utils/ErrorTracking";
import {JWTPayload} from "jose";
import {AccType} from "../../../Handlers/WebHandler";
import {randNumber} from "../../../utils/Random";
import BscWalletService from "../../auth/BscWalletService";

import {IDependencies} from "../../../DependenciesInjector";

type WalletAddress = string;
type KeyPair = [publicKey: string, privateKey: string];
type JwtRefreshTokenData = [address: string, type: string];

type JWT_AUTH = string;
const K_APPENDED_BYTES = 16;
const DisplayText = 'Your login code:';

export default class WebLoginService {
    constructor(
        private _dep: IDependencies,
        private readonly _privateKeyRedisKey: string,
        private readonly _jwtRefreshRedisKey: string,
        private readonly _wallet: BscWalletService,
    ) {
        this._logger = this._dep.logger.clone('[WEB_LOGIN]');
        this._redis = this._dep.redis;
        this._encryptor = new AesEncryption();
        this._encryptor.importKey(this._dep.envConfig.aesSecret);
        this._rsa = new RedisExpireMap(this._logger, this._redis, _privateKeyRedisKey, ['', ''] as KeyPair);
        this._deobfuscate = new AppendBytesObfuscate(K_APPENDED_BYTES);
        this._jwtService = this._dep.generalServices.jwtService;
        this._errorTracking = new ErrorTracking(this._logger)
    }
    private readonly _errorTracking: ErrorTracking;
    private readonly _redis: IRedisDatabase;
    private readonly _jwtService: JwtService;
    private readonly _logger: ILogger;
    private readonly _encryptor: AesEncryption;
    private readonly _rsa: IAutoExpireMap<WalletAddress, KeyPair>;
    private readonly _deobfuscate: IObfuscate;

    public async generateNonceData(walletAddress: WalletAddress): Promise<string> {
        return await this._wallet.generateNonceData(walletAddress);
    }

    public async checkProof(walletAddress: WalletAddress, signatureBase64: string): Promise<boolean> {
        return await this._wallet.checkProof(walletAddress, signatureBase64);
    }

    public async generateJwtWithRefreshToken(address: WalletAddress, type: AccType): Promise<JwtTokenInfo | null> {
        const refreshToken = `${Date.now()}${randNumber()}`;
        const data = `["${address}","${type}"]`

        let jwt: string;
        if (type === 'wallet') {
            jwt = await this.createJwtAuth(address);
        } else {
            jwt = await this.createJwtAuthForAccount(address);
        }
        const success = await this._redis.hashes.addWithTTL(
            this._jwtRefreshRedisKey,
            new Map().set(refreshToken, data), RedisConfig.K_REFRESH_TOKEN_EXPIRED);
        if (success) {
            return {
                jwt: jwt,
                refreshToken: refreshToken,
                walletAddress: address
            };
        }
        this._logger.error(`${address} Failed to save refresh token to redis`);
        return null;
    }

    public async generateNeverExpireJwt(address: WalletAddress): Promise<JwtTokenInfo | null> {
        const jwt = await this.createJwtAuth(address, '1y');
        return {
            jwt: jwt,
            refreshToken: '',
            walletAddress: address
        };
    }

    public async convertRefreshJwtToAccountData(refreshToken: string): Promise<string | null> {
        return await this._redis.hashes.readField(this._jwtRefreshRedisKey, refreshToken) ?? null;
    }

    public async convertRefreshJwtToAddress(refreshToken: string): Promise<string | null> {
        const data = await this._redis.hashes.readField(this._jwtRefreshRedisKey, refreshToken);
        if (!data) {
            return null;
        }

        try {
            // Parse with strong typing
            const jwtTokenData = JSON.parse(data) as JwtRefreshTokenData;
            
            if (this.isValidJwtRefreshTokenData(jwtTokenData)) {
                // Return the address (first element)
                return jwtTokenData[0].toLowerCase();
            }

            return null;
        } catch (error) {
            return null;
        }
    }

    private isValidJwtRefreshTokenData(data: unknown): data is JwtRefreshTokenData {
        return Array.isArray(data) && 
               data.length === 2 && 
               typeof data[0] === 'string' && 
               typeof data[1] === 'string';
    }

    public async refreshJwt(refreshToken: string, address: string): Promise<JwtTokenInfo | null> {
        if (!refreshToken || !address) {
            return null;
        }
        const jwt = await this.createJwtAuth(address);
        const success = await this._redis.hashes.setTTL(
            this._jwtRefreshRedisKey, [refreshToken], RedisConfig.K_REFRESH_TOKEN_EXPIRED);
        if (success) {
            return {
                jwt: jwt,
                refreshToken: refreshToken,
                walletAddress: address
            }
        }
        return null;
    }

    public async refreshJwtAccount(refreshToken: string, address: string): Promise<JwtTokenInfo | null> {
        if (!refreshToken || !address) {
            return null;
        }
        const jwt = await this.createJwtAuthForAccount(address);
        const success = await this._redis.hashes.setTTL(
            this._jwtRefreshRedisKey, [refreshToken], RedisConfig.K_REFRESH_TOKEN_EXPIRED);
        if (success) {
            return {
                jwt: jwt,
                refreshToken: refreshToken,
                walletAddress: address
            }
        }
        return null;
    }

    public async verifyJwtAndGetData(jwt: string): Promise<JWTPayload | null> {
        const payload = await this._jwtService.verifyToken(jwt);
        if (!payload) {
            return null;
        }
        return payload;
    }

    public async tryCreateRsaPublicKey(walletAddress: WalletAddress): Promise<string> {
        let keys = await this._rsa.get(walletAddress);
        if (keys != null && keys.length == 2) {
            await this._rsa.extendExpireTime(walletAddress, RedisConfig.K_PRIVATE_KEY_EXPIRED);
            return keys[0];
        }
        const rsa = new RsaEncryption(this._dep.envConfig.rsaDelimiter);
        await rsa.generateKeyPair();
        const publicKey = rsa.exportPublicKey();
        const privateKey = rsa.exportPrivateKey();
        await this._rsa.add(walletAddress, [publicKey, privateKey], RedisConfig.K_PRIVATE_KEY_EXPIRED);
        return publicKey;
    }

    public async verifyLoginData(walletAddress: string, loginData: string, extLogger: ILogger | undefined = undefined): Promise<UserInfoData> {
        const logger = extLogger || this._logger;
        if (loginData == null || loginData.length === 0) {
            throw new Error(`${walletAddress} invalid login data`);
        }

        const rsaKeys = await this._rsa.get(walletAddress);
        if (!rsaKeys) {
            this._errorTracking.markError(walletAddress, ErrorCodes.MissingPrivateKey);
            throw new Error(`${walletAddress} private key not found`);
        }

        const rsa = new RsaEncryption(this._dep.envConfig.rsaDelimiter);
        rsa.importPrivateKey(rsaKeys[1]);

        let obj = this.decryptData(rsa, walletAddress, loginData);

        const currentAesKey = this._deobfuscate.deobfuscate(obj.aesKey);
        logger.info(`${walletAddress} decrypted aes key: ${currentAesKey}`);

        const jwtPayLoad = await this.verifyJwtAndGetData(obj.encryptedJwt);

        if (!jwtPayLoad) {
            this._errorTracking.markError(walletAddress, ErrorCodes.JwtExpired);
            throw new Error(`${walletAddress} Invalid jwt payload ${obj.encryptedJwt}`);
        }

        return {
            aesKey: currentAesKey,
            extraData: obj.extraData,
            userName: walletAddress,
            jwtPayload: jwtPayLoad
        };
    }

    private async createJwtAuthForAccount(userName: string, expired: string = JwtExpired.K_JWT_AUTH_EXPIRED): Promise<JWT_AUTH> {
        const data: JwtPayloadAccount = {
            userName: userName,
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

    private async createJwtAuth(address: string, expired: string = JwtExpired.K_JWT_AUTH_EXPIRED): Promise<JWT_AUTH> {
        const data: JwtPayload = {
            address: address
        }
        return await this._jwtService.buildCreateToken(expired)(data);
    }
}

type LoginData = {
    aesKey: string,
    encryptedJwt: string,
    extraData: string
}

export type UserInfoData = {
    aesKey: string,
    extraData: string,
    userName: string,
    jwtPayload: JWTPayload
}
