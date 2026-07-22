import ILogger from "../services/ILogger";
import IBlockchainCenter, {ICenterLog} from "../services/IBlockchainCenter";
import {
    BRIDGE_INDEXER_ABI, CENTER_NETWORK, Chain, CONFIRMED_DEPTH, ERC20_ABI, INDEXER_BLOCK_STEP,
    INetworkConfig, monitorCellKey, monitorChainMetaKey, oppositeChain, RedisKeys, symbolOfToken, TokenSymbol,
} from "../consts/Consts";
import BridgeReportRepository, {IAnomalyRow} from "../indexer/BridgeReportRepository";
import EventDecoder from "../indexer/EventDecoder";
import BlockTimeResolver from "../indexer/BlockTimeResolver";
import MonitorCache from "./MonitorCache";

// Per-chain meta, cached separately from the token cells because deposit/withdraw never changes it (only an
// operator toggling enable flags or the fee does), so a deposit shouldn't invalidate it.
interface IChainMeta {
    chainId: number;
    bridgeAddress: string;
    depositEnabled: boolean;
    withdrawEnabled: boolean;
    feePercent: string;
}

// One cell of the aggregate table (chain × token). Every wei value is a decimal string.
export interface ITokenCell {
    symbol: TokenSymbol;
    token: string;
    liquidity: string;
    totalDeposited: string;
    totalWithdrawn: string;
    collectedFees: string;
    sweptFees: string;
    totalFunded: string;
    // Same-chain accounting identity: liquidity must equal deposited − withdrawn + collectedFees + funded.
    solvency: {expected: string; actual: string; ok: boolean};
}

export interface IChainAggregate {
    chain: Chain;
    chainId: number;
    bridgeAddress: string;
    depositEnabled: boolean;
    withdrawEnabled: boolean;
    feePercent: string;
    tokens: ITokenCell[];
}

// Cross-chain invariant (§16.1, catches residual 4-10): withdrawn on a chain must not exceed the OPPOSITE
// chain's deposited for the same token. Same-chain solvency can't see this — the counters self-balance.
// Also carries the directional liability: what withdrawChain still OWES (deposited on the opposite chain that
// hasn't been withdrawn here yet) and whether its on-chain liquidity can cover that debt's net payout.
export interface ICrossChainCell {
    symbol: TokenSymbol;
    withdrawChain: Chain;
    depositChain: Chain;
    totalWithdrawn: string;
    oppositeDeposited: string;
    ok: boolean;              // invariant: withdrawn ≤ oppositeDeposited
    outstanding: string;      // debt still owed on withdrawChain = oppositeDeposited − withdrawn (gross, ≥ 0)
    requiredNet: string;      // tokens needed to service it = outstanding × (100 − fee) / 100
    liquidity: string;        // withdrawChain's ERC20 balance for this token
    liquidityOk: boolean;     // liquidity ≥ requiredNet
}

export interface IAggregateResponse {
    chains: IChainAggregate[];
    crossChain: ICrossChainCell[];
    fetchedAt: number; // epoch ms the on-chain snapshot was taken
    cached: boolean;   // true = served from the TTL cache, not read this request
}

export interface IHealthCell {
    chain: Chain;
    cursor: number;
    head: number;
    confirmedHead: number;
    lag: number;
}

export interface IHealthResponse {
    cells: IHealthCell[];
    fetchedAt: number;
    cached: boolean;
}

export interface IBackfillResult {
    walletsScanned: number;      // distinct wallets found in the CSV
    rowsUpserted: number;        // (wallet, token) counter rows written (non-zero counters only)
    ledgerTxProcessed: number;   // txs whose receipt was fetched + decoded into the ledger
    ledgerTxSkipped: number;     // txs already in the ledger — no receipt fetch (§18.3)
    ledgerEventsInserted: number;// individual ledger rows newly inserted
    ledgerTxFailed: number;      // receipt fetch/decode failures — not marked, retried on the next backfill
    gapFilled: number | null;
}

// Result of a server-side getLogs sweep of one gap's range.
export interface IGapSweepResult {
    gapId: number;
    chain: Chain;
    fromBlock: number;
    toBlock: number;
    deposits: number;
    withdraws: number;
}

// One Deposit/Withdraw tx pulled from the CSV, with the timestamp taken straight from the export (no block-time
// read needed for history).
interface ICsvTxRow {
    txHash: string;
    ts: number | null;
}

export interface IGapWithTime {
    id: number;
    chain: Chain;
    from_block: number;
    to_block: number;
    fromTime: number | null; // unix seconds; null if the block-timestamp read failed
    toTime: number | null;
}

