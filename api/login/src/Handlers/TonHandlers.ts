import {IDependencies} from "../DependenciesInjector";
import {ILogger} from "../Services";
import {Request, Response} from 'express';
import {TonProofService} from "../services-impl/login/ton/TonProofService";
import {CheckProofRequest} from "../dto/CheckProofRequest.dto";
import {TonApiService} from "../services-impl/login/ton/TonApiService";
import TonLoginService, {IJwtTokenPayload} from "../services-impl/login/ton/TonLoginService";
import JwtService from "../services-impl/JwtService";
import {CHAIN} from "../dto/CHAIN";
import IObfuscate from "../services-impl/encrypt/IObfuscate";
import {RedisConfig} from "../consts/Consts";
import BotSecurityService from "../services-impl/encrypt/BotSecurityService";
import {K_INVALID_DATA_ERR, K_MISSING_DATA_ERR, K_SERVER_ERR} from "../consts/ResponseError";
import {JwtResponseData} from "./SolHandlers";
import DatabaseAccess from "../services-impl/DatabaseAccess";
import IpWhitelist from "../services-impl/encrypt/IpWhitelist";
import {randomResponse} from "../services-impl/utils/RandomResponse";

const EDITOR_USERNAME_PREFIX = "Editor";
const TAG = '[Ton Handlers]';

export default class TonHandlers {
    constructor(private readonly _dep: IDependencies) {
        this.#logger = _dep.logger.clone('[TON]');

        const generalServices = _dep.generalServices;

        // Use services from GeneralServices
        this._jwtLoginService = generalServices.jwtService;
        this._obfuscate = generalServices.obfuscate;
        this._botSecurity = generalServices.botSecurityService;
        this._databaseAccess = generalServices.databaseAccess;

        // Initialize Ton specific services
        this.#tonProof = new TonProofService(_dep, this._jwtLoginService);
        this.#tonLogin = new TonLoginService(_dep, this._jwtLoginService);
    }

    readonly #logger: ILogger;
    readonly #tonProof: TonProofService;
    readonly #tonLogin: TonLoginService;
    private readonly _obfuscate: IObfuscate;
    private readonly _botSecurity: BotSecurityService;
    private readonly _jwtLoginService: JwtService;
    private readonly _databaseAccess: DatabaseAccess;
    readonly #tonApiServices: Map<string, TonApiService> = new Map();

    public async generateNonce(req: Request, res: Response) {
        try {
            const walletAddress = req.body.address ?? "null";
            const hash = this._botSecurity.validateUserFirstRequest(walletAddress, req);
            if (!hash) {
                this.#logger.error(`${TAG} invalid hash: ${walletAddress}`);
                return; // not response
            }

            this._botSecurity.assignServerTokenToCookie(hash, RedisConfig.K_SERVER_TOKEN_COOKIE_EXPIRED, res);

            const payloadToken = await this.#tonProof.generatePayloadToken();
            this.#logger.info(`${TAG} Generate payload success`);
            res.sendSuccess({payload: payloadToken});
        } catch (e) {
            this.#logger.error(e.message);
            res.sendGenericError();
        }
    }

    public async notSupported(req: Request, res: Response) {
        this.#logger.info(`${TAG} Old client call not supported api`);
        res.sendError('Get the latest version and try again');
    }

    public async checkProof(req: Request, res: Response) {
        try {
            const body = CheckProofRequest.parse(await req.body);
            const client = this.getTonApiService(body.network);

            const walletAddress = req.body.address;
            if (!walletAddress) {
                this.#logger.error('Missing wallet address');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            if (this._dep.envConfig.isProduction && body.network !== CHAIN.MAINNET) {
                return res.sendError('Invalid network chain');
            }

            const isValid = await this.#tonProof.checkProof(body, (address) => client.getWalletPublicKey(address));
            if (!isValid) {
                return res.sendError('Invalid proof');
            }

            if (!await this.#tonProof.verifyPayloadToken(body.proof.payload)) {
                return res.sendError('Invalid payload token');
            }

            const returnData = await this.#tonLogin.createLoginToken(body);
            if (!returnData) {
                this.#logger.error(`Cannot generate jwt`);
                res.sendError(K_SERVER_ERR);
                return;
            }
            this._botSecurity.assignRefreshTokenToCookie(returnData.refreshToken, RedisConfig.K_REFRESH_TOKEN_COOKIE_EXPIRED, res);
            const rsaPublicKey = await this.tryCreateRsaPublicKey(returnData.address);
            const resData: JwtResponseData = {
                auth: returnData.token,
                key: rsaPublicKey,
                version: "0" //No effect for ton
            }
            this.#logger.info(`${returnData.address} CheckProof success`);
            return res.sendSuccess(resData);

        } catch (e) {
            this.#logger.error(e.message);
            res.sendGenericError();
        }
    }

    private async tryCreateRsaPublicKey(walletAddress: string): Promise<string> {
        const rsaPublicKey = await this.#tonLogin.tryCreateRsaPublicKey(walletAddress);
        return this._obfuscate.obfuscate(rsaPublicKey);
    }

