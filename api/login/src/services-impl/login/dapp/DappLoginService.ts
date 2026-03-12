import {ILogger, IRedisDatabase} from "../../../Services";
import JwtService from "../../JwtService";
import DatabaseAccess, {UserAccount} from "../../DatabaseAccess";
import BotSecurityService from "../../encrypt/BotSecurityService";
import {JwtDapp, JwtExpired} from "../../../consts/Consts";
import {K_INVALID_DATA_ERR} from "../../../consts/ResponseError";
import BscWalletService from "../../auth/BscWalletService";
import {JWTPayload} from "jose";
import {toDataForDapp} from "../../../utils/AccountUtils";

import IAutoExpireMap from "../../utils/IAutoExpireMap";
import RedisExpireMap from "../../utils/RedisExpireMap";
import {RedisKeys} from "../../../consts/Consts";

export type JWT_DAPP = string;
export type WalletAddress = string;

export interface VerifyAccountResult {
    success: boolean;
    error?: string;
    jwt?: string;
    data?: DataForDapp;
}

export interface VerifyJwtResult {
    success: boolean;
    error?: string;
    data?: DataForDapp;
}

export type DataForDapp = {
    id: number;
    username: string | null;
    email: string;
    address: string;
    nickname?: string;
    avatar?: number | null;
};
const displayText = 'Your code:';
export default class DappLoginService {
    constructor(
        private readonly _logger: ILogger,
        private readonly _jwtService: JwtService,
        private readonly _databaseAccess: DatabaseAccess,
        private readonly _isProd: boolean,
        _redis: IRedisDatabase,
        aesSecret: string,
        dappSignPadding: string,
    ) {
        const nonces: IAutoExpireMap<string, [number, string]> = new RedisExpireMap(
            _logger,
            _redis,
            RedisKeys.AP_WEB_LOGIN_NONCE,
            [0, ''] as [number, string]
        );
        this._wallet = new BscWalletService(_logger, aesSecret, dappSignPadding, displayText, nonces);
    }

    private readonly _wallet: BscWalletService;
    
    /**
     * Verify user account with username and password and create JWT
     */
    public async verifyAccount(userName: string, password: string): Promise<VerifyAccountResult> {
        try {
            // Get data account from database
            const account = await this._databaseAccess.checkAccountSenspark(userName, password);
            if (!account) {
                this._logger.error(`Senspark account not found in database`);
                return {
                    success: false,
                    error: K_INVALID_DATA_ERR
                };
            }

            this._logger.info(`Generating JWT`);
            const jwt = await this.createNewJwtToken(userName);
            if (!jwt) {
                this._logger.error(`Cannot generate JWT`);
                return {
                    success: false,
                    error: K_INVALID_DATA_ERR
                };
            }

            return {
                success: true,
                jwt: jwt,
                data: toDataForDapp(account)
            };
        } catch (e) {
            this._logger.error((e as Error).message);
            return {
                success: false,
                error: 'Internal Server Error'
            };
        }
    }


    /**
     * Verify jwt for senspark account or wallet account
     */
    public async verifyJwt(jwt: JWTPayload): Promise<VerifyJwtResult> {
        try {
            // Verify JWT
            const payload = jwt as JwtDapp;
            if (!payload) {
                // Jwt may be expired, need login again
                this._logger.error(`Invalid JWT`);
                return {
                    success: false,
                    error: K_INVALID_DATA_ERR
                };
            }
            const userName = payload.userName;

            if (!userName) {
                this._logger.error(`Invalid JWT payload`);
                return {
                    success: false,
                    error: K_INVALID_DATA_ERR
                };
            }

            let account: UserAccount | null = null;

            // Check account in database
            account = await this._databaseAccess.getInfoAccountForDapp(userName);
            if (!account) {
                this._logger.error(`Account not found in database`);
                return {
                    success: false,
                    error: K_INVALID_DATA_ERR
                };
            }


            return {
                success: true,
                data: toDataForDapp(account)
            };
        } catch (e) {
            // Need login again
            this._logger.error((e as Error).message);
            return {
                success: false,
                error: 'Internal Server Error'
            };
        }
    }

    private async createNewJwtToken(userName: string): Promise<JWT_DAPP> {
        const data: JwtDapp = {
            userName: userName
        }
        const expired = this._isProd ? JwtExpired.K_JWT_AUTH_DAPP_EXPIRED : JwtExpired.K_JWT_AUTH_DAPP_EXPIRED_TEST;
        return await this._jwtService.buildCreateToken(expired)(data);
    }

    /**
     * Generate nonce data for wallet login
     */
    public async generateNonceData(walletAddress: WalletAddress): Promise<string> {
        return await this._wallet.generateNonceData(walletAddress);
    }

    public async checkProof(walletAddress: WalletAddress, signatureBase64: string): Promise<boolean> {
        try {
            // Check proof of wallet
            return await this._wallet.checkProof(walletAddress, signatureBase64);
        } catch (e) {
            this._logger.error(`Error checking proof for wallet ${walletAddress}: ${(e as Error).message}`);
            return false;
        }
    }

    /**
     * Verify wallet signature and create JWT
     */
    public async verifyWallet(walletAddress: WalletAddress, signatureBase64: string): Promise<VerifyAccountResult> {
        try {
            // Verify wallet signature
            const validSignature = await this._wallet.checkProof(walletAddress, signatureBase64);
            if (!validSignature) {
                this._logger.error(`Invalid signature for wallet ${walletAddress}`);
                return {
                    success: false,
                    error: K_INVALID_DATA_ERR
                };
            }

            const account = await this._databaseAccess.getOrCreateNewWalletBsc(walletAddress);

            if (!account) {
                this._logger.error(`Wallet ${walletAddress} not found in database`);
                return {
                    success: false,
                    error: K_INVALID_DATA_ERR
                };
            }

            // Create JWT token
            this._logger.info(`Generating JWT for wallet ${walletAddress}`);
            const jwt = await this.createNewJwtToken(walletAddress);
            if (!jwt) {
                this._logger.error(`Cannot generate JWT for wallet ${walletAddress}`);
                return {
                    success: false,
                    error: K_INVALID_DATA_ERR
                };
            }

            return {
                success: true,
                jwt: jwt,
                data: toDataForDapp(account)
            };
        } catch (e) {
            this._logger.error((e as Error).message);
            return {
                success: false,
                error: 'Internal Server Error'
            };
        }
    }

}
