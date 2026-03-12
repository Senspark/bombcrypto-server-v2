import {IDependencies} from "../../../DependenciesInjector";
import {CHAIN} from "../../../dto/CHAIN";
import {CheckProofRequestDto} from "../../../dto/CheckProofRequest.dto";
import {Address} from "@ton/ton";
import {ILogger} from "../../../Services";
import * as crypto from "node:crypto";
import {Buffer} from "buffer";
import JwtService from "../../JwtService";
import {JwtTokenInfo, LoginData, RedisConfig, RedisKeys} from "../../../consts/Consts";
import RsaEncryption from "../../encrypt/RsaEncryption";
import IAutoExpireMap from "../../utils/IAutoExpireMap";
import IObfuscate from "../../encrypt/IObfuscate";
import {AppendBytesObfuscate} from "../../encrypt/AppendBytesObfuscate";
import RedisExpireMap from "../../utils/RedisExpireMap";
import DatabaseAccess from "../../DatabaseAccess";

type WalletAddress = string;
type KeyPair = [publicKey: string, privateKey: string];

const K_APPENDED_BYTES = 16;

export default class TonLoginService {
    constructor(
        private readonly _dep: IDependencies,
        private readonly _jwtService: JwtService,
    ) {
        this.#logger = _dep.logger.clone('[TON_LOGIN]');
        const telegramBotTokens = _dep.envConfig.telegramBotTokens;
        this._rsa = new RedisExpireMap(this.#logger, this._dep.redis, RedisKeys.AP_TON_LOGIN_PRIVATE_KEY, ['', ''] as KeyPair);
        // this.#logger.info(`Telegram bot tokens: ${telegramBotTokens.join(',')}`);

        this.#telegramBotSecrets = telegramBotTokens.map(e =>
            crypto.createHmac('sha256', "WebAppData").update(e).digest());
        this._deobfuscate = new AppendBytesObfuscate(K_APPENDED_BYTES);
        this._databaseAccess = this._dep.generalServices.databaseAccess;
    }

    readonly #logger: ILogger;
    readonly #telegramBotSecrets: Buffer[];
    private readonly _rsa: IAutoExpireMap<WalletAddress, KeyPair>;
    private readonly _deobfuscate: IObfuscate;
    private readonly _databaseAccess: DatabaseAccess;

    public async createLoginToken(request: CheckProofRequestDto): Promise<ILoginPayload | null> {
        const refreshToken = `${Date.now()}${this.randNumber()}`;
        const address = request.address
        const token = await this._jwtService.createLoginToken({
            address: address,
            network: request.network
        } as IJwtTokenPayload);

        const success = await this._dep.redis.hashes.addWithTTL(
            RedisKeys.AP_TON_LOGIN_JWT_REFRESH,
            new Map().set(refreshToken, address), RedisConfig.K_REFRESH_TOKEN_EXPIRED);
        if (success) {
            return {token: token, address: address, refreshToken: refreshToken};
        }
        this.#logger.error(`${address} Failed to save refresh token to redis`);
        return null;
    }

    public randNumber(): number {
        // generate random number from 1_000_000 to 9_999_999
        const min = 1_000_000;
        const max = 9_999_999;
        return Math.floor(Math.random() * max) + min;
    }

    public async refreshJwt(refreshToken: string, address: string, network: string): Promise<JwtTokenInfo | null> {
        if (!refreshToken || !address) {
            return null;
        }
        const jwt = await this._jwtService.createLoginToken(
            {
                address: address,
                network: network
            } as IJwtTokenPayload
        );
        const success = await this._dep.redis.hashes.setTTL(
            RedisKeys.AP_TON_LOGIN_JWT_REFRESH, [refreshToken], RedisConfig.K_REFRESH_TOKEN_EXPIRED);
        if (success) {
            return {
                jwt: jwt,
                refreshToken: refreshToken,
                walletAddress: address
            }
        }
        return null;
    }

