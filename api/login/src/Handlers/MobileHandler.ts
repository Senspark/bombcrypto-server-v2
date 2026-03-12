import {IDependencies} from "../DependenciesInjector";
import JwtService from "../services-impl/JwtService";
import {ILogger} from "../Services";
import {Request, Response} from "express";
import IObfuscate from "../services-impl/encrypt/IObfuscate";
import {JwtResponseData, RedisKeys} from "../consts/Consts";
import ErrorTracking from "../services-impl/utils/ErrorTracking";
import UserBanManager from "../services-impl/UserBanManager";
import WebLoginService from "../services-impl/login/bsc/WebLoginService";
import DatabaseAccess, {UserAccount} from "../services-impl/DatabaseAccess";
import MobileLoginService from "../services-impl/login/bsc/MobieLoginService";
import {
    K_INVALID_DATA_ERR,
    K_MISSING_DATA_ERR,
    K_SECRET_ERR,
    K_SERVER_ERR,
    K_WRONG_DATA
} from "../consts/ResponseError";
import RegisterService from "../services-impl/auth/RegisterService";
import ProfileService from "../services-impl/auth/ProfileService";
import {v7 as uuid} from "uuid";
import BscWalletService from "../services-impl/auth/BscWalletService";

import IAutoExpireMap from "../services-impl/utils/IAutoExpireMap";
import RedisExpireMap from "../services-impl/utils/RedisExpireMap";

const VERSION_HEADER = 'x-bc-version';
const GUEST = 'GUEST';

