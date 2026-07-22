import ILogger from "../services/ILogger";
import IBlockchainCenter from "../services/IBlockchainCenter";
import {
    BRIDGE_INDEXER_ABI, CENTER_NETWORK, Chain, CONFIRMED_DEPTH, INetworkConfig, oppositeChain, TokenSymbol,
} from "../consts/Consts";
import BridgeReportRepository from "./BridgeReportRepository";

// Alert-only detection: NEVER writes on-chain — an operator pulls setWithdrawEnabled(false) by hand. Three
// checks on a loose timer:
//   1. CROSS_CHAIN_INSOLVENT  — totalWithdrawn(chain) > totalDeposited(opposite): the signer over-authorized
//   2. PER_USER_OVER_WITHDRAW — a wallet withdrew more than it deposited on the opposite chain (from DB)
//   3. CHECKSUM_MISMATCH      — Σ(DB deposited) exceeds on-chain totalDeposited (double-count / index bug)
// Same-chain solvency is deliberately not checked — those counters self-balance, so they can't catch this.
export default class AnomalyChecker {
    readonly #logger: ILogger;
    readonly #repo: BridgeReportRepository;
    readonly #center: IBlockchainCenter;
    readonly #configs: Map<Chain, INetworkConfig>;
    readonly #intervalMs: number;
    #stopped = false;

    constructor(
        logger: ILogger, repo: BridgeReportRepository, center: IBlockchainCenter,
        configs: Map<Chain, INetworkConfig>, intervalMs: number,
    ) {
        this.#logger = logger.clone('[Anomaly]');
        this.#repo = repo;
        this.#center = center;
        this.#configs = configs;
        this.#intervalMs = intervalMs;
    }

    start(): void {
        this.#loop().catch((e) => this.#logger.error(e));
    }

    stop(): void {
        this.#stopped = true;
    }

    async #loop(): Promise<void> {
        while (!this.#stopped) {
            await this.#sleep(this.#intervalMs);
            await this.runOnce();
        }
    }

    // Runs all three checks; each is isolated so one failing read doesn't skip the others.
    async runOnce(): Promise<void> {
        try {
            await this.#checkCrossChainAndChecksum();
        } catch (e) {
            this.#logger.error(e);
        }
        try {
            await this.#checkPerUser();
        } catch (e) {
            this.#logger.error(e);
        }
    }

    async #checkCrossChainAndChecksum(): Promise<void> {
        for (const symbol of Object.values(TokenSymbol)) {
            for (const chain of Object.values(Chain)) {
                const cfg = this.#configs.get(chain)!;
                const opp = oppositeChain(chain);
                const oppCfg = this.#configs.get(opp)!;
                const tokenC = cfg.tokens[symbol];
                const tokenOpp = oppCfg.tokens[symbol];

                const totalWithdrawnC = await this.#readCounter(chain, cfg.bridgeAddress, 'totalWithdrawn', tokenC);
                const totalDepositedOpp = await this.#readCounter(opp, oppCfg.bridgeAddress, 'totalDeposited', tokenOpp);

                // CROSS_CHAIN_INSOLVENT — the whole point of the module.
                if (totalWithdrawnC > totalDepositedOpp) {
                    await this.#raise("CROSS_CHAIN_INSOLVENT", chain, tokenC, undefined, {
                        symbol, withdrawChain: chain, totalWithdrawn: totalWithdrawnC.toString(),
                        depositChain: opp, totalDepositedOpposite: totalDepositedOpp.toString(),
                    });
                }

                // CHECKSUM_MISMATCH — indexed deposits must never exceed the on-chain counter.
                const dbSumDeposited = await this.#repo.sumDeposited(chain, tokenC);
                const totalDepositedC = await this.#readCounter(chain, cfg.bridgeAddress, 'totalDeposited', tokenC);
                if (dbSumDeposited > totalDepositedC) {
                    await this.#raise("CHECKSUM_MISMATCH", chain, tokenC, undefined, {
                        symbol, dbSumDeposited: dbSumDeposited.toString(), onchainTotalDeposited: totalDepositedC.toString(),
                    });
                }
                // dbSum < on-chain just means the sweep is still catching up — not an anomaly.
            }
        }
    }

    async #checkPerUser(): Promise<void> {
        const rows = await this.#repo.perUserOverWithdrawn();
        for (const r of rows) {
            const cfg = this.#configs.get(r.chain)!;
            const opp = oppositeChain(r.chain);
            const oppCfg = this.#configs.get(opp)!;
            const tokenC = cfg.tokens[r.symbol];
            const tokenOpp = oppCfg.tokens[r.symbol];

            // Live-confirm: the DB check lags cross-chain ingestion — a wallet that legitimately deposited on the
            // opposite chain can look over-withdrawn until that deposit is swept. Re-read on-chain and only raise
            // if it's still a real breach, so ingest lag never fires a false alert.
            const withdrawnLive = await this.#readMapping(r.chain, cfg.bridgeAddress, 'withdrawn', r.wallet, tokenC);
            const oppDepositedLive = await this.#readMapping(opp, oppCfg.bridgeAddress, 'deposited', r.wallet, tokenOpp);
            if (withdrawnLive <= oppDepositedLive) continue;

            await this.#raise("PER_USER_OVER_WITHDRAW", r.chain, tokenC, r.wallet, {
                symbol: r.symbol, withdrawn: withdrawnLive.toString(), oppositeDeposited: oppDepositedLive.toString(),
            });
        }
    }

    async #readMapping(chain: Chain, bridge: string, method: string, wallet: string, token: string): Promise<bigint> {
        const raw = await this.#center.callContract(
            CENTER_NETWORK[chain], bridge, BRIDGE_INDEXER_ABI, method, [wallet, token], CONFIRMED_DEPTH[chain],
        );
        return BigInt(raw);
    }

    async #readCounter(chain: Chain, bridge: string, method: string, token: string): Promise<bigint> {
        const raw = await this.#center.callContract(
            CENTER_NETWORK[chain], bridge, BRIDGE_INDEXER_ABI, method, [token], CONFIRMED_DEPTH[chain],
        );
        return BigInt(raw);
    }

    // Insert dedupes on an already-open anomaly, so log only when a genuinely NEW one is recorded.
    async #raise(kind: string, chain: Chain, token: string | undefined, wallet: string | undefined, detail: object): Promise<void> {
        const inserted = await this.#repo.insertAnomaly(kind, detail, chain, token, wallet);
        if (inserted) this.#logger.error(`ANOMALY ${kind} chain=${chain} ${JSON.stringify(detail)}`);
    }

    #sleep(ms: number): Promise<void> {
        return new Promise((resolve) => setTimeout(resolve, ms));
    }
}
