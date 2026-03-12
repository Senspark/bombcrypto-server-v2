import JwtService from "../../JwtService";
import {ILogger, IRedisDatabase} from "../../../Services";
import nacl from "tweetnacl";
import {JwtExpired, JwtTokenInfo, LoginData, RedisConfig, RedisKeys, UserInfoData} from "../../../consts/Consts";
import RsaEncryption from "../../encrypt/RsaEncryption";
import * as web3 from "@solana/web3.js";
import {PublicKey} from "@solana/web3.js";
import {
    base64ToByteArray,
    byteArrayToBase64,
    mergeByteArray,
    stringToByteArray,
    uint32ToByteArray,
} from "../../../utils/String";
import AesEncryption from "../../encrypt/AesEncryption";
import IObfuscate from "../../encrypt/IObfuscate";
import {AppendBytesObfuscate} from "../../encrypt/AppendBytesObfuscate";
import IAutoExpireMap from "../../utils/IAutoExpireMap";
import RedisExpireMap from "../../utils/RedisExpireMap";
import ErrorTracking, {ErrorCodes} from "../../utils/ErrorTracking";
import {randNumber} from "../../../utils/Random";
import DatabaseAccess from "../../DatabaseAccess";


type WalletAddress = string;
type Nonce = [rand: number, iv: string];
type KeyPair = [publicKey: string, privateKey: string];

type JWT_AUTH = string;
const K_APPENDED_BYTES = 16;

export default class SolLoginService {
    constructor(
        logger: ILogger,
        private readonly _redis: IRedisDatabase,
        private readonly _databaseAccess: DatabaseAccess,
        private readonly _jwtService: JwtService,
        private readonly _errorTracking: ErrorTracking,
        aesSecret: string,
        private readonly _gameSignPadding: string,
        private readonly _rsaDelimiter: string,
    ) {
        this._logger = logger.clone('[SOL_LOGIN]');
        this._encryptor = new AesEncryption();
        this._encryptor.importKey(aesSecret);
        this._rsa = new RedisExpireMap(this._logger, this._redis, RedisKeys.AP_SOL_LOGIN_PRIVATE_KEY, ['', ''] as KeyPair);
        this._nonces = new RedisExpireMap(this._logger, this._redis, RedisKeys.AP_SOL_LOGIN_NONCE, [0, ''] as Nonce);
        this._deobfuscate = new AppendBytesObfuscate(K_APPENDED_BYTES);
    }

    private readonly _logger: ILogger;
    private readonly _nonces: IAutoExpireMap<WalletAddress, Nonce>;
    private readonly _encryptor: AesEncryption;
    private readonly _rsa: IAutoExpireMap<WalletAddress, KeyPair>;
    private readonly _deobfuscate: IObfuscate;

    public async generateNonceData(walletAddress: WalletAddress): Promise<string> {
        const n = randNumber();
        const iv = this._encryptor.generateIV();
        await this._nonces.add(walletAddress, [n, iv], RedisConfig.K_NONCE_EXPIRED);

        // merge bytes n + iv;
        return byteArrayToBase64(mergeByteArray(uint32ToByteArray(n), base64ToByteArray(iv)));
    }

    /**
     * For testing purpose
     */
    public async assignNonce(walletAddress: WalletAddress, nonce: Nonce): Promise<void> {
        await this._nonces.add(walletAddress, nonce, RedisConfig.K_NONCE_EXPIRED);
    }


    public async checkProof(walletAddress: WalletAddress, signatureBase64: string): Promise<boolean> {
        const pWalletAddress = new web3.PublicKey(walletAddress);
        const bWalletAddress = pWalletAddress.toBytes();
        if (!PublicKey.isOnCurve(bWalletAddress)) {
            throw new Error(`${walletAddress} Invalid address`);
        }

        const nonceCompact = await this._nonces.get(walletAddress);

        if (!nonceCompact) {
            throw new Error(`${walletAddress} Nonce not found`);
        }

        const originalMessage = stringToByteArray(this.generateStringToSign(nonceCompact));
        const signedBytes = base64ToByteArray(signatureBase64);

        await this._nonces.remove(walletAddress);

        const verified = nacl.sign.detached.verify(originalMessage, signedBytes, bWalletAddress);
        if (!verified) {
            this._logger.error(`${pWalletAddress.toString()} Signature is invalid`);
            return false;
        }

        return true;
    }