// Live on-chain read layer for the monitor page: every number comes straight from the contract at confirmed
// depth, never from the report DB (which only backs the per-wallet / lifecycle / gaps / anomaly lists). This
// is a READ + backfill surface only — it never writes on-chain.
export default class MonitorService {
    readonly #logger: ILogger;
    readonly #center: IBlockchainCenter;
    readonly #configs: Map<Chain, INetworkConfig>;
    readonly #repo: BridgeReportRepository;
    readonly #cache: MonitorCache;
    readonly #blockTime: BlockTimeResolver;
    readonly #decoder = new EventDecoder();
    // In-process single-flight so a burst of polls/refreshes triggers ONE on-chain read per cell, not one per
    // request. Keyed by the cell/meta cache key.
    readonly #cellInflight = new Map<string, Promise<{data: ITokenCell; fetchedAt: number}>>();
    readonly #metaInflight = new Map<string, Promise<{data: IChainMeta; fetchedAt: number}>>();
    #healthInflight: Promise<{data: IHealthCell[]; fetchedAt: number}> | null = null;

    constructor(
        logger: ILogger, center: IBlockchainCenter, configs: Map<Chain, INetworkConfig>,
        repo: BridgeReportRepository, cache: MonitorCache, blockTime: BlockTimeResolver,
    ) {
        this.#logger = logger.clone('[Monitor]');
        this.#center = center;
        this.#configs = configs;
        this.#repo = repo;
        this.#cache = cache;
        this.#blockTime = blockTime;
    }

    // Assembles the table from the per-cell + per-chain-meta caches. `force` (the "Refresh" button) re-reads
    // every cell from chain; a plain poll serves each cell from cache and only pays RPC for the cells the sweep
    // invalidated. `cached` is true only when EVERY part was a cache hit; `fetchedAt` is the OLDEST part's
    // snapshot time, so the staleness label reflects the least-fresh cell on screen.
    async aggregate(force = false): Promise<IAggregateResponse> {
        const chains: IChainAggregate[] = [];
        const totals = new Map<string, {deposited: bigint; withdrawn: bigint}>();
        let allCached = true;
        let oldest = Date.now();

        for (const chain of Object.values(Chain)) {
            const meta = await this.#getChainMeta(chain, force);
            allCached &&= meta.cached;
            oldest = Math.min(oldest, meta.fetchedAt);

            const tokens: ITokenCell[] = [];
            for (const symbol of Object.values(TokenSymbol)) {
                const cell = await this.#getCell(chain, symbol, force);
                allCached &&= cell.cached;
                oldest = Math.min(oldest, cell.fetchedAt);
                tokens.push(cell.data);
                totals.set(`${chain}:${symbol}`, {
                    deposited: BigInt(cell.data.totalDeposited),
                    withdrawn: BigInt(cell.data.totalWithdrawn),
                });
            }

            chains.push({
                chain, chainId: meta.data.chainId, bridgeAddress: meta.data.bridgeAddress,
                depositEnabled: meta.data.depositEnabled, withdrawEnabled: meta.data.withdrawEnabled,
                feePercent: meta.data.feePercent, tokens,
            });
        }

        // Derived from the (possibly mixed-freshness) cell totals — no cache of its own, so invalidating one cell
        // is enough to make its cross-chain row current on the next read.
        const cellOf = (c: Chain, s: TokenSymbol) => chains.find((x) => x.chain === c)!.tokens.find((t) => t.symbol === s)!;
        const feeOf = (c: Chain) => BigInt(chains.find((x) => x.chain === c)!.feePercent);
        const HUNDRED = BigInt(100);
        const crossChain: ICrossChainCell[] = [];
        for (const symbol of Object.values(TokenSymbol)) {
            for (const chain of Object.values(Chain)) {
                const opp = oppositeChain(chain);
                const withdrawn = totals.get(`${chain}:${symbol}`)!.withdrawn;
                const oppositeDeposited = totals.get(`${opp}:${symbol}`)!.deposited;
                // Debt owed on `chain`: opposite-chain deposits not yet withdrawn here. Servicing it costs the net
                // (post-fee) amount — the fee stays in the contract balance — so compare that against liquidity.
                const outstanding = oppositeDeposited > withdrawn ? oppositeDeposited - withdrawn : BigInt(0);
                const requiredNet = (outstanding * (HUNDRED - feeOf(chain))) / HUNDRED;
                const liquidity = BigInt(cellOf(chain, symbol).liquidity);
                crossChain.push({
                    symbol, withdrawChain: chain, depositChain: opp,
                    totalWithdrawn: withdrawn.toString(),
                    oppositeDeposited: oppositeDeposited.toString(),
                    ok: withdrawn <= oppositeDeposited,
                    outstanding: outstanding.toString(),
                    requiredNet: requiredNet.toString(),
                    liquidity: liquidity.toString(),
                    liquidityOk: liquidity >= requiredNet,
                });
            }
        }

        return {chains, crossChain, fetchedAt: oldest, cached: allCached};
    }

