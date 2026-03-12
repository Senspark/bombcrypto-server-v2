import express from "express";
import { WebHandlers } from "../Handlers/WebHandler";
import {IDependencies} from "../DependenciesInjector";
import {RonHandlers} from "../Handlers/RonHandlers";
import {BasHandlers} from "../Handlers/BasHandlers";
import {BscHandlers} from "../Handlers/BscHandlers";
import {PolHandlers} from "../Handlers/PolHandlers";


export function createWebRouter(dependencies: IDependencies): express.Router {
    const webRouter = express.Router();

    const webHandlers = new WebHandlers(dependencies);
    registerLegacyBscPolygonRoutes(webRouter, webHandlers);

    // New network-specific handlers
    const bscHandlers = new BscHandlers(dependencies);
    const polHandlers = new PolHandlers(dependencies);
    const ronHandlers = new RonHandlers(dependencies);
    const basHandlers = new BasHandlers(dependencies);

    // Register new network-specific routes
    registerBscRoutes(webRouter, bscHandlers);
    registerPolRoutes(webRouter, polHandlers);
    registerRonRoutes(webRouter, ronHandlers);
    registerBasRoutes(webRouter, basHandlers);

    return webRouter;
}

// Legacy function for backward compatibility - supports old clients using root level routes
function registerLegacyBscPolygonRoutes(router: express.Router, handlers: WebHandlers) {
    router.post("/verify", handlers.verifyLoginData.bind(handlers));
    // router.post("/nonce", handlers.generateNonce.bind(handlers));
    // router.post("/check_proof", handlers.checkProof.bind(handlers));
    router.post("/check_proof_account", handlers.checkProofAccount.bind(handlers));
    // router.get("/editor_get_jwt", handlers.signEditorJwt.bind(handlers));
    // router.get("/editor_get_jwt_account", handlers.getJwtForAccountFromEditor.bind(handlers));
    // router.get("/refresh/:rf", handlers.refreshJwtToken.bind(handlers));
    router.get("/ban_list", handlers.getBannedList.bind(handlers));
    router.get("/check_server", handlers.checkServerMaintain.bind(handlers));
    // router.post("/change_nick_name", handlers.changeNickname.bind(handlers));
}

function registerBscRoutes(router: express.Router, handlers: BscHandlers) {
    router.post("/bsc/verify", handlers.verifyLoginData.bind(handlers));
    router.post("/bsc/nonce", handlers.generateNonce.bind(handlers));
    router.post("/bsc/check_proof", handlers.checkProof.bind(handlers));
    router.post("/bsc/check_proof_account", handlers.checkProofAccount.bind(handlers));
    router.get("/bsc/editor_get_jwt", handlers.signEditorJwt.bind(handlers));
    router.get("/bsc/editor_get_jwt_account", handlers.getJwtForAccountFromEditor.bind(handlers));
    router.get("/bsc/refresh/:rf", handlers.refreshJwtToken.bind(handlers));
    router.get("/bsc/ban_list", handlers.getBannedList.bind(handlers));
    router.get("/bsc/check_server", handlers.checkServerMaintain.bind(handlers));
    router.post("/bsc/change_nick_name", handlers.changeNickname.bind(handlers));
}

function registerPolRoutes(router: express.Router, handlers: PolHandlers) {
    router.post("/pol/verify", handlers.verifyLoginData.bind(handlers));
    router.post("/pol/nonce", handlers.generateNonce.bind(handlers));
    router.post("/pol/check_proof", handlers.checkProof.bind(handlers));
    router.post("/pol/check_proof_account", handlers.checkProofAccount.bind(handlers));
    router.get("/pol/editor_get_jwt", handlers.signEditorJwt.bind(handlers));
    router.get("/pol/editor_get_jwt_account", handlers.getJwtForAccountFromEditor.bind(handlers));
    router.get("/pol/refresh/:rf", handlers.refreshJwtToken.bind(handlers));
    router.get("/pol/ban_list", handlers.getBannedList.bind(handlers));
    router.get("/pol/check_server", handlers.checkServerMaintain.bind(handlers));
    router.post("/pol/change_nick_name", handlers.changeNickname.bind(handlers));
}

function registerRonRoutes(router: express.Router, handlers: RonHandlers) {
    router.post("/ron/verify", handlers.verifyLoginData.bind(handlers));
    router.post("/ron/nonce", handlers.generateNonce.bind(handlers));
    router.post("/ron/check_proof", handlers.checkProof.bind(handlers));
    router.get("/ron/editor_get_jwt", handlers.signEditorJwt.bind(handlers));
    router.get("/ron/refresh/:rf", handlers.refreshJwtToken.bind(handlers));
    router.get("/ron/check_server", handlers.checkServerMaintain.bind(handlers));
    router.post("/ron/change_nick_name", handlers.changeNickname.bind(handlers));
}

function registerBasRoutes(router: express.Router, handlers: BasHandlers) {
    router.post("/bas/verify", handlers.verifyLoginData.bind(handlers));
    router.post("/bas/nonce", handlers.generateNonce.bind(handlers));
    router.post("/bas/check_proof", handlers.checkProof.bind(handlers));
    router.get("/bas/editor_get_jwt", handlers.signEditorJwt.bind(handlers));
    router.get("/bas/refresh/:rf", handlers.refreshJwtToken.bind(handlers));
    router.get("/bas/check_server", handlers.checkServerMaintain.bind(handlers));
    router.post("/bas/change_nick_name", handlers.changeNickname.bind(handlers));
}