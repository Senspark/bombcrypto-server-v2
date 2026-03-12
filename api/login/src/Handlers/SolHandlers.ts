import {IDependencies} from "../DependenciesInjector";
import JwtService from "../services-impl/JwtService";
import {ILogger} from "../Services";
import {Request, Response} from "express";
import SolLoginService from "../services-impl/login/sol/SolLoginService";
import IObfuscate from "../services-impl/encrypt/IObfuscate";
import BotSecurityService from "../services-impl/encrypt/BotSecurityService";
import {RedisConfig} from "../consts/Consts";
import ErrorTracking from "../services-impl/utils/ErrorTracking";
import UserBanManager from "../services-impl/UserBanManager";
import {K_INVALID_DATA_ERR, K_MISSING_DATA_ERR, K_SECRET_ERR, K_SERVER_ERR} from "../consts/ResponseError";
import DatabaseAccess from "../services-impl/DatabaseAccess";

const EDITOR_USERNAME_PREFIX = "Editor";
const VERSION_HEADER = 'x-bc-version';

export class SolHandlers {
    constructor(private readonly _dep: IDependencies) {
        this._logger = _dep.logger.clone('[SOL]');

        const generalServices = _dep.generalServices;

        this._jwtLoginService = generalServices.jwtService;
        this._obfuscate = generalServices.obfuscate;
        this._botSecurity = generalServices.botSecurityService;
        this._databaseAccess = generalServices.databaseAccess;

        // Create services not available in GeneralServices
        const errorTracking = new ErrorTracking(this._logger);
        this._loginService = new SolLoginService(this._logger, this._dep.redis, generalServices.databaseAccess, this._jwtLoginService, errorTracking, _dep.envConfig.aesSecret, _dep.envConfig.gameSignPadding, _dep.envConfig.rsaDelimiter);
        this._userBanManager = new UserBanManager(this._logger, _dep.envConfig.syncBannedList, errorTracking, _dep.redis, _dep.scheduler);
    }

    private readonly _logger: ILogger;
    private readonly _jwtLoginService: JwtService;
    private readonly _loginService: SolLoginService;
    private readonly _obfuscate: IObfuscate;
    private readonly _botSecurity: BotSecurityService;
    private readonly _userBanManager: UserBanManager;
    private readonly _databaseAccess: DatabaseAccess;

