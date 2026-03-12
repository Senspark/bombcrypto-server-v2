import {IDependencies} from "../DependenciesInjector";
import JwtService from "../services-impl/JwtService";
import {ILogger} from "../Services";
import {Request, Response} from "express";
import IObfuscate from "../services-impl/encrypt/IObfuscate";
import BotSecurityService from "../services-impl/encrypt/BotSecurityService";
import {JwtPayload, JwtPayloadAccount, JwtResponseData, JwtTokenInfo, RedisConfig, RedisKeys} from "../consts/Consts";
import ErrorTracking from "../services-impl/utils/ErrorTracking";
import UserBanManager from "../services-impl/UserBanManager";
import WebLoginService from "../services-impl/login/bsc/WebLoginService";
import CombineLogger from "../services-impl/loggers/CombineLogger";
import {UserAccountCache} from "../services-impl/UserAccountCache";
import {sleep} from "../utils/Time";
import {randomResponse} from "../services-impl/utils/RandomResponse";
import {
    K_INVALID_DATA_ERR,
    K_MISSING_DATA_ERR,
    K_SECRET_ERR,
    K_SERVER_ERR,
    K_WRONG_DATA
} from "../consts/ResponseError";
import DatabaseAccess from "../services-impl/DatabaseAccess";
import ProfileService from "../services-impl/auth/ProfileService";
import BscWalletService from "../services-impl/auth/BscWalletService";

import IAutoExpireMap from "../services-impl/utils/IAutoExpireMap";
import RedisExpireMap from "../services-impl/utils/RedisExpireMap";
import {removeNameSuffix} from "../services-impl/utils/UserNameSuffix";

const EDITOR_USERNAME_PREFIX = "editor";

const VERSION_HEADER = 'x-bc-version';


export class BscHandlers {
    constructor(
        private readonly _dep: IDependencies,
    ) {
        this._logger = _dep.logger.clone('[BSC]');

        // Create BscWalletService first
        const displayText = 'Your login code:';
        const nonces: IAutoExpireMap<string, [number, string]> = new RedisExpireMap(
            this._logger,
            this._dep.redis,
            RedisKeys.AP_BSC_LOGIN_NONCE,
            [0, ''] as [number, string]
        );
        const wallet = new BscWalletService(
            this._logger,
            _dep.envConfig.aesSecret,
            _dep.envConfig.gameSignPadding,
            displayText,
            nonces
        );

        // Inject BscWalletService into WebLoginService
        this._loginService = new WebLoginService(
            _dep,
            RedisKeys.AP_BSC_LOGIN_PRIVATE_KEY,
            RedisKeys.AP_BSC_LOGIN_JWT_REFRESH,
            wallet
        );

        const generalServices = _dep.generalServices;

        this._jwtService = generalServices.jwtService;
        this._obfuscate = generalServices.obfuscate;
        this._botSecurity = generalServices.botSecurityService;
        this._userAccountCache = generalServices.userAccountCache;
        this._databaseAccess = generalServices.databaseAccess;

        // Create services not available in GeneralServices
        const errorTracking = new ErrorTracking(this._logger);
        this._userBanManager = new UserBanManager(this._logger, _dep.envConfig.syncBannedList, errorTracking, _dep.redis, _dep.scheduler);
        this._profileService = new ProfileService(this._dep);

    }

    private readonly _loginService: WebLoginService

    private readonly _logger: ILogger;
    private readonly _jwtService: JwtService;
    private readonly _obfuscate: IObfuscate;
    private readonly _botSecurity: BotSecurityService;
    private readonly _userBanManager: UserBanManager;
    private readonly _userAccountCache: UserAccountCache;
    private readonly _databaseAccess: DatabaseAccess;
    private readonly _profileService: ProfileService;

    public async generateNonce(req: Request, res: Response) {
        const logger = new CombineLogger('[NONCE]', this._logger.clone(''));
        const now = Date.now();
        await this._generateNonce(req, res, logger);
        const done = Date.now();
        logger.info(`Time: ${done - now}ms`);
        logger.dump();
    }

