import express, {Router} from "express";
import {fileURLToPath} from "url";
import {dirname} from "path";
import Dependencies from "./Dependencies";
import {computeCodeHash} from "./codeHash";
import BlockchainCenterApi from "./services-impl/BlockchainCenterApi";
import Database from "./services-impl/Database";
import DepositReader from "./services-impl/DepositReader";
import Signer from "./services-impl/Signer";
import SignerEvents from "./indexer/SignerEvents";
import IndexerService from "./indexer/IndexerService";
import BridgeReportRepository from "./indexer/BridgeReportRepository";
import BlockTimeResolver from "./indexer/BlockTimeResolver";
import MonitorService from "./monitor/MonitorService";
import MonitorCache from "./monitor/MonitorCache";
import MonitorInvalidator from "./monitor/MonitorInvalidator";
import buildMonitorRouter from "./monitor/MonitorRouter";
import getRedisClient from "./services-impl/Redis";
import {buildNetworkConfigs, StreamKeys} from "./consts/Consts";
import {IBridgeRequest} from "./consts/Messages";

const dependencies = new Dependencies();
const logger = dependencies.logger;
const envConfig = dependencies.envConfig;

// version = deploy label (build number/time/sha from the deploy script); code = fingerprint of the running
// bytes. Watch `code`: it only changes when the actual code does, so a bumped `version` with an unchanged
// `code` means the rebuild did NOT take effect (cache hit or container not recreated).
const buildInfo = {version: envConfig.appVersion, code: computeCodeHash(dirname(fileURLToPath(import.meta.url)))};

try {
    const center = new BlockchainCenterApi(envConfig.blockchainCenterApi, logger);
    const configs = buildNetworkConfigs(envConfig.isProduction);
    const reader = new DepositReader(logger, center, configs, envConfig.depositCacheTtlMs);
    const signerEvents = new SignerEvents();
    const signer = new Signer(logger, envConfig, reader, configs, signerEvents);

    dependencies.messenger.listen(StreamKeys.SV_DEPBRIDGE_REQUEST_STR, (msg: IBridgeRequest) => {
        signer.signWithdraw(msg)
            .then((result) => dependencies.messenger.send(StreamKeys.AP_DEPBRIDGE_RESULT_STR, result))
            .catch((e) => logger.error(e));
    });

    // The indexer is optional and isolated: if its DB is unreachable, log and keep signing. The monitor REST
    // layer shares that same DB/repo, so it only mounts when the indexer is enabled.
    let indexer: IndexerService | undefined;
    let monitorRouter: Router | undefined;
    if (envConfig.indexerEnabled) {
        try {
            const db = new Database(envConfig.indexerDatabaseUrl, logger);
            const repo = new BridgeReportRepository(db);
            const monitorCache = new MonitorCache(getRedisClient(envConfig.redisConnectionString), logger, envConfig.monitorCacheTtlMs);
            const blockTime = new BlockTimeResolver(center, monitorCache, logger);
            const invalidator = new MonitorInvalidator(monitorCache);
            indexer = new IndexerService(logger, envConfig, db, repo, center, configs, signerEvents, blockTime, dependencies.messenger, invalidator);
            indexer.start();
            const monitorService = new MonitorService(logger, center, configs, repo, monitorCache, blockTime);
            monitorRouter = buildMonitorRouter(logger, repo, monitorService, buildInfo);
        } catch (e) {
            logger.error("indexer failed to start — continuing as signer only:");
            logger.error(e);
        }
    }

    const app = express();
    app.use(express.json({limit: '25mb'})); // /monitor/backfill accepts a block-explorer log export
    app.get('/health', (_req, res) => res.json({ok: true, ...buildInfo, signer: signer.signerAddress}));
    if (monitorRouter) app.use('/monitor', monitorRouter);
    const server = app.listen(envConfig.port, () => {
        logger.info(`ap-deposit-bridge ${buildInfo.version} (code ${buildInfo.code}) started — health on :${envConfig.port}, signer ${signer.signerAddress}`);
    });

    const shutdown = () => {
        logger.info("shutting down…");
        indexer?.stop().catch((e) => logger.error(e));
        server.close();
        process.exit(0);
    };
    process.on('SIGTERM', shutdown);
    process.on('SIGINT', shutdown);
} catch (e) {
    logger.error("Error starting ap-deposit-bridge:");
    logger.error(e);
    process.exit(1);
}