    // One (chain, token) cell: cache hit unless forced/invalidated, else a single-flighted 6-RPC read.
    async #getCell(chain: Chain, symbol: TokenSymbol, force: boolean): Promise<{data: ITokenCell; fetchedAt: number; cached: boolean}> {
        const key = monitorCellKey(chain, symbol);
        if (!force) {
            const hit = await this.#cache.read<ITokenCell>(key);
            if (hit) return {data: hit.data, fetchedAt: hit.fetchedAt, cached: true};
        }
        const {data, fetchedAt} = await this.#loadCell(key, chain, symbol);
        return {data, fetchedAt, cached: false};
    }

    #loadCell(key: string, chain: Chain, symbol: TokenSymbol): Promise<{data: ITokenCell; fetchedAt: number}> {
        let inflight = this.#cellInflight.get(key);
        if (!inflight) {
            inflight = this.#computeCell(chain, symbol)
                .then(async (data) => {
                    const fetchedAt = Date.now();
                    await this.#cache.write(key, {data, fetchedAt});
                    return {data, fetchedAt};
                })
                .finally(() => this.#cellInflight.delete(key));
            this.#cellInflight.set(key, inflight);
        }
        return inflight;
    }

    async #computeCell(chain: Chain, symbol: TokenSymbol): Promise<ITokenCell> {
        const cfg = this.#config(chain);
        const network = CENTER_NETWORK[chain];
        const depth = CONFIRMED_DEPTH[chain];
        const bridge = cfg.bridgeAddress;
        const token = cfg.tokens[symbol];

        const totalDeposited = this.#uint(await this.#read(network, bridge, BRIDGE_INDEXER_ABI, 'totalDeposited', [token], depth));
        const totalWithdrawn = this.#uint(await this.#read(network, bridge, BRIDGE_INDEXER_ABI, 'totalWithdrawn', [token], depth));
        const collectedFees = this.#uint(await this.#read(network, bridge, BRIDGE_INDEXER_ABI, 'collectedFees', [token], depth));
        const sweptFees = this.#uint(await this.#read(network, bridge, BRIDGE_INDEXER_ABI, 'sweptFees', [token], depth));
        const totalFunded = this.#uint(await this.#read(network, bridge, BRIDGE_INDEXER_ABI, 'totalFunded', [token], depth));
        const liquidity = this.#uint(await this.#read(network, token, ERC20_ABI, 'balanceOf', [bridge], depth));

        const expected = totalDeposited - totalWithdrawn + collectedFees + totalFunded;
        return {
            symbol, token,
            liquidity: liquidity.toString(),
            totalDeposited: totalDeposited.toString(),
            totalWithdrawn: totalWithdrawn.toString(),
            collectedFees: collectedFees.toString(),
            sweptFees: sweptFees.toString(),
            totalFunded: totalFunded.toString(),
            solvency: {expected: expected.toString(), actual: liquidity.toString(), ok: liquidity === expected},
        };
    }

    // Per-chain meta (enable flags + fee): cached apart from the cells so a deposit never invalidates it.
    async #getChainMeta(chain: Chain, force: boolean): Promise<{data: IChainMeta; fetchedAt: number; cached: boolean}> {
        const key = monitorChainMetaKey(chain);
        if (!force) {
            const hit = await this.#cache.read<IChainMeta>(key);
            if (hit) return {data: hit.data, fetchedAt: hit.fetchedAt, cached: true};
        }
        const {data, fetchedAt} = await this.#loadChainMeta(key, chain);
        return {data, fetchedAt, cached: false};
    }

    #loadChainMeta(key: string, chain: Chain): Promise<{data: IChainMeta; fetchedAt: number}> {
        let inflight = this.#metaInflight.get(key);
        if (!inflight) {
            inflight = this.#computeChainMeta(chain)
                .then(async (data) => {
                    const fetchedAt = Date.now();
                    await this.#cache.write(key, {data, fetchedAt});
                    return {data, fetchedAt};
                })
                .finally(() => this.#metaInflight.delete(key));
            this.#metaInflight.set(key, inflight);
        }
        return inflight;
    }

    async #computeChainMeta(chain: Chain): Promise<IChainMeta> {
        const cfg = this.#config(chain);
        const network = CENTER_NETWORK[chain];
        const depth = CONFIRMED_DEPTH[chain];
        const bridge = cfg.bridgeAddress;

        const depositEnabled = this.#bool(await this.#read(network, bridge, BRIDGE_INDEXER_ABI, 'depositEnabled', [], depth));
        const withdrawEnabled = this.#bool(await this.#read(network, bridge, BRIDGE_INDEXER_ABI, 'withdrawEnabled', [], depth));
        const feePercent = this.#uint(await this.#read(network, bridge, BRIDGE_INDEXER_ABI, 'feePercent', [], depth));

        return {
            chainId: cfg.chainId, bridgeAddress: bridge,
            depositEnabled, withdrawEnabled, feePercent: feePercent.toString(),
        };
    }

    // Indexer progress per chain: how far the tail cursor lags the confirmed head (a large lag = the sweep
    // is behind, and untracked history sits in an open gap awaiting manual backfill). Cached like aggregate —
    // it hits an RPC (getLatestBlockNumber) per chain.
    async health(force = false): Promise<IHealthResponse> {
        if (!force) {
            const hit = await this.#cache.read<IHealthCell[]>(RedisKeys.MONITOR_INDEXER_STATUS);
            if (hit) return {cells: hit.data, fetchedAt: hit.fetchedAt, cached: true};
        }
        const {data, fetchedAt} = await this.#loadHealth();
        return {cells: data, fetchedAt, cached: false};
    }

    #loadHealth(): Promise<{data: IHealthCell[]; fetchedAt: number}> {
        if (!this.#healthInflight) {
            this.#healthInflight = this.#computeHealth()
                .then(async (data) => {
                    const fetchedAt = Date.now();
                    await this.#cache.write(RedisKeys.MONITOR_INDEXER_STATUS, {data, fetchedAt});
                    return {data, fetchedAt};
                })
                .finally(() => {
                    this.#healthInflight = null;
                });
        }
        return this.#healthInflight;
    }

    async #computeHealth(): Promise<IHealthCell[]> {
        const out: IHealthCell[] = [];
        for (const chain of Object.values(Chain)) {
            const network = CENTER_NETWORK[chain];
            const depth = CONFIRMED_DEPTH[chain];
            const cursor = await this.#repo.getCursor(chain);
            const head = await this.#center.getLatestBlockNumber(network);
            const confirmedHead = head - depth;
            out.push({chain, cursor, head, confirmedHead, lag: Math.max(0, confirmedHead - cursor)});
        }
        return out;
    }

    // Open gaps enriched with the DATE range of their block range, so an operator knows which dates to pick on
    // the explorer Export page. Timestamp reads are best-effort — a failure leaves the field null, not the list.
    async listGaps(): Promise<IGapWithTime[]> {
        const gaps = await this.#repo.listOpenGaps();
        const out: IGapWithTime[] = [];
        for (const g of gaps) {
            const network = CENTER_NETWORK[g.chain];
            out.push({
                ...g,
                fromTime: await this.#blockTime.resolve(network, g.from_block),
                toTime: await this.#blockTime.resolve(network, g.to_block),
            });
        }
        return out;
    }

    // Backfill a gap from a bscscan/polygonscan *Transactions* CSV export (picked by date range — which
    // sidesteps the explorer's block-range export cap). The CSV carries no amounts, so we take the wallets that
    // did a Deposit/Withdraw in that window and targeted-read their CURRENT on-chain counters: the counters are
    // monotonic, so the live value already catches the wallet fully up, and the GREATEST upsert keeps the max.
    // On success the gap row is marked filled.
    async ingestBackfill(chain: Chain, csvText: string, gapId?: number): Promise<IBackfillResult> {
        const cfg = this.#config(chain);
        const network = CENTER_NETWORK[chain];
        const depth = CONFIRMED_DEPTH[chain];
        const wallets = this.#extractWallets(csvText);
        const head = Math.max(0, (await this.#center.getLatestBlockNumber(network)) - depth);

        // Itemized ledger (§18): fetch each Deposit/Withdraw tx's receipt and record every event as its own row,
        // giving a real per-user timeline. Runs first so an operator's counter summary and the timeline agree.
        const ledger = await this.#backfillLedger(chain, cfg.bridgeAddress, network, csvText);

        // Counter summary (stopgap §18.6): live targeted-read of each wallet's CURRENT counters. Kept as a fast
        // total alongside the ledger; monotonic, so the live value already catches the wallet fully up.
        let rowsUpserted = 0;
        for (const wallet of wallets) {
            for (const symbol of Object.values(TokenSymbol)) {
                const token = cfg.tokens[symbol];
                const deposited = this.#uint(await this.#read(network, cfg.bridgeAddress, BRIDGE_INDEXER_ABI, 'deposited', [wallet, token], depth));
                const withdrawn = this.#uint(await this.#read(network, cfg.bridgeAddress, BRIDGE_INDEXER_ABI, 'withdrawn', [wallet, token], depth));
                if (deposited === 0n && withdrawn === 0n) continue; // wallet never touched this token — no row
                if (deposited > 0n) await this.#repo.upsertDeposited(chain, token, symbol, wallet, deposited, head);
                if (withdrawn > 0n) await this.#repo.upsertWithdrawn(chain, token, symbol, wallet, withdrawn, head);
                rowsUpserted++;
            }
        }

        let gapFilled: number | null = null;
        if (gapId !== undefined && Number.isFinite(gapId)) {
            await this.#repo.markGapFilled(gapId);
            gapFilled = gapId;
        }
        this.#logger.info(
            `backfill ${chain}: wallets=${wallets.length} rows=${rowsUpserted} ` +
            `ledger[processed=${ledger.ledgerTxProcessed} skipped=${ledger.ledgerTxSkipped} ` +
            `events=${ledger.ledgerEventsInserted} failed=${ledger.ledgerTxFailed}] gap=${gapFilled}`,
        );
        return {walletsScanned: wallets.length, rowsUpserted, ...ledger, gapFilled};
    }

    // Server-side auto-fill: getLogs the gap's block range in chunks and run the same decode+upsert the forward
    // tail does. For SMALL gaps (restart/downtime windows) — a huge historical range is still better via CSV
    // export (getLogs over millions of blocks is exactly what the tail avoids). Marks the gap filled on success.
    async sweepGap(gapId: number): Promise<IGapSweepResult> {
        const gap = await this.#repo.getGapById(gapId);
        if (!gap) throw new Error(`gap #${gapId} not found`);
        if (gap.status !== "open") throw new Error(`gap #${gapId} already ${gap.status}`);

        const cfg = this.#config(gap.chain);
        const network = CENTER_NETWORK[gap.chain];
        let deposits = 0;
        let withdraws = 0;
        for (let from = gap.from_block; from <= gap.to_block; from += INDEXER_BLOCK_STEP + 1) {
            const to = Math.min(from + INDEXER_BLOCK_STEP, gap.to_block);
            const r = await this.#sweepRange(gap.chain, cfg, network, from, to);
            deposits += r.deposits;
            withdraws += r.withdraws;
        }
        await this.#repo.markGapFilled(gapId);
        this.#logger.info(
            `gap #${gapId} swept ${gap.chain} [${gap.from_block}..${gap.to_block}] deposits=${deposits} withdraws=${withdraws}`,
        );
        return {gapId, chain: gap.chain, fromBlock: gap.from_block, toBlock: gap.to_block, deposits, withdraws};
    }

    // Close a gap without scanning — for a range known to hold nothing (e.g. a solo-test window). Report-DB only.
    async discardGap(gapId: number): Promise<{gapId: number; chain: Chain}> {
        const gap = await this.#repo.getGapById(gapId);
        if (!gap) throw new Error(`gap #${gapId} not found`);
        if (gap.status !== "open") throw new Error(`gap #${gapId} already ${gap.status}`);
        await this.#repo.markGapDiscarded(gapId);
        this.#logger.info(`gap #${gapId} discarded ${gap.chain} [${gap.from_block}..${gap.to_block}]`);
        return {gapId, chain: gap.chain};
    }

    // One getLogs chunk of a gap -> decode -> idempotent upsert. Mirrors the forward tail's processRange; the
    // (chain, tx_hash, log_index) uniqueness makes re-sweeping an already-covered range harmless.
    async #sweepRange(
        chain: Chain, cfg: INetworkConfig, network: string, fromBlock: number, toBlock: number,
    ): Promise<{deposits: number; withdraws: number}> {
        const logs = await this.#center.getLogs(network, cfg.bridgeAddress, this.#decoder.eventTopics(), fromBlock, toBlock);
        let deposits = 0;
        let withdraws = 0;
        for (const log of logs) {
            const ev = this.#decoder.decode(log);
            if (!ev) continue;
            const symbol = symbolOfToken(cfg, ev.token);
            if (!symbol) continue;
            const ts = await this.#blockTime.resolve(network, ev.block);
            if (ev.kind === "Deposit") {
                await this.#repo.upsertDeposited(chain, ev.token, symbol, ev.user, ev.depositedTotal, ev.block);
                await this.#repo.insertLedgerEvent({
                    chain, token: ev.token, symbol, wallet: ev.user, kind: "deposit",
                    amount: ev.amount, net: null, cumulative: ev.depositedTotal,
                    block: ev.block, txHash: ev.txHash, logIndex: ev.logIndex, ts,
                });
                deposits++;
            } else {
                await this.#repo.upsertWithdrawn(chain, ev.token, symbol, ev.user, ev.withdrawnTotal, ev.block);
                await this.#repo.insertLedgerEvent({
                    chain, token: ev.token, symbol, wallet: ev.user, kind: "withdraw",
                    amount: ev.amount, net: ev.net, cumulative: ev.withdrawnTotal,
                    block: ev.block, txHash: ev.txHash, logIndex: ev.logIndex, ts,
                });
                withdraws++;
            }
        }
        return {deposits, withdraws};
    }

    // Re-evaluate an anomaly against CURRENT on-chain state (the "Kiểm tra lại" button). If the condition no
    // longer holds it's marked resolved and drops off the alert list — the system verifies, the operator never
    // has to guess whether it's safe to dismiss.
    async recheckAnomaly(anomalyId: number): Promise<{id: number; resolved: boolean; detail: object}> {
        const a = await this.#repo.getAnomalyById(anomalyId);
        if (!a) throw new Error(`anomaly #${anomalyId} not found`);
        if (a.status !== "open") return {id: anomalyId, resolved: true, detail: {}};

        const {breached, detail} = await this.#evalAnomaly(a);
        if (!breached) {
            await this.#repo.resolveAnomaly(anomalyId);
            this.#logger.info(`anomaly #${anomalyId} (${a.kind}) rechecked -> resolved`);
        }
        return {id: anomalyId, resolved: !breached, detail};
    }

    // Live on-chain re-evaluation of one anomaly's condition. `breached` true = still a real anomaly.
    async #evalAnomaly(a: IAnomalyRow & {status: string}): Promise<{breached: boolean; detail: object}> {
        const sym = (a.detail as {symbol?: TokenSymbol})?.symbol;
        if (!sym) return {breached: true, detail: (a.detail ?? {}) as object}; // can't verify → leave open

        if (a.kind === "PER_USER_OVER_WITHDRAW" && a.chain && a.wallet) {
            const opp = oppositeChain(a.chain);
            const cfg = this.#config(a.chain);
            const oppCfg = this.#config(opp);
            const withdrawn = this.#uint(await this.#read(CENTER_NETWORK[a.chain], cfg.bridgeAddress, BRIDGE_INDEXER_ABI, 'withdrawn', [a.wallet, cfg.tokens[sym]], CONFIRMED_DEPTH[a.chain]));
            const oppDeposited = this.#uint(await this.#read(CENTER_NETWORK[opp], oppCfg.bridgeAddress, BRIDGE_INDEXER_ABI, 'deposited', [a.wallet, oppCfg.tokens[sym]], CONFIRMED_DEPTH[opp]));
            return {breached: withdrawn > oppDeposited, detail: {symbol: sym, withdrawn: withdrawn.toString(), oppositeDeposited: oppDeposited.toString()}};
        }
        if (a.kind === "CROSS_CHAIN_INSOLVENT" && a.chain) {
            const opp = oppositeChain(a.chain);
            const cfg = this.#config(a.chain);
            const oppCfg = this.#config(opp);
            const totalWithdrawn = this.#uint(await this.#read(CENTER_NETWORK[a.chain], cfg.bridgeAddress, BRIDGE_INDEXER_ABI, 'totalWithdrawn', [cfg.tokens[sym]], CONFIRMED_DEPTH[a.chain]));
            const totalDepositedOpp = this.#uint(await this.#read(CENTER_NETWORK[opp], oppCfg.bridgeAddress, BRIDGE_INDEXER_ABI, 'totalDeposited', [oppCfg.tokens[sym]], CONFIRMED_DEPTH[opp]));
            return {breached: totalWithdrawn > totalDepositedOpp, detail: {symbol: sym, withdrawChain: a.chain, totalWithdrawn: totalWithdrawn.toString(), depositChain: opp, totalDepositedOpposite: totalDepositedOpp.toString()}};
        }
        if (a.kind === "CHECKSUM_MISMATCH" && a.chain) {
            const cfg = this.#config(a.chain);
            const token = cfg.tokens[sym];
            const dbSum = await this.#repo.sumDeposited(a.chain, token);
            const onchain = this.#uint(await this.#read(CENTER_NETWORK[a.chain], cfg.bridgeAddress, BRIDGE_INDEXER_ABI, 'totalDeposited', [token], CONFIRMED_DEPTH[a.chain]));
            return {breached: dbSum > onchain, detail: {symbol: sym, dbSumDeposited: dbSum.toString(), onchainTotalDeposited: onchain.toString()}};
        }
        return {breached: true, detail: (a.detail ?? {}) as object}; // unknown kind → leave open
    }

    // Ledger half of the backfill: one receipt per unique Deposit/Withdraw tx in the CSV. A tx already in the
    // ledger is skipped WITHOUT an RPC call (§18.3); a receipt/decode failure is counted and left unmarked so a
    // later re-upload retries it. `ts` comes straight from the CSV row — no block-time read for history.
    async #backfillLedger(
        chain: Chain, bridgeAddress: string, network: string, csvText: string,
    ): Promise<Pick<IBackfillResult, "ledgerTxProcessed" | "ledgerTxSkipped" | "ledgerEventsInserted" | "ledgerTxFailed">> {
        const bridge = bridgeAddress.toLowerCase();
        const cfg = this.#config(chain);
        const rows = this.#extractTxRows(csvText);
        this.#logger.info(`ledger backfill ${chain}: ${rows.length} candidate deposit/withdraw tx(s) from CSV`);

        let ledgerTxProcessed = 0;
        let ledgerTxSkipped = 0;
        let ledgerEventsInserted = 0;
        let nullReceipt = 0; // gateway returned success but result=null (RPC has no receipt for the tx)
        let fetchError = 0;  // the gateway call threw (HTTP error, timeout, …)
        let noEvent = 0;     // receipt fetched but no bridge Deposit/Withdraw log decoded from it

        for (const row of rows) {
            if (await this.#repo.ledgerHasTx(chain, row.txHash)) {
                ledgerTxSkipped++;
                continue;
            }
            let receipt: Awaited<ReturnType<IBlockchainCenter["getTransactionReceipt"]>>;
            try {
                receipt = await this.#center.getTransactionReceipt(network, row.txHash);
            } catch (e) {
                fetchError++;
                this.#logger.error(`ledger ${chain}: receipt fetch FAILED tx=${row.txHash}: ${e instanceof Error ? e.message : e}`);
                continue;
            }
            if (!receipt || !Array.isArray(receipt.logs)) {
                nullReceipt++;
                this.#logger.warn(`ledger ${chain}: gateway returned NULL receipt tx=${row.txHash} — RPC pool has no receipt for this (historical/pruned) block`);
                continue;
            }
            let insertedForTx = 0;
            for (const rlog of receipt.logs) {
                if ((rlog.address ?? "").toLowerCase() !== bridge) continue; // ignore other contracts' logs
                const clog: ICenterLog = {
                    blockNumber: rlog.blockNumber,
                    transactionHash: rlog.transactionHash ?? row.txHash,
                    logIndex: rlog.index ?? rlog.logIndex ?? 0,
                    topics: rlog.topics,
                    data: rlog.data,
                };
                const ev = this.#decoder.decode(clog);
                if (!ev) continue;
                const symbol = symbolOfToken(cfg, ev.token);
                if (!symbol) continue;
                const inserted = await this.#repo.insertLedgerEvent({
                    chain, token: ev.token, symbol, wallet: ev.user,
                    kind: ev.kind === "Deposit" ? "deposit" : "withdraw",
                    amount: ev.amount,
                    net: ev.kind === "Withdraw" ? ev.net : null,
                    cumulative: ev.kind === "Deposit" ? ev.depositedTotal : ev.withdrawnTotal,
                    block: ev.block, txHash: ev.txHash, logIndex: ev.logIndex, ts: row.ts,
                });
                if (inserted) {
                    ledgerEventsInserted++;
                    insertedForTx++;
                }
            }
            if (insertedForTx === 0) {
                noEvent++;
                this.#logger.warn(`ledger ${chain}: no NEW bridge Deposit/Withdraw event in tx=${row.txHash} (receipt logs=${receipt.logs.length}; already-indexed or non-bridge tx)`);
            }
            ledgerTxProcessed++;
        }

        if (nullReceipt || fetchError || noEvent) {
            this.#logger.warn(`ledger backfill ${chain} incomplete — nullReceipt=${nullReceipt} fetchError=${fetchError} noEvent=${noEvent} (see per-tx lines above)`);
        }
        return {ledgerTxProcessed, ledgerTxSkipped, ledgerEventsInserted, ledgerTxFailed: nullReceipt + fetchError};
    }

    // Unique wallets that did a Deposit/Withdraw, pulled from a bscscan Transactions CSV (every field quoted).
    #extractWallets(csvText: string): string[] {
        const lines = csvText.split(/\r?\n/).filter((l) => l.trim().length > 0);
        if (lines.length < 2) return [];
        const header = this.#splitCsvLine(lines[0]).map((h) => h.toLowerCase());
        const fromIdx = header.indexOf("from");
        const methodIdx = header.indexOf("method");
        if (fromIdx < 0 || methodIdx < 0) {
            throw new Error("backfill CSV: missing 'From'/'Method' columns (expected a bscscan Transactions export)");
        }
        const wallets = new Set<string>();
        for (let i = 1; i < lines.length; i++) {
            const cols = this.#splitCsvLine(lines[i]);
            const method = (cols[methodIdx] ?? "").toLowerCase();
            const from = (cols[fromIdx] ?? "").toLowerCase();
            if (from && /deposit|withdraw/.test(method)) wallets.add(from);
        }
        return [...wallets];
    }

    // Unique Deposit/Withdraw txs from the CSV, each with its timestamp taken from the export. Handles "Withdraw
    // To" (legacy relayer) rows too — the receipt's indexed `user` is the real beneficiary (§18.6). Prefers the
    // UnixTimestamp column; falls back to parsing "DateTime (UTC)" as UTC.
    #extractTxRows(csvText: string): ICsvTxRow[] {
        const lines = csvText.split(/\r?\n/).filter((l) => l.trim().length > 0);
        if (lines.length < 2) return [];
        const header = this.#splitCsvLine(lines[0]).map((h) => h.toLowerCase());
        const hashIdx = header.findIndex((h) => h.includes("transaction hash") || h === "txhash");
        const methodIdx = header.indexOf("method");
        const unixIdx = header.findIndex((h) => h.includes("unixtimestamp"));
        const dateIdx = header.findIndex((h) => h.includes("datetime"));
        if (hashIdx < 0 || methodIdx < 0) {
            throw new Error("backfill CSV: missing 'Transaction Hash'/'Method' columns (expected a bscscan Transactions export)");
        }
        const seen = new Set<string>();
        const rows: ICsvTxRow[] = [];
        for (let i = 1; i < lines.length; i++) {
            const cols = this.#splitCsvLine(lines[i]);
            const method = (cols[methodIdx] ?? "").toLowerCase();
            if (!/deposit|withdraw/.test(method)) continue;
            const txHash = (cols[hashIdx] ?? "").toLowerCase();
            if (!txHash.startsWith("0x") || seen.has(txHash)) continue;
            seen.add(txHash);
            rows.push({txHash, ts: this.#parseCsvTs(unixIdx >= 0 ? cols[unixIdx] : undefined, dateIdx >= 0 ? cols[dateIdx] : undefined)});
        }
        return rows;
    }

    #parseCsvTs(unix?: string, datetime?: string): number | null {
        if (unix && /^\d+$/.test(unix.trim())) return Number(unix.trim());
        if (datetime && datetime.trim()) {
            // bscscan renders UTC without a zone suffix ("2026-07-10 10:07:59") — pin it to UTC explicitly.
            const ms = Date.parse(`${datetime.trim().replace(" ", "T")}Z`);
            if (!Number.isNaN(ms)) return Math.floor(ms / 1000);
        }
        return null;
    }

    // bscscan quotes every field ("a","b",…); pull each quoted value in order.
    #splitCsvLine(line: string): string[] {
        const out: string[] = [];
        const re = /"([^"]*)"/g;
        let m: RegExpExecArray | null;
        while ((m = re.exec(line)) !== null) out.push(m[1]);
        return out;
    }

    #read(network: string, address: string, abi: readonly string[], method: string, args: any[], depth: number): Promise<any> {
        return this.#center.callContract(network, address, abi, method, args, depth);
    }

    #uint(raw: any): bigint {
        return BigInt(raw);
    }

    #bool(raw: unknown): boolean {
        return raw === true || raw === "true";
    }

    #config(chain: Chain): INetworkConfig {
        const cfg = this.#configs.get(chain);
        if (!cfg) throw new Error(`no network config for ${chain}`);
        return cfg;
    }
}
