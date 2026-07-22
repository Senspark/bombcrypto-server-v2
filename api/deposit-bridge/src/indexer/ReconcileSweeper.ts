import ILogger from "../services/ILogger";
import IBlockchainCenter from "../services/IBlockchainCenter";
import {
    CENTER_NETWORK, Chain, CONFIRMED_DEPTH, INDEXER_BLOCK_STEP, INetworkConfig, symbolOfToken, TokenSymbol,
} from "../consts/Consts";
import BridgeReportRepository from "./BridgeReportRepository";
import EventDecoder from "./EventDecoder";
import BlockTimeResolver from "./BlockTimeResolver";
import {IMonitorInvalidator} from "../monitor/MonitorInvalidator";

// Forward tail for ONE chain. On the first iteration it jumps the cursor to the current head and records the
// untouched range (deploy block → head, or the downtime window) as a gap for manual backfill — it never
// races millions of historical blocks. Then, once per tick, it drains every confirmed block up to a FROZEN
// head snapshot in getLogs-sized chunks — so a fast chain (0.42s/block) never falls permanently behind the
// way a fixed one-chunk-per-tick cadence does. A failed read is retried in place (cursor never advances past
// unread blocks), and the loop never crashes the process.
//
// Cadence is signal-driven. Idle, it sleeps `idleMs` (a loose safety net). A SmartFox notify / withdraw calls
// signalActivity(), which wakes the current sleep immediately and puts it in ACTIVE mode: it re-drains every
// `activeMs` for a `activeWindowMs` grace period after the last signal, so a deposit/withdraw tx that lands a
// few seconds later — and only reaches confirmed depth a bit after that — is swept almost at once instead of
// waiting a full idle cycle. Once the window lapses it relaxes back to idle.
export default class ReconcileSweeper {
    readonly #logger: ILogger;
    readonly #repo: BridgeReportRepository;
    readonly #center: IBlockchainCenter;
    readonly #decoder: EventDecoder;
    readonly #blockTime: BlockTimeResolver;
    readonly #config: INetworkConfig;
    readonly #chain: Chain;
    readonly #network: string;
    readonly #depth: number;
    readonly #idleMs: number;
    readonly #activeMs: number;
    readonly #activeWindowMs: number;
    readonly #startBlock: number;
    readonly #invalidator: IMonitorInvalidator;
    #stopped = false;
    #bootstrapped = false;
    #activeUntil = 0;              // unix ms; while now < this, sweep at the fast cadence
    #wakeResolver: (() => void) | null = null; // resolves the in-flight idle/active sleep early on a signal

    constructor(
        logger: ILogger, repo: BridgeReportRepository, center: IBlockchainCenter, decoder: EventDecoder,
        blockTime: BlockTimeResolver, config: INetworkConfig, idleMs: number, activeMs: number,
        activeWindowMs: number, startBlock: number, invalidator: IMonitorInvalidator,
    ) {
        this.#chain = config.chain;
        this.#logger = logger.clone(`[Sweep:${this.#chain}]`);
        this.#repo = repo;
        this.#center = center;
        this.#decoder = decoder;
        this.#blockTime = blockTime;
        this.#config = config;
        this.#network = CENTER_NETWORK[this.#chain];
        this.#depth = CONFIRMED_DEPTH[this.#chain];
        this.#idleMs = idleMs;
        this.#activeMs = activeMs;
        this.#activeWindowMs = activeWindowMs;
        this.#startBlock = startBlock;
        this.#invalidator = invalidator;
    }

    start(): void {
        this.#loop().catch((e) => this.#logger.error(e));
    }

    stop(): void {
        this.#stopped = true;
        this.#wakeNow(); // let a sleeping loop observe #stopped and exit promptly
    }

    // Called from IndexerService when a bridge activity notify (or withdraw) targets this chain: extend the
    // fast-mode window and interrupt the current sleep so the next drain starts now. Cheap and idempotent —
    // spamming it just keeps the sweeper hot, never queues work.
    signalActivity(): void {
        this.#activeUntil = Date.now() + this.#activeWindowMs;
        this.#wakeNow();
    }

