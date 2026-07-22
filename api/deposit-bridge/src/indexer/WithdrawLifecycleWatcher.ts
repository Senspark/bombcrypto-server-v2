import ILogger from "../services/ILogger";
import IBlockchainCenter from "../services/IBlockchainCenter";
import {BRIDGE_INDEXER_ABI, CENTER_NETWORK, Chain, CONFIRMED_DEPTH} from "../consts/Consts";
import BridgeReportRepository from "./BridgeReportRepository";
import SignerEvents, {ISignedEvent} from "./SignerEvents";

// Tracks each signed withdraw through signed -> executed | expired so no authorization hangs in an unknown
// state. Driven by the in-process "signed" event; a slow poll resolves each record from the on-chain
// withdrawn counter. executed = withdrawn caught up to the signed otherDeposited (the contract sets
// withdrawn += otherDeposited - withdrawn); expired = deadline passed without that happening.
export default class WithdrawLifecycleWatcher {
    readonly #logger: ILogger;
    readonly #repo: BridgeReportRepository;
    readonly #center: IBlockchainCenter;
    readonly #configs: Map<Chain, {bridgeAddress: string}>;
    readonly #pollMs: number;
    #stopped = false;

    constructor(
        logger: ILogger, repo: BridgeReportRepository, center: IBlockchainCenter,
        configs: Map<Chain, {bridgeAddress: string}>, signerEvents: SignerEvents, pollMs: number,
    ) {
        this.#logger = logger.clone('[Lifecycle]');
        this.#repo = repo;
        this.#center = center;
        this.#configs = configs;
        this.#pollMs = pollMs;
        signerEvents.onSigned((e) => this.#onSigned(e));
    }

    start(): void {
        this.#pollLoop().catch((e) => this.#logger.error(e));
    }

    stop(): void {
        this.#stopped = true;
    }

    // Open a lifecycle record. Fire-and-forget: a DB hiccup here must never affect the signing reply.
    #onSigned(e: ISignedEvent): void {
        this.#logger.info(`signed ${e.symbol} ${e.wallet} on ${e.chain} other=${e.otherDeposited} deadline=${e.deadline}`);
        this.#repo
            .insertLifecycleSigned(e.chain, e.token, e.symbol, e.wallet, e.otherDeposited, e.deadline)
            .catch((err) => this.#logger.error(err));
    }

    async #pollLoop(): Promise<void> {
        while (!this.#stopped) {
            await this.#sleep(this.#pollMs);
            try {
                const pending = await this.#repo.pendingLifecycle();
                const nowSec = Math.floor(Date.now() / 1000);
                for (const row of pending) {
                    try {
                        await this.#resolve(row.id, row.chain, row.token, row.wallet, BigInt(row.other_deposited), row.deadline, nowSec);
                    } catch (e) {
                        // One bad read shouldn't stall the rest — leave it 'signed' for the next poll.
                        this.#logger.warn(`resolve lifecycle #${row.id} failed: ${(e as Error).message}`);
                    }
                }
            } catch (e) {
                this.#logger.error(e);
            }
        }
    }

    async #resolve(
        id: number, chain: Chain, token: string, wallet: string, otherDeposited: bigint, deadline: number, nowSec: number,
    ): Promise<void> {
        const bridge = this.#configs.get(chain)?.bridgeAddress;
        if (!bridge) return;
        const raw = await this.#center.callContract(
            CENTER_NETWORK[chain], bridge, BRIDGE_INDEXER_ABI, 'withdrawn', [wallet, token], CONFIRMED_DEPTH[chain],
        );
        const withdrawn = BigInt(raw);
        if (withdrawn >= otherDeposited) {
            await this.#repo.markLifecycle(id, "executed");
            this.#logger.info(`lifecycle #${id} executed (${wallet} on ${chain})`);
        } else if (nowSec > deadline) {
            await this.#repo.markLifecycle(id, "expired");
            this.#logger.info(`lifecycle #${id} expired (${wallet} on ${chain})`);
        }
    }

    #sleep(ms: number): Promise<void> {
        return new Promise((resolve) => setTimeout(resolve, ms));
    }
}
