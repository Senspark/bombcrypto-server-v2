import {IDependencies} from "../DependenciesInjector";
import JwtService from "../services-impl/JwtService";
import {ILogger} from "../Services";
import {Request, Response} from "express";
import BotSecurityService from "../services-impl/encrypt/BotSecurityService";
import {RedisConfig} from "../consts/Consts";
import {K_MISSING_DATA_ERR, K_SERVER_ERR, K_WRONG_DATA} from "../consts/ResponseError";
import DatabaseAccess from "../services-impl/DatabaseAccess";
import DappLoginService from "../services-impl/login/dapp/DappLoginService";
import {randomResponse} from "../services-impl/utils/RandomResponse";
import RegisterService from "../services-impl/auth/RegisterService";
import PasswordService from "../services-impl/auth/PasswordService";
import ProfileService from "../services-impl/auth/ProfileService";
import {ethers} from "ethers";

export class DappHandler {
    constructor(private readonly _dep: IDependencies) {
        this._logger = _dep.logger.clone('[DAPP]');

        const generalServices = _dep.generalServices;
        this._isProd = _dep.envConfig.isProduction;

        this._jwtService = generalServices.jwtService;
        this._botSecurity = generalServices.botSecurityService;
        this._databaseAccess = generalServices.databaseAccess;

        // Initialize DappLoginService
        this._dappLoginService = new DappLoginService(
            this._logger,
            this._jwtService,
            this._databaseAccess,
            this._isProd,
            this._dep.redis,
            this._dep.envConfig.aesSecret,
            this._dep.envConfig.dappSignPadding,
        );
        
        // Initialize services from AuthHandler
        this._registerService = new RegisterService(this._dep);
        this._passwordService = new PasswordService(this._dep);
        this._profileService = new ProfileService(this._dep);
    }

    private readonly _logger: ILogger;
    private readonly _jwtService: JwtService;
    private readonly _databaseAccess: DatabaseAccess;
    private readonly _isProd: boolean;
    private readonly _botSecurity: BotSecurityService;
    private readonly _dappLoginService: DappLoginService;
    
    // Added fields from AuthHandler
    private readonly _registerService: RegisterService;
    private readonly _passwordService: PasswordService;
    private readonly _profileService: ProfileService;

    /**
     * Verify user account with username and password and save jwt for next time
     * Use for the first time login or jwt expired
     */
    public async verifyAccount(req: Request, res: Response) {
        try {
            const userName = req.body.username;
            const password = req.body.password;

            // Handle request validation in the handler
            if (!userName || !password) {
                this._logger.error('Missing userName or password');
                return res.sendError(K_MISSING_DATA_ERR);
            }
            
            // Login first time needs to check if client dapp has client token
            // for subsequent logins (using JWT) this client token is required for validation
            const hash = this._botSecurity.validateDappFirstRequest(userName, req, this._logger);
            if (!hash) {
                this._logger.error(`${userName} Missing Client Token in verifyAccount`);
                return await randomResponse(res);
            }

            // Call service to verify account - business logic is in service
            const result = await this._dappLoginService.verifyAccount(userName, password);

            if (!result.success) {
                return res.sendError(result.error!);
            }

            // Assign API token to cookies for subsequent JWT logins
            this._botSecurity.assignDappApiTokenToCookie(hash, RedisConfig.K_SERVER_TOKEN_COOKIE_EXPIRED, res);

            const resData = {
                jwt: result.jwt,
                type: "account",
            };

            this._logger.info(`${userName} Verification successful`);
            return res.sendSuccess(resData);
        } catch (e) {
            this._logger.error((e as Error).message);
            return res.sendGenericError();
        }
    }