    public async generateJwtWithRefreshToken(address: WalletAddress): Promise<JwtTokenInfo | null> {
        const refreshToken = `${Date.now()}${randNumber()}`;
        const jwt = await this.createJwtAuth(address);
        const success = await this._redis.hashes.addWithTTL(
            RedisKeys.AP_SOL_LOGIN_JWT_REFRESH,
            new Map().set(refreshToken, address), RedisConfig.K_REFRESH_TOKEN_EXPIRED);
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

    public async refreshJwtToAddress(refreshToken: string): Promise<string | null> {
        return await this._redis.hashes.readField(RedisKeys.AP_SOL_LOGIN_JWT_REFRESH, refreshToken) ?? null;
    }

    public async refreshJwt(refreshToken: string, address: string): Promise<JwtTokenInfo | null> {
        if (!refreshToken || !address) {
            return null;
        }
        const jwt = await this.createJwtAuth(address);
        const success = await this._redis.hashes.setTTL(
            RedisKeys.AP_SOL_LOGIN_JWT_REFRESH, [refreshToken], RedisConfig.K_REFRESH_TOKEN_EXPIRED);
        if (success) {
            return {
                jwt: jwt,
                refreshToken: refreshToken,
                walletAddress: address
            }
        }
        return null;
    }

    public async verifyJwtAndGetWalletAddress(jwt: string): Promise<string | null> {
        const payload = await this._jwtService.verifyToken(jwt);
        if (!payload) {
            return null;
        }
        return (payload as JwtPayload).address ?? null;
    }

    public async tryCreateRsaPublicKey(walletAddress: WalletAddress): Promise<string> {
        let keys = await this._rsa.get(walletAddress);
        if (keys != null && keys.length == 2) {
            await this._rsa.extendExpireTime(walletAddress, RedisConfig.K_PRIVATE_KEY_EXPIRED);
            return keys[0];
        }
        const rsa = new RsaEncryption(this._rsaDelimiter);
        await rsa.generateKeyPair();
        const publicKey = rsa.exportPublicKey();
        const privateKey = rsa.exportPrivateKey();
        await this._rsa.add(walletAddress, [publicKey, privateKey], RedisConfig.K_PRIVATE_KEY_EXPIRED);
        return publicKey;
    }

    public async verifyLoginData(walletAddress: string, loginData: string): Promise<UserInfoData> {
        if (loginData == null || loginData.length === 0) {
            throw new Error(`${walletAddress} invalid login data`);
        }

        const rsaKeys = await this._rsa.get(walletAddress);
        if (!rsaKeys) {
            this._errorTracking.markError(walletAddress, ErrorCodes.MissingPrivateKey);
            throw new Error(`${walletAddress} private key not found`);
        }

        const rsa = new RsaEncryption(this._rsaDelimiter);
        rsa.importPrivateKey(rsaKeys[1]);

        let obj = this.decryptData(rsa, walletAddress, loginData);

        const currentAesKey = this._deobfuscate.deobfuscate(obj.aesKey);
        this._logger.info(`${walletAddress} decrypted aes key: ${currentAesKey}`);
        // const oldAesKey = await this._redis.hashes.readField(RedisKeys.AP_SOL_LOGIN_AES_USED, walletAddress);
        //
        // if (oldAesKey === currentAesKey) {
        //     this._errorTracking.markError(walletAddress, ErrorCodes.SameAesKeyUsed);
        //     throw new Error(`${walletAddress} aes key is used`);
        // }
        // await this._redis.hashes.addWithTTL(RedisKeys.AP_SOL_LOGIN_AES_USED, [walletAddress, currentAesKey], SolConfig.K_AES_USED_EXPIRED);

        const jwtWalletAddress = await this.verifyJwtAndGetWalletAddress(obj.encryptedJwt);
        if (jwtWalletAddress !== walletAddress) {
            this._errorTracking.markError(walletAddress, ErrorCodes.JwtExpired);
            throw new Error(`${walletAddress} Invalid wallet address (${jwtWalletAddress}) ${!jwtWalletAddress ? obj.encryptedJwt : ''}`);
        }

        return {
            aesKey: currentAesKey,
            extraData: obj.extraData,
        };
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

    private generateStringToSign(nonce: Nonce): string {
        const display = "Your login code:";
        const message = `${this._gameSignPadding}${nonce[0]}`;
        const encryptedNonce = this._encryptor.encrypt(message, nonce[1]);
        return `${display} ${encryptedNonce}`;
    }
}

type JwtPayload = {
    address: string;
};




