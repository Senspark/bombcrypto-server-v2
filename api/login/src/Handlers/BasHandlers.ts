import {IDependencies} from "../DependenciesInjector";
import {ILogger} from "../Services";
import {Request, Response} from "express";
import IObfuscate from "../services-impl/encrypt/IObfuscate";
import BotSecurityService from "../services-impl/encrypt/BotSecurityService";
import {JwtResponseData, RedisConfig, RedisKeys} from "../consts/Consts";
import ErrorTracking from "../services-impl/utils/ErrorTracking";
import {K_INVALID_DATA_ERR, K_MISSING_DATA_ERR, K_SERVER_ERR, K_WRONG_DATA} from "../consts/ResponseError";
import DatabaseAccess from "../services-impl/DatabaseAccess";
import {randomResponse} from "../services-impl/utils/RandomResponse";
import WebLoginService from "../services-impl/login/bsc/WebLoginService";
import BscWalletService from "../services-impl/auth/BscWalletService";

import IAutoExpireMap from "../services-impl/utils/IAutoExpireMap";
import RedisExpireMap from "../services-impl/utils/RedisExpireMap";
import {removeNameSuffix} from "../services-impl/utils/UserNameSuffix";
import ProfileService from "../services-impl/auth/ProfileService";

const EDITOR_USERNAME_PREFIX = "editor";
const VERSION_HEADER = 'x-bc-version';

function extractClientVersionFromHeader(req: Request): string | undefined {
    return req.header(VERSION_HEADER);
}

export class BasHandlers {
    constructor(
        private readonly _dep: IDependencies,
    ) {
        this._logger = _dep.logger.clone('[BAS]');

        // Create BscWalletService first
        const displayText = 'Your login code:';
        const nonces: IAutoExpireMap<string, [number, string]> = new RedisExpireMap(
            this._logger,
            this._dep.redis,
            RedisKeys.AP_BAS_LOGIN_NONCE,
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
            RedisKeys.AP_BAS_LOGIN_PRIVATE_KEY,
            RedisKeys.AP_BAS_LOGIN_JWT_REFRESH,
            wallet
        );

        const generalServices = _dep.generalServices;

        this._obfuscate = generalServices.obfuscate;
        this._botSecurity = generalServices.botSecurityService;
        this._databaseAccess = generalServices.databaseAccess;

        // Create services not available in GeneralServices
        const errorTracking = new ErrorTracking(this._logger);
        this._profileService = new ProfileService(this._dep);
    }

    private readonly _loginService: WebLoginService

    private readonly _logger: ILogger;
    private readonly _obfuscate: IObfuscate;
    private readonly _botSecurity: BotSecurityService;
    private readonly _databaseAccess: DatabaseAccess;
    private readonly _profileService: ProfileService;

