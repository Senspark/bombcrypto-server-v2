import express from "express";
import { MobileHandlers } from "../Handlers/MobileHandler";
import {IDependencies} from "../DependenciesInjector";

export function createMobileRouter(dependencies: IDependencies): express.Router {
    const mobileRouter = express.Router();
    const mobileHandlers = new MobileHandlers(dependencies);

    // Server call
    mobileRouter.post(`/verify`, mobileHandlers.verifyLoginData.bind(mobileHandlers));
    
    // Client unity call directly
    mobileRouter.post(`/refresh`, mobileHandlers.refreshJwtToken.bind(mobileHandlers));
    mobileRouter.post(`/check_proof`, mobileHandlers.checkProof.bind(mobileHandlers));
    mobileRouter.get(`/check_server`, mobileHandlers.checkServerMaintain.bind(mobileHandlers));
    mobileRouter.post(`/check_proof_guest`, mobileHandlers.checkProofForGuest.bind(mobileHandlers));
    
    // Update profile
    mobileRouter.get(`/create_guest_account`, mobileHandlers.createGuestAccount.bind(mobileHandlers));
    mobileRouter.post(`/create_senspark_account`, mobileHandlers.createSensparkAccount.bind(mobileHandlers));
    mobileRouter.post(`/change_nick_name`, mobileHandlers.changeNickname.bind(mobileHandlers));

    return mobileRouter;
}