    public async refreshJwtToAddress(refreshToken: string): Promise<string | null> {
        return await this._dep.redis.hashes.readField(RedisKeys.AP_TON_LOGIN_JWT_REFRESH, refreshToken) ?? null;
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

    private async verifyLoginToken(loginToken: string): Promise<IAddressPack | undefined> {
        try {
            const payload = (await this._jwtService.verifyToken(loginToken)) as IJwtTokenPayload | null;
            if (!payload || !payload.address) {
                this.#logger.error('Invalid wallet address');
                return undefined;
            }
            // if (payload.network !== this.#currentNetwork) {
            //     this.#logger.error('Invalid network');
            //     return undefined;
            // }
            const friendlyAddress = Address.parse(payload.address).toString({
                bounceable: false,
                testOnly: payload.network === CHAIN.TESTNET
            });
            return {
                rawAddress: payload.address,
                friendlyAddress: friendlyAddress
            };
        } catch (e) {
            this.#logger.error(e.message);
            return undefined;
        }
    }

    public async verifyTelegramUserInfo(walletAddress: string, loginData: string): Promise<IUserLoginInfo | undefined> {
        try {
            if (loginData == null || loginData.length === 0) {
                this.#logger.error(`${walletAddress} missing data`);
                return undefined;
            }

            const rsaKeys = await this._rsa.get(walletAddress);
            if (!rsaKeys) {
                this.#logger.error(`${walletAddress} private key not found`);
                return undefined;
            }

            const rsa = new RsaEncryption(this._dep.envConfig.rsaDelimiter);
            rsa.importPrivateKey(rsaKeys[1]);

            let obj = this.decryptData(rsa, walletAddress, loginData);

            const currentAesKey = this._deobfuscate.deobfuscate(obj.aesKey);
            this.#logger.info(`${walletAddress} decrypted aes key: ${currentAesKey}`);

            const addressPack = await this.verifyLoginToken(obj.encryptedJwt);
            if (!addressPack) {
                this.#logger.error(`${walletAddress} invalid jwt`);
                return undefined;
            }

            if (addressPack.rawAddress !== walletAddress) {
                this.#logger.error(`${walletAddress} Invalid wallet address (${addressPack.rawAddress}) ${!addressPack.rawAddress ? obj.encryptedJwt : ''}`);
                return undefined;
            }

            //extra data
            const telegramUserInfo = obj.extraData;
            let user;

            // Client mới ko có chơi qua telegram nữa nên ko có telegram data, này để cho các client cũ còn đang ở telegram
            if (telegramUserInfo) {
                const isValid = await this.validateTelegramInitData(walletAddress, telegramUserInfo!!);
                if (!isValid) {
                    this.#logger.error(`${walletAddress} Malformed telegram user info`);
                    return undefined;
                }
                const params = Object.fromEntries(new URLSearchParams(telegramUserInfo));
                user = JSON.parse(params.user);
            }

            return {
                telegramUserId: user?.id ?? "",
                telegramUserName: user ? `${user["first_name"] ?? undefined} ${user["last_name"] ?? undefined}` : undefined,
                friendlyAddress: addressPack.friendlyAddress,
                aesKey: currentAesKey
            };
        } catch (e) {
            this.#logger.error(`${walletAddress} ${e.message}`);
            return undefined;
        }
    }

    private decryptData(rsa: RsaEncryption, walletAddress: WalletAddress, loginData: string) {
        try {
            const decrypted = rsa.decrypt(loginData);
            return JSON.parse(decrypted) as LoginData;
        } catch (e) {
            throw new Error(`${walletAddress} failed to decrypt login data ${loginData}`);
        }
    }


    public async validateTelegramInitData(userName: string, telegramInitData: string): Promise<boolean> {
        if (!this._dep.envConfig.isProduction) {
            const initData = new URLSearchParams(telegramInitData);
            await this._databaseAccess.checkToUpdateTelegramId(initData, userName);
            return true;
        }
        const initData = new URLSearchParams(telegramInitData);
        const hash = initData.get('hash');
        const dataToCheck: string[] = [];
        initData.sort();
        initData.forEach((v, k) => k !== 'hash' && dataToCheck.push(`${k}=${v}`));
        let isValid = false;
        for (const s of this.#telegramBotSecrets) {
            const _hash = crypto.createHmac('sha256', s).update(dataToCheck.join('\n')).digest('hex');
            isValid = hash === _hash;
            if (isValid) {
                break;
            }
        }

        this.#logger.info(`Validate [${isValid ? "TRUE" : "FALSE"}] with telegram init data: ${telegramInitData}`);
        // Luư id telegram của user vào database mỗi khi login
        if (isValid) {
            await this._databaseAccess.checkToUpdateTelegramId(initData, userName);
        }
        return isValid;
    }
}

export interface IJwtTokenPayload {
    address: string;
    network: CHAIN;
}

interface ILoginPayload {
    token: string;
    address: string;
    refreshToken: string;
}

interface IUserLoginInfo {
    telegramUserId: string;
    telegramUserName: string | undefined;
    friendlyAddress: string;
    aesKey: string;
}


interface IAddressPack {
    rawAddress: string;
    friendlyAddress: string;
}