    async #loop(): Promise<void> {
        while (!this.#stopped) {
            try {
                if (!this.#bootstrapped) {
                    const enabled = await this.#bootstrap();
                    if (!enabled) return; // no start block and no cursor → nothing to anchor, sweep disabled
                    this.#bootstrapped = true;
                } else {
                    await this.#tailToHead();
                }
            } catch (e) {
                // Never advance on failure: the same range is retried next tick, so no block is skipped.
                this.#logger.error(e);
            }
            // Each drain already caught up to head; pick the gap by whether activity is still expected soon.
            const active = Date.now() < this.#activeUntil;
            await this.#sleep(active ? this.#activeMs : this.#idleMs);
        }
    }

    // Anchor the tail at the current head and record everything below it as a gap to backfill by hand.
    async #bootstrap(): Promise<boolean> {
        const head = (await this.#center.getLatestBlockNumber(this.#network)) - this.#depth;
        const existing = await this.#repo.getCursorRaw(this.#chain);
        if (existing === null && this.#startBlock <= 0) {
            this.#logger.warn(`no start block and no cursor — sweep disabled for ${this.#chain}`);
            return false;
        }
        const gapStart = existing ?? this.#startBlock;
        if (head > gapStart) {
            await this.#repo.insertGap(this.#chain, gapStart, head);
            this.#logger.info(`gap recorded [${gapStart}..${head}] — backfill manually from the explorer`);
        }
        await this.#repo.jumpCursor(this.#chain, head);
        this.#logger.info(`tailing from block ${head}`);
        return true;
    }

    // One tick: drain every confirmed block up to a head snapshot taken ONCE at the start. Freezing head is
    // what makes this terminate — chasing a live head at 0.42s/block never converges because the tip outruns
    // our own getLogs+write throughput. Blocks produced mid-drain are simply picked up next tick.
    async #tailToHead(): Promise<void> {
        const head = (await this.#center.getLatestBlockNumber(this.#network)) - this.#depth;
        let cursor = await this.#repo.getCursor(this.#chain);
        const changed = new Set<TokenSymbol>();
        let advanced = false;
        while (!this.#stopped && cursor < head) {
            const to = Math.min(cursor + INDEXER_BLOCK_STEP, head);
            const {deposits, withdraws, symbols} = await this.#processRange(cursor, to);
            await this.#repo.setCursor(this.#chain, to);
            for (const s of symbols) changed.add(s);
            advanced = true;
            this.#logger.info(`swept [${cursor}..${to}] head=${head} deposits=${deposits} withdraws=${withdraws}`);
            cursor = to;
        }
        // Invalidate only what moved: the cursor advanced → health lag changed; each token with an event → that
        // one aggregate cell. The next monitor poll re-reads just these, not the whole table.
        if (advanced) await this.#invalidator.invalidateHealth();
        for (const symbol of changed) await this.#invalidator.invalidateCell(this.#chain, symbol);
    }

    async #processRange(fromBlock: number, toBlock: number): Promise<{deposits: number; withdraws: number; symbols: Set<TokenSymbol>}> {
        const logs = await this.#center.getLogs(
            this.#network, this.#config.bridgeAddress, this.#decoder.eventTopics(), fromBlock, toBlock,
        );
        let deposits = 0;
        let withdraws = 0;
        const symbols = new Set<TokenSymbol>();
        for (const log of logs) {
            const ev = this.#decoder.decode(log);
            if (!ev) continue;
            const symbol = symbolOfToken(this.#config, ev.token);
            if (!symbol) continue; // event for a token this bridge doesn't index — skip
            symbols.add(symbol);
            const ts = await this.#blockTime.resolve(this.#network, ev.block); // cached; null only on RPC failure
            if (ev.kind === "Deposit") {
                await this.#repo.upsertDeposited(this.#chain, ev.token, symbol, ev.user, ev.depositedTotal, ev.block);
                await this.#repo.insertLedgerEvent({
                    chain: this.#chain, token: ev.token, symbol, wallet: ev.user, kind: "deposit",
                    amount: ev.amount, net: null, cumulative: ev.depositedTotal,
                    block: ev.block, txHash: ev.txHash, logIndex: ev.logIndex, ts,
                });
                deposits++;
            } else {
                await this.#repo.upsertWithdrawn(this.#chain, ev.token, symbol, ev.user, ev.withdrawnTotal, ev.block);
                await this.#repo.insertLedgerEvent({
                    chain: this.#chain, token: ev.token, symbol, wallet: ev.user, kind: "withdraw",
                    amount: ev.amount, net: ev.net, cumulative: ev.withdrawnTotal,
                    block: ev.block, txHash: ev.txHash, logIndex: ev.logIndex, ts,
                });
                withdraws++;
            }
        }
        return {deposits, withdraws, symbols};
    }

    // Interruptible sleep: resolves after `ms`, or early if #wakeNow() is called (a signal arrived, or stopping).
    #sleep(ms: number): Promise<void> {
        return new Promise((resolve) => {
            const timer = setTimeout(() => {
                this.#wakeResolver = null;
                resolve();
            }, ms);
            this.#wakeResolver = () => {
                clearTimeout(timer);
                this.#wakeResolver = null;
                resolve();
            };
        });
    }

    #wakeNow(): void {
        const resolver = this.#wakeResolver;
        this.#wakeResolver = null;
        if (resolver) resolver();
    }
}