    /**
     * Call from Client
     */
    public async generateNonce(req: Request, res: Response) {
        try {
            const walletAddress = req.body.walletAddress;
            const clientVersion = extractClientVersionFromHeader(req);
            if (!walletAddress) {
                this._logger.error('Missing wallet address');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            const hash = this._botSecurity.validateUserFirstRequest(walletAddress, req);
            if (!hash) {
                return; // not response
            }

            if (this._userBanManager.isBanned(walletAddress)) {
                this._logger.info(`${walletAddress}(${clientVersion}) is banned (generateNonce)`);
                res.sendError(K_SECRET_ERR);
                return;
            }

            this._botSecurity.assignServerTokenToCookie(hash, RedisConfig.K_SERVER_TOKEN_COOKIE_EXPIRED, res);

            const nonce = await this._loginService.generateNonceData(walletAddress);
            this._logger.info(`${walletAddress}(${clientVersion}) generateNonce success`);
            res.sendSuccess({nonce: nonce});
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendGenericError();
        }
    }

    /**
     * Call from Client
     */
    public async checkProof(req: Request, res: Response) {
        try {
            const walletAddress = req.body.walletAddress;
            const signature = req.body.signature;
            const clientVersion = extractClientVersionFromHeader(req);

            if (!walletAddress || !signature) {
                this._logger.error('Missing wallet address or signature');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            if (!this._botSecurity.validateUserSecondRequest(walletAddress, req)) {
                return; // not response
            }

            if (this._userBanManager.isBanned(walletAddress)) {
                this._logger.info(`${walletAddress}(${clientVersion}) is banned (checkProof)`);
                res.sendError(K_SECRET_ERR);
                return;
            }

            // TODO: uncomment để dễ test
            const verified = await this._loginService.checkProof(walletAddress, signature);
            if (!verified) {
                res.sendError(K_INVALID_DATA_ERR);
                return;
            }

            const authInfo = await this._loginService.generateJwtWithRefreshToken(walletAddress);
            if (!authInfo) {
                this._logger.error(`${walletAddress}(${clientVersion}) cannot generate jwt`);
                res.sendError(K_SERVER_ERR);
                return;
            }
            this._botSecurity.assignRefreshTokenToCookie(authInfo.refreshToken, RedisConfig.K_REFRESH_TOKEN_COOKIE_EXPIRED, res);
            const rsaPublicKey = await this.tryCreateRsaPublicKey(walletAddress);
            const resData: JwtResponseData = {
                auth: authInfo.jwt,
                key: rsaPublicKey,
                version: this._dep.envConfig.versionSol
            }
            this._logger.info(`${walletAddress}(${clientVersion}) checkProof success`);
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
            if (!this._botSecurity.validateUserSecondRequest('<null>', req)) {
                return; // not response
            }

            const refreshToken = this._botSecurity.getRefreshToken(req);
            const clientVersion = extractClientVersionFromHeader(req);
            if (!refreshToken) {
                this._logger.error('Missing refresh token');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            const walletAddress = await this._loginService.refreshJwtToAddress(refreshToken);
            if (!walletAddress) {
                this._logger.error('Cannot refresh jwt (missing wallet address)');
                res.sendError(K_INVALID_DATA_ERR);
                return;
            }

            if (this._userBanManager.isBanned(walletAddress)) {
                this._logger.info(`${walletAddress}(${clientVersion}) is banned (checkProof)`);
                res.sendError(K_SECRET_ERR);
                return;
            }

            const newAuthToken = await this._loginService.refreshJwt(refreshToken, walletAddress);
            if (!newAuthToken) {
                this._logger.error(`${walletAddress}(${clientVersion}) Cannot refresh jwt (error)`);
                res.sendError(K_INVALID_DATA_ERR);
                return;
            }

            if (this._userBanManager.isBanned(newAuthToken.walletAddress)) {
                this._logger.info(`${newAuthToken.walletAddress}(${clientVersion}) is banned (refreshJwtToken)`);
                res.sendError(K_SECRET_ERR);
                return;
            }

            const rsaPublicKey = await this.tryCreateRsaPublicKey(newAuthToken.walletAddress);
            const resData: JwtResponseData = {
                auth: newAuthToken.jwt,
                key: rsaPublicKey,
                version: this._dep.envConfig.versionSol
            };

            this._logger.info(`${newAuthToken.walletAddress}(${clientVersion}) refreshJwtToken success`);
            res.sendSuccess(resData);
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendGenericError();
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
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            if (!walletAddress.startsWith(EDITOR_USERNAME_PREFIX)) {
                // Chỉ cho phép những user name nào bắt đầu bằng "Editor" được sign jwt
                res.sendGenericError();
                return;
            }

            const authInfo = await this._loginService.generateNeverExpireJwt(walletAddress);
            if (!authInfo) {
                this._logger.error(`${walletAddress} Cannot generate jwt`);
                res.sendError(K_SERVER_ERR);
                return;
            }

            const rsaPublicKey = await this._loginService.tryCreateRsaPublicKey(walletAddress);
            const resData: JwtResponseData = {
                auth: authInfo.jwt,
                key: rsaPublicKey,
                version: this._dep.envConfig.versionSol
            }
            this._logger.info(`${walletAddress} signEditorJwt success`);
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

            const walletAddress = req.body.walletAddress;
            const loginData = req.body.loginData;

            if (!walletAddress || !loginData) {
                const err = 'Missing wallet address or login data';
                this._logger.error(err);
                res.sendError(err);
                return;
            }

            if (this._userBanManager.isBanned(walletAddress)) {
                this._logger.info(`${walletAddress} is banned (verifyLoginData)`);
                res.sendError(K_SECRET_ERR);
                return;
            }

            const userInfo = await this._loginService.verifyLoginData(walletAddress, loginData);
            const account = await this._databaseAccess.getOrCreateNewWalletSol(walletAddress);

            if (!account) {
                this._logger.error(`${walletAddress} Cannot find account`);
                res.sendError(K_SERVER_ERR);
                return;
            }

            this._logger.info(`${walletAddress} verifyLoginData success`);
            res.sendSuccess({
                userId: account.uid,
                walletAddress: account.userName,
                createAt: account.createAt.getTime(), // epochMilis
                aesKey: userInfo.aesKey,
                extraData: userInfo.extraData
            });
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
    }

    public async getBannedList(req: Request, res: Response) {
        try {
            res.sendRawJson(this._userBanManager.exportBannedList());
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
    }

    public async checkServerMaintain(req: Request, res: Response) {
        try {
            this._logger.info(`Check server maintain`);
            //Server test ko cần check
            if (!this._dep.envConfig.isProduction) {
                return res.sendSuccess(false);
            }

            const isMaintain = this._dep.envConfig.isServerSolMaintenance;
            return res.sendSuccess(isMaintain);

        } catch (e) {
            this._logger.error(e.message);
            res.sendSuccess(true);
        }
    }

    private async tryCreateRsaPublicKey(walletAddress: string): Promise<string> {
        const rsaPublicKey = await this._loginService.tryCreateRsaPublicKey(walletAddress);
        return this._obfuscate.obfuscate(rsaPublicKey);
    }
}

export type JwtResponseData = {
    auth: string; // jwt
    key: string; // rsa public key
    version: string
}

export function extractClientVersionFromHeader(req: Request) {
    const version = req.get(VERSION_HEADER);
    return 'v' + (version ? version : '0');
}