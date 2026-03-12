import express from "express";
import TonHandlers from "../Handlers/TonHandlers";
import {IDependencies} from "../DependenciesInjector";

export function createTonRouter(dependencies: IDependencies): express.Router {
    const tonRouter = express.Router();
    const tonHandlers = new TonHandlers(dependencies);

    // Server call
    tonRouter.post(`/verify`, tonHandlers.verifyLoginToken.bind(tonHandlers));
    
    // React call
    tonRouter.post(`/generate_payload`, tonHandlers.notSupported.bind(tonHandlers));
    tonRouter.post(`/nonce`, tonHandlers.generateNonce.bind(tonHandlers));
    tonRouter.post(`/check_proof`, tonHandlers.checkProof.bind(tonHandlers));
    tonRouter.post(`/refresh`, tonHandlers.refreshJwtToken.bind(tonHandlers));
    tonRouter.get(`/editor_get_jwt`, tonHandlers.signEditorJwt.bind(tonHandlers));
    tonRouter.get(`/check_server`, tonHandlers.checkServerMaintain.bind(tonHandlers));

    return tonRouter;
}