    /**
     * For Unity Editor only
     */
    public async signEditorJwt(req: Request, res: Response) {
        try {
            const walletAddressEditor = req.query.walletAddress as string;
            if (!walletAddressEditor) {
                this.#logger.error('Missing wallet address');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            if (!walletAddressEditor.startsWith(EDITOR_USERNAME_PREFIX)) {
                // Chỉ cho phép những user name nào bắt đầu bằng "Editor" được sign jwt
                res.sendGenericError();
                return;
            }
            const walletAddress = walletAddressEditor.replace(EDITOR_USERNAME_PREFIX, '');
            const jwt = await this._jwtLoginService.createAuthToken({
                address: walletAddress,
                network: CHAIN.TESTNET
            } as IJwtTokenPayload);

            if (!jwt) {
                this.#logger.error(`Editor ${walletAddress} Cannot generate jwt`);
                res.sendError(K_SERVER_ERR);
                return;
            }

            const rsaPublicKey = await this.#tonLogin.tryCreateRsaPublicKey(walletAddress);
            const resData: JwtResponseData = {
                auth: jwt,
                key: rsaPublicKey,
                version: "0" // no effect for Ton
            }
            this.#logger.info(`Ton: ${walletAddress} signEditorJwt success`);
            res.sendSuccess(resData);
        } catch (e) {
            this.#logger.error((e as Error).stack);
            res.sendGenericError();
        }
    }

    public async verifyLoginToken(req: Request, res: Response) {
        try {
            await this._dep.bearerService.verifyBearer(req);
            const walletAddress = req.body.walletAddress;
            const loginData = req.body.loginData;

            if (!walletAddress || !loginData) {
                const err = 'Missing wallet address or login data';
                this.#logger.error(err);
                res.sendError(err);
                return;
            }
            const userInfo = await this.#tonLogin.verifyTelegramUserInfo(walletAddress, loginData);
            if (!userInfo) {
                const err = 'Invalid login data';
                this.#logger.error(err);
                res.sendError(err);
                return;
            }
            const user = await this._databaseAccess.getOrCreateNewWalletTon(walletAddress, userInfo?.telegramUserId, userInfo?.telegramUserName);
            res.sendSuccess({
                userId: user.uid,
                walletAddress: walletAddress,
                telegramUserName: userInfo?.telegramUserName ?? userInfo.friendlyAddress,
                createAt: user.createAt.getTime(), // epochMilis
                aesKey: userInfo.aesKey,
            });
        } catch (e) {
            this.#logger.error(e.message);
            res.sendError(e.message);
        }
    }

    public async refreshJwtToken(req: Request, res: Response) {
        try {
            if (!this._botSecurity.validateUserSecondRequest('<null>', req)) {
                return randomResponse(res);
            }

            const network = req.body.network;
            const requestAddress = req.body.address;

            if (!requestAddress || !network) {
                this.#logger.error('Missing address or network');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            if (this._dep.envConfig.isProduction && network !== CHAIN.MAINNET) {
                return res.sendError('Invalid network chain');
            }

            const refreshToken = this._botSecurity.getRefreshToken(req);
            if (!refreshToken) {
                this.#logger.error('Missing refresh token');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            const saveAddress = await this.#tonLogin.refreshJwtToAddress(refreshToken);
            if (!saveAddress) {
                this.#logger.error('Cannot find save address');
                res.sendError(K_INVALID_DATA_ERR);
                return;
            }

            if (saveAddress !== requestAddress) {
                this.#logger.error(`Wallet address not match: ${saveAddress} <> ${requestAddress}`);
                res.sendError(K_INVALID_DATA_ERR);
                return;
            }

            const jwtData = await this.#tonLogin.refreshJwt(refreshToken, requestAddress, network);
            if (!jwtData) {
                this.#logger.error('Cannot refresh jwt');
                res.sendError(K_SERVER_ERR);
                return;
            }
            const rsaPublicKey = await this.tryCreateRsaPublicKey(jwtData.walletAddress);
            const resData: JwtResponseData = {
                auth: jwtData.jwt,
                key: rsaPublicKey,
                version: "0" // No effect for Ton
            }
            this.#logger.info(`${jwtData.walletAddress} refreshJwtToken success`);
            res.sendSuccess(resData);
        } catch (e) {
            this.#logger.error((e as Error).stack);
            res.sendGenericError();
        }
    }

    public async checkServerMaintain(req: Request, res: Response) {
        try {
            const envConfig = this._dep.envConfig;
            if (!envConfig.isProduction) {
                return res.sendSuccess(false);
            }

            const isMaintain = envConfig.isServerTonMaintenance;
            return res.sendSuccess(isMaintain);

        } catch (e) {
            this.#logger.error(e.message);
            return res.sendSuccess(true);
        }
    }

    private getTonApiService(network: CHAIN): TonApiService {
        if (!this.#tonApiServices.has(network)) {
            this.#tonApiServices.set(network, TonApiService.create(network));
        }
        return this.#tonApiServices.get(network)!;
    }

    private getIpAddress(req: Request) {
        const forwardedIp = req.headers['x-forwarded-for'];
        let ip = req.ip;
        if (forwardedIp) {
            ip = forwardedIp.toString().split(',')[0];
        }
        return ip;
    }

}