    /**
     * Call from Client
     */
    public async _generateNonce(req: Request, res: Response, logger: ILogger) {
        try {
            const walletAddress = req.body.walletAddress;
            const clientVersion = extractClientVersionFromHeader(req);
            if (!walletAddress) {
                logger.error('Missing wallet address');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            if (this._userBanManager.isBanned(walletAddress)) {
                logger.info(`${walletAddress}(${clientVersion}) is banned (generateNonce)`);
                return res.sendError(K_SECRET_ERR);
            }

            const nonce = await this._loginService.generateNonceData(walletAddress);
            logger.info(`${walletAddress}(${clientVersion}) generateNonce success`);
            return res.sendSuccess({nonce: nonce});
        } catch (e) {
            logger.error((e as Error).message);
            return res.sendGenericError();
        }
    }

    public async checkProof(req: Request, res: Response) {
        const logger = new CombineLogger('[WALLET]', this._logger.clone(''));
        const now = Date.now();
        await this._checkProof(req, res, logger);
        const done = Date.now();
        logger.info(`Time: ${done - now}ms`);
        logger.dump();
    }

    /**
     * Call from Client
     */
    public async _checkProof(req: Request, res: Response, logger: ILogger) {
        try {
            const walletAddress = req.body.walletAddress;
            const signature = req.body.signature;
            const clientVersion = extractClientVersionFromHeader(req);

            if (!walletAddress || !signature) {
                logger.error('missing wallet address or signature');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            logger.info(`${walletAddress} (${clientVersion})`);

            if (this._userBanManager.isBanned(walletAddress)) {
                logger.info(`is banned`);
                return res.sendError(K_SECRET_ERR);
            }

            const verified = await this._loginService.checkProof(walletAddress, signature);
            if (!verified) {
                logger.error(`checkProof failed`);
                return res.sendError(K_INVALID_DATA_ERR);
            }

            const authInfo = await this._loginService.generateJwtWithRefreshToken(walletAddress, 'wallet');
            if (!authInfo) {
                logger.error(`cannot generate jwt`);
                return res.sendError(K_SERVER_ERR);
            }
            const rsaPublicKey = await this.tryCreateRsaPublicKey(walletAddress);
            const extraData = {
                version: this._dep.envConfig.versionWeb
            }
            const resData: JwtResponseData = {
                auth: authInfo.jwt,
                rf: authInfo.refreshToken,
                key: rsaPublicKey,
                extraData: JSON.stringify(extraData)
            }
            logger.info(`success`);
            return res.sendSuccess(resData);
        } catch (e) {
            logger.error((e as Error).message);
            return res.sendGenericError();
        }
    }

    public async checkProofAccount(req: Request, res: Response) {
        const logger = new CombineLogger('[ACC]', this._logger.clone(''));
        const now = Date.now();
        await this._checkProofAccount(logger, req, res);
        const done = Date.now();
        logger.info(`Time: ${done - now}ms`);
        logger.dump();
    }

    /**
     * Call from Client (use for username and password)
     */
    public async _checkProofAccount(logger: ILogger, req: Request, res: Response) {
        try {
            const userName = req.body.userName;
            const password = req.body.password;
            const clientVersion = extractClientVersionFromHeader(req);

            if (!userName || !password) {
                logger.error('missing wallet userName or password');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            logger.info(`${userName} (${clientVersion})`);

            // Get data account from database
            const account = await this._databaseAccess.checkAccountSenspark(userName, password);

            if (!account) {
                logger.error(`not found in database`);
                return res.sendError(K_INVALID_DATA_ERR);
            }

            logger.info(`generate Jwt`);
            const authInfo = await this._loginService.generateJwtWithRefreshToken(userName, 'account');
            if (!authInfo) {
                logger.error(`cannot generate jwt`);
                return res.sendError(K_INVALID_DATA_ERR);
            }

            logger.info(`generate Rsa`);
            const rsaPublicKey = await this.tryCreateRsaPublicKey(userName);
            const extraData = {
                version: this._dep.envConfig.versionWeb,
                isUserFi: account.isUserFi,
                address: account.address,
            }
            const resData = {
                auth: authInfo.jwt,
                key: rsaPublicKey,
                rf: authInfo.refreshToken,
                extraData: JSON.stringify(extraData)
            };

            await this._userAccountCache.set(userName, account);

            logger.info(`success`);
            return res.sendSuccess(resData);
        } catch (e) {
            logger.error((e as Error).message);
            return res.sendGenericError();
        }
    }

    public async refreshJwtToken(req: Request, res: Response) {
        const logger = new CombineLogger('[REFRESH]', this._logger.clone(''));
        const now = Date.now();
        await this._refreshJwtToken(req, res, logger);
        const done = Date.now();
        logger.info(`Time: ${done - now}ms`);
        logger.dump();
    }

    /**
     * Call from Client
     */
    public async _refreshJwtToken(req: Request, res: Response, logger: ILogger) {
        try {
            const refreshToken: string = req.params['rf'];
            const clientVersion = extractClientVersionFromHeader(req);
            if (!refreshToken) {
                logger.error('missing refresh token');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            logger.info(`rt=${refreshToken} (${clientVersion})`);

            const data = await this._loginService.convertRefreshJwtToAccountData(refreshToken);

            if (!data) {
                logger.error(`missing data`);
                return res.sendError(K_INVALID_DATA_ERR);
            }

            const parsedArray = JSON.parse(data);
            if (!parsedArray || parsedArray.length < 2) {
                logger.error(`cannot refresh jwt ${parsedArray} length < 2`);
                return res.sendError(K_INVALID_DATA_ERR);
            }
            const isWallet = parsedArray[1] === 'wallet';
            const walletAddress = parsedArray[0];

            if (!walletAddress) {
                logger.error('missing wallet address');
                return res.sendError(K_INVALID_DATA_ERR);
            }

            logger.info(walletAddress);

            if (this._userBanManager.isBanned(walletAddress)) {
                logger.info(`is banned`);
                return res.sendError(K_SECRET_ERR);
            }

            let newAuthToken: JwtTokenInfo | null;

            if (isWallet) {
                newAuthToken = await this._loginService.refreshJwt(refreshToken, walletAddress);
            } else {
                newAuthToken = await this._loginService.refreshJwtAccount(refreshToken, walletAddress);
            }
            if (!newAuthToken) {
                logger.error(`cannot refresh jwt (error)`);
                return res.sendError(K_INVALID_DATA_ERR);
            }

            if (this._userBanManager.isBanned(newAuthToken.walletAddress)) {
                logger.info(`is banned`);
                return res.sendError(K_SECRET_ERR);
            }

            const rsaPublicKey = await this.tryCreateRsaPublicKey(newAuthToken.walletAddress);
            const extraData = {
                version: this._dep.envConfig.versionWeb
            }
            const resData: JwtResponseData = {
                auth: newAuthToken.jwt,
                key: rsaPublicKey,
                extraData: JSON.stringify(extraData)
            };

            logger.info(`success`);
            return res.sendSuccess(resData);
        } catch (e) {
            logger.error((e as Error).message);
            return res.sendGenericError();
        }
    }

    public async verifyLoginData(req: Request, res: Response) {
        const logger = new CombineLogger('[VERIFY]', this._logger.clone(''));
        const now = Date.now();
        await this._verifyLoginData(req, res, logger);
        const done = Date.now();
        logger.info(`Time: ${done - now}ms`);
        logger.dump();
    }

    /**
     * Call from Server
     */
    public async _verifyLoginData(req: Request, res: Response, logger: ILogger) {
        try {
            await this._dep.bearerService.verifyBearer(req);

            // Server tự remove suffix và lowercase nếu cần rồi
            const walletAddress = req.body.walletAddress;
            // const walletAddress = removeNameSuffix(walletAddressSuffix);

            const loginData = req.body.loginData;

            if (!loginData) {
                const err = 'missing login data';
                logger.error(err);
                return res.sendError(err);
            }

            logger.info(walletAddress);

            if (this._userBanManager.isBanned(walletAddress)) {
                logger.info(`is banned`);
                return res.sendError(K_SECRET_ERR);
            }

            const userInfo = await this._loginService.verifyLoginData(walletAddress, loginData, logger);
            let account = await this._userAccountCache.get(walletAddress);

            if (account) {
                logger.info(`use account from cache`)
            } else {
                logger.info(`use account from database`)
                if (this.isJwtPayloadWallet(userInfo.jwtPayload)) {
                    // BscWalletService
                    account = await this._databaseAccess.getOrCreateNewWalletBsc(walletAddress);
                } else if (this.isJwtPayloadAccount(userInfo.jwtPayload)) {
                    // Account senspark
                    account = await this._databaseAccess.getInfoAccountBsc(walletAddress);
                } else {
                    logger.error(`jwtPayload not match any type`);
                    return res.sendError(K_SERVER_ERR);
                }

                if (account) {
                    await this._userAccountCache.set(walletAddress, account);
                } else {
                    logger.error(`account not found`);
                    return res.sendError(K_SERVER_ERR);
                }
            }

            logger.info(`success`);
            return res.sendSuccess({
                userId: account.uid,
                userName: account.userName,
                address: account.address ?? '',
                isUserFi: account.isUserFi,
                nickName: account.nickName,
                createAt: account.createAt.getTime(), // epochMilis
                aesKey: userInfo.aesKey,
                extraData: userInfo.extraData
            });

        } catch (e) {
            logger.error((e as Error).message);
            return res.sendError(e.message, 403);
        }
    }

    public async getBannedList(req: Request, res: Response) {
        try {
            res.sendRawJson(this._userBanManager.exportBannedList());
        } catch (e) {
            this._logger.error((e as Error).message);
            res.sendError(e.message, 403);
        }
    }

    public async checkServerMaintain(req: Request, res: Response) {
        try {
            //Server test ko cần check
            if (!this._dep.envConfig.isProduction) {
                return res.sendSuccess(false);
            }

            const isMaintain = this._dep.envConfig.isServerWebMaintenance;
            return res.sendSuccess(isMaintain);

        } catch (e) {
            this._logger.error(e.message);
            return res.sendSuccess(true);
        }
    }

    private async tryCreateRsaPublicKey(walletAddress: string): Promise<string> {
        const rsaPublicKey = await this._loginService.tryCreateRsaPublicKey(walletAddress);
        return this._obfuscate.obfuscate(rsaPublicKey);
    }

    private isJwtPayloadWallet(payload: any): payload is JwtPayload {
        return (payload as JwtPayload).address !== undefined;
    }

    private isJwtPayloadAccount(payload: any): payload is JwtPayloadAccount {
        return (payload as JwtPayloadAccount).userName !== undefined;
    }

    /**
     * Client call directly, check server token
     */
    public async changeNickname(req: Request, res: Response) {
        try {
            const userName = req.body.userName;
            const newNickName = req.body.newNickName;

            if (!userName || !newNickName) {
                this._logger.error('Missing userName or newNickName');
                res.sendError('2@@Missing data');
                return;
            }

            const validHeader = await this._jwtService.verifyJwtHeader(req, userName);
            if (!validHeader) {
                this._logger.error('Invalid jwt header');
                res.sendError(K_WRONG_DATA);
                return;
            }

            const success = await this._profileService.changeNickname(userName, newNickName);
            if (!success) {
                res.sendError('Failed to change nickname', 500);
                return;
            }
            this._logger.info(`Change nickname success: ${userName}, newNickName: ${newNickName}`);
            res.sendSuccess(true);

        } catch (e) {
            this._logger.error((e as Error).stack);
            return res.sendError(e.message, 403);
        }
    }

    /**
     * For Unity Editor only
     */
    public async signEditorJwt(req: Request, res: Response) {
        try {
            const walletAddress = req.query.walletAddress as string;
            if (!walletAddress) {
                this._logger.error('Missing wallet address');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            if (!walletAddress.startsWith(EDITOR_USERNAME_PREFIX)) {
                // Chỉ cho phép những user name nào bắt đầu bằng "Editor" được sign jwt
                return res.sendGenericError();
            }

            const authInfo = await this._loginService.generateNeverExpireJwt(walletAddress);
            if (!authInfo) {
                this._logger.error(`${walletAddress} Cannot generate jwt`);
                return res.sendError(K_SERVER_ERR);
            }

            const rsaPublicKey = await this._loginService.tryCreateRsaPublicKey(walletAddress);
            const extraData = {
                version: this._dep.envConfig.versionWeb
            }
            const resData: JwtResponseData = {
                auth: authInfo.jwt,
                key: rsaPublicKey,
                extraData: JSON.stringify(extraData)
            }
            this._logger.info(`${walletAddress} signEditorJwt success`);
            return res.sendSuccess(resData);
        } catch (e) {
            this._logger.error((e as Error).message);
            return res.sendGenericError();
        }
    }

    private async checkAccountForEditor(userName: string, password: string, clientVersion: string, res: Response): Promise<JwtResponseData | null> {

        const account = await this._databaseAccess.checkAccountSenspark(userName, password);
        if (!account) {
            this._logger.error(`${userName}(${clientVersion}) not found in database`);
            return null;
        }

        const authInfo = await this._loginService.generateJwtWithRefreshToken(userName, 'account');
        if (!authInfo) {
            this._logger.error(`${userName}(${clientVersion}) cannot generate jwt`);
            return null;
        }
        const rsaPublicKey = await this._loginService.tryCreateRsaPublicKey(userName);
        const extraData = {
            version: this._dep.envConfig.versionWeb,
            isUserFi: account.isUserFi,
            address: account.address,
        }
        return {
            auth: authInfo.jwt,
            key: rsaPublicKey,
            extraData: JSON.stringify(extraData)
        };
    }

    public async getJwtForAccountFromEditor(req: Request, res: Response) {
        const userName = req.query.username as string;
        const password = req.query.password as string;
        if (userName && password) {
            const resData = await this.checkAccountForEditor(userName, password, '0', res);
            if (!resData) {
                return res.sendError(K_WRONG_DATA);
            }
            this._logger.info(`${userName} checkProof success`);
            return res.sendSuccess(resData);
        } else {
            this._logger.error('Missing wallet address or password');
            return res.sendError(K_MISSING_DATA_ERR);
        }
    }
}

function extractClientVersionFromHeader(req: Request) {
    const version = req.get(VERSION_HEADER);
    return 'v' + (version ? version : '0');
}

function runAfterDelay(ms: number, call: () => void) {
    (async () => {
        await sleep(ms);
        call();
    })();
}

export type AccType = 'wallet' | 'account'