    /**
     * Call from Client
     */
    public async generateNonce(req: Request, res: Response) {
        try {
            const walletAddress = req.body.walletAddress.toLowerCase();
            const clientVersion = extractClientVersionFromHeader(req);
            if (!walletAddress) {
                this._logger.error('Missing wallet address');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            // const hash = this._botSecurity.validateUserFirstRequest(walletAddress, req, this._logger);
            // if (!hash) {
            //     this._logger.error(`${walletAddress} Missing Client Token in generateNonce`);
            //     return await randomResponse(res);
            // }
            // this._botSecurity.assignServerTokenToCookie(hash, RedisConfig.K_SERVER_TOKEN_COOKIE_EXPIRED, res);

            const nonce = await this._loginService.generateNonceData(walletAddress);
            this._logger.info(`${walletAddress}(${clientVersion}) generateNonce success`);

            return res.sendSuccess({nonce: nonce});
        } catch (err) {
            this._logger.error((err as Error).stack);
            return res.sendError(K_SERVER_ERR);
        }
    }

    /**
     * Call from Client
     */
    public async checkProof(req: Request, res: Response) {
        try {
            const walletAddress: string = req.body.walletAddress.toLowerCase();
            const signature: string = req.body.signature;
            const clientVersion = extractClientVersionFromHeader(req);

            if (!walletAddress || !signature) {
                this._logger.error(`Missing data: ${walletAddress} ${signature}`);
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            // if (!this._botSecurity.validateUserSecondRequest(walletAddress, req)) {
            //     await randomResponse(res)
            // }

            const verified = await this._loginService.checkProof(walletAddress, signature);
            if (!verified) {
                res.sendError(K_INVALID_DATA_ERR);
                return;
            }

            const authInfo = await this._loginService.generateJwtWithRefreshToken(walletAddress, 'wallet');
            if (!authInfo) {
                this._logger.error(`${walletAddress}(${clientVersion}) cannot generate jwt`);
                res.sendError(K_SERVER_ERR);
                return;
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
            this._logger.info(`${walletAddress}(${clientVersion}) checkProof success`);
            res.sendSuccess(resData);
        } catch (err) {
            this._logger.error((err as Error).stack);
            res.sendError(K_SERVER_ERR);
        }
    }

    /**
     * Call from Server
     */
    public async verifyLoginData(req: Request, res: Response) {
        try {
            await this._dep.bearerService.verifyBearer(req);

            const walletAddressSuffix = req.body.walletAddress.toLowerCase();

            const walletAddress = removeNameSuffix(walletAddressSuffix);

            const loginData = req.body.loginData;

            if (!loginData) {
                const err = 'missing login data';
                this._logger.error(err);
                return res.sendError(err);
            }

            const userInfo = await this._loginService.verifyLoginData(walletAddress, loginData);

            // ví dùng để tạo trong database phải có network type ở cuối để tạo ra user mới
            const account = await this._databaseAccess.getOrCreateNewWalletBsc(walletAddress);

            if (!account) {
                this._logger.error(`account not found`);
                return res.sendError(K_SERVER_ERR);
            }
            this._logger.info(`success`);
            return res.sendSuccess({
                userId: account.uid,
                walletAddress: account.address, // account bas thì ko liên quan gì đến account fi bên bsc nên luôn trả về address ko phải userName
                createAt: account.createAt.getTime(), // epochMilis
                aesKey: userInfo.aesKey,
                extraData: userInfo.extraData
            });
        } catch (err) {
            this._logger.error((err as Error).stack);
            res.sendError(K_SERVER_ERR);
        }
    }

    /**
     * For Unity Editor only
     */
    public async signEditorJwt(req: Request, res: Response) {
        try {
            let address = req.query.walletAddress as string;
            if (!address) {
                this._logger.error('Missing wallet address');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            if (!address.startsWith(EDITOR_USERNAME_PREFIX)) {
                // Chỉ cho phép những user name nào bắt đầu bằng "Editor" được sign jwt
                return res.sendGenericError();
            }
            address = address.toLowerCase();

            const authInfo = await this._loginService.generateJwtWithRefreshToken(address, 'wallet');
            if (!authInfo) {
                this._logger.error(`${address} Cannot generate jwt`);
                return res.sendError(K_SERVER_ERR);
            }

            const rsaPublicKey = await this._loginService.tryCreateRsaPublicKey(address);
            const extraData = {
                version: this._dep.envConfig.versionWeb
            }
            const resData: JwtResponseData = {
                auth: authInfo.jwt,
                key: rsaPublicKey,
                extraData: JSON.stringify(extraData)
            }
            this._logger.info(`${address} signEditorJwt success`);
            return res.sendSuccess(resData);
        } catch (e) {
            this._logger.error((e as Error).message);
            return res.sendGenericError();
        }
    }

    /**
     * Refresh token call from Client
     */
    public async refreshJwtToken(req: Request, res: Response) {
        try {

            // if (!this._botSecurity.validateUserSecondRequest('<null>', req)) {
            //     this._logger.error('missing Server Token');
            //     return randomResponse(res);
            // }
            const refreshToken: string = req.params['rf'];
            const clientVersion = extractClientVersionFromHeader(req);
            if (!refreshToken) {
                this._logger.error('missing refresh token');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            this._logger.info(`rt=${refreshToken} (${clientVersion})`);

            const lowerAddress = await this._loginService.convertRefreshJwtToAddress(refreshToken);

            if (!lowerAddress) {
                this._logger.error(`missing data`);
                return res.sendError(K_INVALID_DATA_ERR);
            }
            // const lowerAddress = (JSON.parse(jwtAddressToken) as [string, string])[0].toLowerCase(); // FIXME: bad logic
            const newAuthToken = await this._loginService.refreshJwt(refreshToken, lowerAddress);
            if (!newAuthToken) {
                this._logger.error(`cannot refresh jwt (error)`);
                return res.sendError(K_INVALID_DATA_ERR);
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

            this._logger.info(`refreshJwtToken success ${lowerAddress}`);
            return res.sendSuccess(resData);

        } catch (err) {
            this._logger.error((err as Error).stack);
            res.sendError(K_SERVER_ERR);
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

            const validHeader = await this._dep.generalServices.jwtService.verifyJwtHeader(req, userName);
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

    private async tryCreateRsaPublicKey(walletAddress: string): Promise<string> {
        const rsaPublicKey = await this._loginService.tryCreateRsaPublicKey(walletAddress);
        return this._obfuscate.obfuscate(rsaPublicKey);
    }

    /**
     * Check server maintenance
     */
    public async checkServerMaintain(_req: Request, res: Response) {
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
}