export class MobileHandlers {
    constructor(private readonly _dep: IDependencies) {
        this._logger = _dep.logger.clone('[MOBILE]');

        const generalServices = _dep.generalServices;

        this._jwtService = generalServices.jwtService;
        this._obfuscate = generalServices.obfuscate;
        this._databaseAccess = generalServices.databaseAccess;


        // Create services not available in GeneralServices
        const errorTracking = new ErrorTracking(this._logger);
        this._mobileLoginService = new MobileLoginService(this._dep,
            RedisKeys.AP_WEB_LOGIN_PRIVATE_KEY,
            RedisKeys.AP_WEB_LOGIN_JWT_REFRESH);

        // Create BscWalletService first
        const displayText = 'Your login code:';
        const nonces: IAutoExpireMap<string, [number, string]> = new RedisExpireMap(
            this._logger,
            this._dep.redis,
            RedisKeys.AP_WEB_LOGIN_NONCE,
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
        this._webLoginService = new WebLoginService(
            this._dep,
            RedisKeys.AP_WEB_LOGIN_PRIVATE_KEY,
            RedisKeys.AP_WEB_LOGIN_JWT_REFRESH,
            wallet
        );
        this._userBanManager = new UserBanManager(this._logger, _dep.envConfig.syncBannedList, errorTracking, _dep.redis, _dep.scheduler);

        this._registerService = new RegisterService(this._dep);
        this._profileService = new ProfileService(this._dep);

    }

    private readonly _logger: ILogger;
    private readonly _webLoginService: WebLoginService;
    private readonly _mobileLoginService: MobileLoginService;
    private readonly _obfuscate: IObfuscate;
    private readonly _userBanManager: UserBanManager;
    private readonly _databaseAccess: DatabaseAccess;

    private readonly _registerService: RegisterService;
    private readonly _profileService: ProfileService;
    private readonly _jwtService: JwtService;

    /**
     * Call from Client
     */
    public async checkProof(req: Request, res: Response) {
        try {
            const userName = req.body.userName;
            const password = req.body.password;
            const clientVersion = extractClientVersionFromHeader(req);

            if (!userName || !password) {
                this._logger.error('Missing wallet userName or password');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            //Check account
            const account = await this._mobileLoginService.queryDatabaseAccount(userName, password);
            if (!account) {
                this._logger.error(`${userName} Cannot find account in check proof`);
                res.sendError(K_INVALID_DATA_ERR);
                return;
            }

            const authInfo = await this._mobileLoginService.generateJwt(userName);
            if (!authInfo) {
                this._logger.error(`${userName} cannot generate jwt`);
                res.sendError(K_INVALID_DATA_ERR);
                return;
            }

            const rsaPublicKey = await this.tryCreateRsaPublicKey(userName);
            const extraData = {
                uid: account.uid,
                isUserFi: account.isUserFi,
                address: account.address,
            }
            const resData = {
                auth: authInfo.jwt,
                key: rsaPublicKey,
                extraData: JSON.stringify(extraData)
            };

            if (!resData) {
                res.sendError(K_WRONG_DATA);
                return;
            }

            this._logger.info(`${userName} checkProof success`);
            res.sendSuccess(resData);
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendGenericError();
        }
    }


    /**
     * Call from Client (use for username and password
     */
    public async checkProofForGuest(req: Request, res: Response) {
        try {
            const userName = req.body.userName;

            if (!userName) {
                this._logger.error('Missing id or userName');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }


            //Check account
            const uid = await this._mobileLoginService.checkGuestAccount(userName);
            if (!uid) {
                this._logger.error(`${userName} Cannot find guest account`);
                return;
            }
            const account: UserAccount = {
                uid: uid,
                userName: userName,
                nickName: null,
                email: null,
                typeAccount: GUEST,
                createAt: new Date(),
                address: "",
                isUserFi: false
            }

            const authInfo = await this._mobileLoginService.generateJwt(userName);
            if (!authInfo) {
                this._logger.error(`${userName} cannot generate jwt`);
                return;
            }
            const rsaPublicKey = await this.tryCreateRsaPublicKey(userName);
            const extraData = {
                version: this._dep.envConfig.versionWeb,
                isUserFi: account.isUserFi,
                address: userName,
            }
            const resData = {
                auth: authInfo.jwt,
                key: rsaPublicKey,
                extraData: JSON.stringify(extraData)
            };

            if (!resData) {
                res.sendError(K_WRONG_DATA);
                return;
            }

            this._logger.info(`${userName} checkProof success`);
            res.sendSuccess(resData);
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendGenericError();
        }
    }

    /**
     * Call from Client
     */
    public async refreshJwtToken(req: Request, res: Response) {
        try {
            const userName = req.body.userName;
            if (!userName) {
                this._logger.error('Missing user name');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            const isValidHeader = await this._jwtService.verifyJwtHeader(req, userName);
            if (!isValidHeader) {
                this._logger.error('Invalid jwt header');
                res.sendError(K_SECRET_ERR);
                return;
            }
            const newAuthToken = await this._mobileLoginService.refreshJwt(userName);

            if (!newAuthToken) {
                this._logger.error(`${userName} Cannot refresh jwt (error)`);
                res.sendError(K_INVALID_DATA_ERR);
                return;
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

            this._logger.info(`${newAuthToken.walletAddress} refreshJwtToken success`);
            res.sendSuccess(resData);
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendGenericError();
        }
    }

    /**
     * Call from Server
     */
    public async verifyLoginData(req: Request, res: Response) {
        try {
            await this._dep.bearerService.verifyBearer(req);

            const userName = req.body.userName;
            const loginData = req.body.loginData;

            if (!loginData) {
                const err = 'Missing login data';
                this._logger.error(err);
                res.sendError(err);
                return;
            }

            if (this._userBanManager.isBanned(userName)) {
                this._logger.info(`${userName} is banned (verifyLoginData)`);
                res.sendError(K_SECRET_ERR);
                return;
            }

            const userInfo = await this._mobileLoginService.verifyLoginData(userName, loginData);
            let account: UserAccount | null;

            account = await this._databaseAccess.getInfoAccountBsc(userName)
            if (!account) {
                this._logger.error(`${userName} Cannot find account in verify login data`);
                res.sendError(K_SERVER_ERR);
                return;
            }

            this._logger.info(`${userName} verifyLoginData success`);
            res.sendSuccess({
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
            this._logger.error((e as Error).stack);
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
            res.sendSuccess(true);
        }
    }


    private async tryCreateRsaPublicKey(walletAddress: string): Promise<string> {
        const rsaPublicKey = await this._webLoginService.tryCreateRsaPublicKey(walletAddress);
        return this._obfuscate.obfuscate(rsaPublicKey);
    }

    /**
     * Handler for creating a Senspark account
     */
    public async createSensparkAccount(req: Request, res: Response) {
        try {
            const userName = req.body.username;
            const password = req.body.password;
            const email = req.body.email;

            if (!userName || !password || !email) {
                this._logger.error('Missing userName or password or email');
                res.sendError('2@@Missing data');
                return;
            }

            const uid = await this._registerService.createSensparkAccount(userName, password, email);
            if (!uid) {
                res.sendError('Failed to create account senspark', 500);
                return;
            }
            this._logger.info(`Create account senspark success: ${userName}`);
            res.sendSuccess(uid);
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
    }

    /**
     * Handler for creating a Guest account
     */
    public async createGuestAccount(req: Request, res: Response) {
        try {
            const userName = uuid();
            this._logger.info(`Create guest account: ${userName}`);

            const userId = await this._registerService.createGuestAccount(userName.toString());
            if (!userId) {
                res.sendError('Failed to create guest account', 500);
                return;
            }

            this._logger.info(`Create guest account success: ${userName}, ID: ${userId}`);
            res.sendSuccess({userName, userId});
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
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
                this._logger.error(`Failed to change nickname: ${userName}, newNickName: ${newNickName}`);
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
}

function extractClientVersionFromHeader(req: Request) {
    const version = req.get(VERSION_HEADER);
    return 'v' + (version ? version : '0');
}