import express from "express";
import { SolHandlers } from "../Handlers/SolHandlers";
import {IDependencies} from "../DependenciesInjector";

export function createSolRouter(dependencies: IDependencies): express.Router {
    const solRouter = express.Router();
    const solHandlers = new SolHandlers(dependencies);

    // Server call
    solRouter.post(`/verify`, solHandlers.verifyLoginData.bind(solHandlers));
    
    // React call
    solRouter.post(`/nonce`, solHandlers.generateNonce.bind(solHandlers));
    solRouter.post(`/check_proof`, solHandlers.checkProof.bind(solHandlers));
    solRouter.get(`/editor_get_jwt`, solHandlers.signEditorJwt.bind(solHandlers));
    solRouter.get(`/refresh`, solHandlers.refreshJwtToken.bind(solHandlers));
    solRouter.get(`/ban_list`, solHandlers.getBannedList.bind(solHandlers));
    solRouter.get(`/check_server`, solHandlers.checkServerMaintain.bind(solHandlers));

    return solRouter;
}