    /**
     * Verify jwt senspark account or wallet account
     * after the first time login use VerifyAccount, use this all the time to login until jwt expired
     */
    public async checkJwt(req: Request, res: Response) {
        try {
            const userName = req.query.username as string;
            
            // Handle request validation in the handler
            if (!userName) {
                this._logger.error('Missing userName');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            if (!this._botSecurity.validateDappSecondRequest(userName, req, this._logger)) {
                this._logger.error(`missing Api Token in verifyJwt`);
                return await randomResponse(res);
            }

            // Get JWT from header and verify - business logic in service
            const jwtDapp = await this._jwtService.getJwtFromHeader(req);
            const result = await this._dappLoginService.verifyJwt(jwtDapp);

            if (!result.success) {
                return res.sendError(result.error!);
            }

            this._logger.info(`${userName} login success`);
            return res.sendSuccess(result.data);
        } catch (e) {
            // Need login again
            this._logger.error((e as Error).message);
            return res.sendGenericError();
        }
    }

    /**
     * Generate nonce for wallet login
     */
    public async getNonce(req: Request, res: Response) {
        try {
            const walletAddress = req.body.address;
            
            // Handle request validation in the handler
            if (!walletAddress) {
                this._logger.error('Missing wallet address');
                return res.sendError(K_MISSING_DATA_ERR);
            }
            
            const address = walletAddress.toLowerCase();
            
            const hash = this._botSecurity.validateDappFirstRequest(address, req, this._logger);
            if (!hash) {
                this._logger.error(`${address} Missing Client Token in getNonce`);
                return await randomResponse(res);
            }
            
            // Generate nonce for the wallet address - business logic in service
            const nonce = await this._dappLoginService.generateNonceData(address);

            this._logger.info(`${address} get nonce success`);
            return res.sendSuccess({
                nonce: nonce
            });
        } catch (e) {
            this._logger.error((e as Error).message);
            return res.sendGenericError();
        }
    }

    /**
     * Verify wallet signature and create JWT
     */
    public async verifyWallet(req: Request, res: Response) {
        try {
            const walletAddress = req.body.address;
            const signatureBase64 = req.body.signature;

            // Handle request validation in the handler
            if (!walletAddress || !signatureBase64) {
                this._logger.error('Missing wallet address or signature');
                return res.sendError(K_MISSING_DATA_ERR);
            }

            const address = walletAddress.toLowerCase();

            const hash = this._botSecurity.validateDappFirstRequest(address, req, this._logger);
            if (!hash) {
                this._logger.error(`${address} Missing Client Token in verifyWallet`);
                return await randomResponse(res);
            }
            
            // Assign API token to cookies for subsequent JWT logins
            this._botSecurity.assignDappApiTokenToCookie(hash, RedisConfig.K_SERVER_TOKEN_COOKIE_EXPIRED, res);

            // Verify wallet signature and generate JWT - business logic in service
            const result = await this._dappLoginService.verifyWallet(address, signatureBase64);

            if (!result.success) {
                return res.sendError(result.error!);
            }

            const resData = {
                jwt: result.jwt,
                userName: address
            };

            this._logger.info(`Wallet verification successful for ${address}`);
            return res.sendSuccess(resData);
        } catch (e) {
            this._logger.error((e as Error).message);
            return res.sendGenericError();
        }
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
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            const uid = await this._registerService.createSensparkAccount(userName, password, email);
            if (!uid) {
                res.sendError(K_SERVER_ERR);
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
     * Handler for creating or updating a Fi account
     */
    public async createAccountFi(req: Request, res: Response) {
        try {
            const userName = req.body.username;
            const password = req.body.password;
            const email = req.body.email;

            if (!userName || !password || !email) {
                this._logger.error('Missing userName, password, or email');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            // Get the JWT from header
            const jwtDapp = await this._jwtService.getJwtFromHeader(req);
            const userNameFromJwt = jwtDapp.userName;
            if (!userNameFromJwt) {
                this._logger.error('Invalid jwt payload for createAccountFi');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }
            
            // userName trong jwt là address vì user này login bằng ví mới đc gọi request này
            const address = userNameFromJwt;

            const uid = await this._registerService.createAccountFi(userName, password, email, address);
            if (!uid) {
                res.sendError('Failed to create or update Fi account', 500);
                return;
            }
            this._logger.info(`Create or update Fi account success: ${userName} with address: ${address}`);
            res.sendSuccess(uid);
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
    }

    /**
     * Handler for changing user password
     */
    public async changePassword(req: Request, res: Response) {
        try {

            const userNameFromDapp = req.body.userName;

            // Basic check, user call api must have api token in cookie
            if (!this._botSecurity.validateDappSecondRequest(userNameFromDapp, req, this._logger)) {
                this._logger.error(`${userNameFromDapp} missing Api Token in changePassword`);
                await randomResponse(res);
                return;
            }

            const jwtDapp = await this._jwtService.getJwtFromHeader(req);
            let userName = jwtDapp.userName
            if (!userName) {
                this._logger.error('Invalid jwt payload for change password');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            const currentPassword = req.body.password;
            const newPassword = req.body.newPassword;

            if (!currentPassword || !newPassword) {
                this._logger.error('Missing userName or currentPassword or newPassword');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            userName = await this.convertToUserNameIfIsWallet(userName);

            const success = await this._passwordService.changePassword(userName, currentPassword, newPassword);
            if (!success) {
                res.sendError(K_SERVER_ERR);
                return;
            }

            this._logger.info(`Password changed successfully for username: ${userName}`);
            res.sendSuccess({success: true});
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
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            // Basic check, user call api must have api token in cookie
            if (!this._botSecurity.validateDappSecondRequest(userName, req, this._logger)) {
                this._logger.error(`${userName} missing Api Token in changeNickname`);
                await randomResponse(res);
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
                res.sendError(K_SERVER_ERR);
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
     * Handle forgot password request
     */
    public async forgotPassword(req: Request, res: Response) {
        try {
            const email = req.body.email;

            if (!email) {
                this._logger.error('Missing email in forgotPassword request');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            const result = await this._passwordService.forgotPassword(email);
            if (!result) {
                res.sendError(K_SERVER_ERR);
                return;
            }

            this._logger.info(`Forgot password email sent successfully to: ${email}`);
            res.sendSuccess({success: true});
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
    }

    /**
     * Handle reset password request
     */
    public async resetPassword(req: Request, res: Response) {
        try {
            const token = req.body.token;
            const newPassword = req.body.newPassword;

            if (!token || !newPassword) {
                this._logger.error('Missing token or newPassword');
                res.sendError('2@@Missing data');
                return;
            }

            const userName = await this._passwordService.resetPassword(token, newPassword);
            if (!userName) {
                res.sendError('Failed to reset password', 400);
                return;
            }

            this._logger.info(`Password reset successfully for username: ${userName}`);
            res.sendSuccess({success: true});
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
    }

    /**
     * Handle force change password request
     * This function allows changing a user's password without requiring the current password
     */
    public async forceChangePassword(req: Request, res: Response) {
        try {
            const newPassword = req.body.newPassword;
            const userNameFromDapp = req.body.userName;

            // Basic check, user call api must have api token in cookie
            if (!this._botSecurity.validateDappSecondRequest(userNameFromDapp, req, this._logger)) {
                this._logger.error(`${userNameFromDapp} missing Api Token in forceChangePassword`);
                await randomResponse(res);
                return;
            }

            const jwtDapp = await this._jwtService.getJwtFromHeader(req);
            let userName = jwtDapp.userName;
            if (!userName) {
                this._logger.error('Invalid jwt payload for forceChangePassword');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            if (!newPassword) {
                this._logger.error('Missing newPassword');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            userName = await this.convertToUserNameIfIsWallet(userName);
            if (!userName) {
                this._logger.error(`Failed to convert userName: ${userName} to address`);
                res.sendError(K_SERVER_ERR);
            }

            const success = await this._passwordService.forceChangePassword(userName, newPassword);
            if (!success) {
                this._logger.error(`User ${userName} failed to force change password`);
                res.sendError(K_SERVER_ERR);
                return;
            }

            this._logger.info(`Password force changed successfully for username: ${userName}`);
            res.sendSuccess({success: true});
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
    }

    /**
     * Assign wallet to user Tr make it become account Fi
     */
    public async assignWalletToAccount(req: Request, res: Response) {
        try {
            const signature = req.body.signature;
            const walletAddress = req.body.address;
            const userNameFromDapp = req.body.userName;

            if (!userNameFromDapp || !signature || !walletAddress) {
                this._logger.error('Missing signature or walletAddress in assignWalletToAccount');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            const address = walletAddress.toLowerCase();

            // Basic check, user call api must have api token in cookie
            if (!this._botSecurity.validateDappSecondRequest(userNameFromDapp, req, this._logger)) {
                this._logger.error(`${address} missing Api Token in verifyJwt`);
                await randomResponse(res);
                return;
            }

            //Check jwt
            const jwtDapp = await this._jwtService.getJwtFromHeader(req);
            const userName = jwtDapp.userName;

            if (!userName) {
                this._logger.error(`${address} Invalid userName in payload for assignWalletToAccount`);
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }
            
            // Verify signature first using wallet service
            const isValidSignature = await this._dappLoginService.checkProof(address, signature);
            if (!isValidSignature) {
                this._logger.error(`${userName} - ${address} Invalid signature in assignWalletToAccount`);
                res.sendError("Signature not match with address");
                return;
            }
            
            // Call the profile service to handle assignment after signature verification
            const result = await this._profileService.AssignWalletToAccount(userName, address);
            
            if (!result) {
                this._logger.error(`${userName} - ${address} User cannot assign wallet to account`);
                res.sendError(K_SERVER_ERR);
                return;
            }

            res.sendSuccess({success: true});
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
    }

    /**
     * Update user avatar
     */
    public async setAvatar(req: Request, res: Response) {
        try {
            // Validate JWT and get username
            const jwtDapp = await this._jwtService.getJwtFromHeader(req);
            const userName = jwtDapp.userName;
            if (!userName) {
                this._logger.error('Invalid jwt payload for setAvatar');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            // Get avatar ID from request body - expect it as a string
            const avatar = req.body.avatar as string;
            if (avatar === undefined || avatar === null) {
                this._logger.error('Missing avatar value');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            // Call the service to handle validation and updating the avatar
            const result = await this._profileService.setAvatarFromString(userName, avatar);
            if (!result.success) {
                this._logger.error(`Failed to update avatar: ${result.error}`);
                res.sendError(result.error || 'Failed to update avatar', 500);
                return;
            }

            this._logger.info(`Avatar updated successfully for user: ${userName} to ${avatar}`);
            res.sendSuccess({success: true});
        } catch (e) {
            this._logger.error((e as Error).stack);
            res.sendError(e.message, 403);
        }
    }

    /**
     * Handler for getting hero characters for a user
     * Uses GET method, extracts userName from JWT and type from query
     */
    public async getAvatarDapp(req: Request, res: Response) {
        try {
            // Get type from query parameters
            const type = req.query.type as string;

            if (!type) {
                this._logger.error('Missing type in getHeroCharacter query');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            // Get userName from JWT in header
            const jwtDapp = await this._jwtService.getJwtFromHeader(req);
            const userName = jwtDapp.userName;

            if (!userName) {
                this._logger.error('Invalid JWT payload in getHeroCharacter');
                res.sendError(K_MISSING_DATA_ERR);
                return;
            }

            const characters = await this._profileService.getAvatarDapp(userName, type);
            this._logger.info(`Retrieved ${characters.length} hero characters for user: ${userName}, type: ${type}`);
            res.sendSuccess({characters});
        } catch (e) {
            this._logger.error(`Error in getHeroCharacter: ${(e as Error).stack}`);
            res.sendError(e.message, 403);
        }
    }

    /**
     * Do user dapp login bằng wallet nên userName có thể là address nên cần convert qua userName
     */
    private async convertToUserNameIfIsWallet(userName: string) : Promise<string>{
        if(ethers.isAddress(userName)){
                userName = await this._databaseAccess.getUsernameByAddress(userName.toLowerCase());
                if (!userName) {
                    this._logger.error(`Failed to get username by address: ${userName}`);
                    throw new Error('Failed to get username by address');
                }
                return userName;
        }
        return userName;
    }
}
