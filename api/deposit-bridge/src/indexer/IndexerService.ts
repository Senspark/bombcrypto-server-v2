import ILogger from "../services/ILogger";
import IEnvConfig from "../services/IEnvConfig";
import IBlockchainCenter from "../services/IBlockchainCenter";
import IDatabase from "../services/IDatabase";
import IMessengerService from "../services/IMessengerService";
import {Chain, IBridgeNotify, INetworkConfig, StreamKeys} from "../consts/Consts";
import BridgeReportRepository from "./BridgeReportRepository";
import EventDecoder from "./EventDecoder";
import BlockTimeResolver from "./BlockTimeResolver";
import ReconcileSweeper from "./ReconcileSweeper";
import WithdrawLifecycleWatcher from "./WithdrawLifecycleWatcher";
import AnomalyChecker from "./AnomalyChecker";
import SignerEvents from "./SignerEvents";
import {IMonitorInvalidator} from "../monitor/MonitorInvalidator";

// Lives in the same process as the signer but shares no state with the signing path: it only READS the chain
// and its OWN report DB. Every worker isolates its own faults, so an indexer crash/hang never touches the
// signing reply. Report-only — no on-chain writes anywhere.
export default class IndexerService {
    readonly #logger: ILogger;
    readonly #env: IEnvConfig;
    readonly #db: IDatabase;
    readonly #repo: BridgeReportRepository;
    readonly #messenger: IMessengerService;
    readonly #configs: Map<Chain, INetworkConfig>;
    readonly #sweepers = new Map<Chain, ReconcileSweeper>();
    readonly #lifecycle: WithdrawLifecycleWatcher;
    readonly #anomaly: AnomalyChecker;

    constructor(
        logger: ILogger, env: IEnvConfig, db: IDatabase, repo: BridgeReportRepository,
        center: IBlockchainCenter, configs: Map<Chain, INetworkConfig>, signerEvents: SignerEvents,
        blockTime: BlockTimeResolver, messenger: IMessengerService, invalidator: IMonitorInvalidator,
    ) {
        this.#logger = logger.clone('[Indexer]');
        this.#env = env;
        this.#db = db;
        this.#configs = configs;
        this.#repo = repo;
        this.#messenger = messenger;
        const decoder = new EventDecoder();

        for (const cfg of configs.values()) {
            this.#sweepers.set(
                cfg.chain,
                new ReconcileSweeper(
                    this.#logger, this.#repo, center, decoder, blockTime, cfg, env.indexerSweepIntervalMs,
                    env.indexerSweepActiveIntervalMs, env.indexerSweepActiveWindowMs, this.#startBlockFor(cfg.chain),
                    invalidator,
                ),
            );
        }
        this.#lifecycle = new WithdrawLifecycleWatcher(
            this.#logger, this.#repo, center, configs, signerEvents, env.indexerLifecyclePollMs,
        );
        this.#anomaly = new AnomalyChecker(this.#logger, this.#repo, center, configs, env.indexerSweepIntervalMs);
    }

    // The report DB must already exist with db/schema.sql applied (the service never provisions it). Each
    // sweeper bootstraps itself (jump-to-head + gap record) on its first tick, retrying if the DB/RPC is not
    // yet reachable, so a slow dependency never crashes the signer.
    start(): void {
        for (const sweeper of this.#sweepers.values()) sweeper.start();
        this.#lifecycle.start();
        this.#anomaly.start();
        // SmartFox activity feed: each notify just kicks the target chain's sweeper into fast mode. Never a
        // safety gate (the idle sweep still covers everything) — so a bad payload is logged and dropped.
        this.#messenger.listen(StreamKeys.SV_DEPBRIDGE_NOTIFY_STR, (n: IBridgeNotify) => this.#onNotify(n));
        this.#logger.info("indexer started (sweep + lifecycle + anomaly + notify)");
    }

    #onNotify(n: IBridgeNotify): void {
        const chain = this.#toChain(n?.chain);
        const sweeper = chain && this.#sweepers.get(chain);
        if (!sweeper) {
            this.#logger.warn(`ignored notify with bad chain=${n?.chain} kind=${n?.kind}`);
            return;
        }
        this.#logger.info(`notify ${n.kind} ${n.wallet} on ${chain}${n.txHash ? ` tx=${n.txHash}` : ''} — accelerating sweep`);
        sweeper.signalActivity();
    }

    #toChain(raw: string | undefined): Chain | null {
        if (raw === Chain.BSC || raw === Chain.POLYGON) return raw;
        return null;
    }

    #startBlockFor(chain: Chain): number {
        return chain === Chain.BSC ? this.#env.indexerStartBlockBsc : this.#env.indexerStartBlockPolygon;
    }

    async stop(): Promise<void> {
        for (const sweeper of this.#sweepers.values()) sweeper.stop();
        this.#lifecycle.stop();
        this.#anomaly.stop();
        await this.#db.close();
    }
}
