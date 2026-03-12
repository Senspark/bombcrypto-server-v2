import express from "express";
import { DappHandler } from "../Handlers/DappHandler";
import {IDependencies} from "../DependenciesInjector";

export function createDappRouter(dependencies: IDependencies): express.Router {
    const dappRouter = express.Router();
    const dappHandlers = new DappHandler(dependencies);

    // Dapp login
    dappRouter.post(`/verify_account`, dappHandlers.verifyAccount.bind(dappHandlers));
    dappRouter.post(`/get_nonce`, dappHandlers.getNonce.bind(dappHandlers));
    dappRouter.post(`/verify_signature`, dappHandlers.verifyWallet.bind(dappHandlers));
    // Used for login after having jwt
    dappRouter.get(`/profile`, dappHandlers.checkJwt.bind(dappHandlers));

    // Create account for dapp
    dappRouter.post(`/create_senspark_account`, dappHandlers.createSensparkAccount.bind(dappHandlers));
    dappRouter.post(`/create_account_fi`, dappHandlers.createAccountFi.bind(dappHandlers));
    // Update password for dapp
    dappRouter.post(`/change_password`, dappHandlers.changePassword.bind(dappHandlers));
    dappRouter.post(`/force_change_password`, dappHandlers.forceChangePassword.bind(dappHandlers));
    dappRouter.post(`/forgot_password`, dappHandlers.forgotPassword.bind(dappHandlers));
    dappRouter.post(`/reset_password`, dappHandlers.resetPassword.bind(dappHandlers));
    // Update profile
    dappRouter.post(`/assign_wallet_to_account`, dappHandlers.assignWalletToAccount.bind(dappHandlers));
    dappRouter.post(`/set_avatar`, dappHandlers.setAvatar.bind(dappHandlers));
    dappRouter.get(`/get_avatar`, dappHandlers.getAvatarDapp.bind(dappHandlers));
    dappRouter.post(`/change_nick_name`, dappHandlers.changeNickname.bind(dappHandlers));
    
    return dappRouter;
}
