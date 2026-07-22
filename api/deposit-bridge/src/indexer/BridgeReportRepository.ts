import IDatabase from "../services/IDatabase";
import {Chain, TokenSymbol} from "../consts/Consts";

export type LifecycleStatus = "signed" | "executed" | "expired";

export interface IPendingLifecycle {
    id: number;
    chain: Chain;
    token: string;
    wallet: string;
    other_deposited: string; // wei
    deadline: number;
}

export interface IOverWithdrawRow {
    chain: Chain;
    symbol: TokenSymbol;
    wallet: string;
    withdrawn: string;           // wei
    opposite_deposited: string;  // wei
}

export interface IGapRow {
    id: number;
    chain: Chain;
    from_block: number;
    to_block: number;
}

// Read-only rows for the monitor REST layer (§16). Wei stays a string end-to-end — never a JS number.
export interface IWalletStateRow {
    chain: Chain;
    token: string;
    symbol: TokenSymbol;
    wallet: string;
    deposited: string;  // wei
    withdrawn: string;  // wei
    updated_block: number;
    updated_at: string;
}

export interface ILifecycleRow {
    id: number;
    chain: Chain;
    token: string;
    symbol: TokenSymbol;
    wallet: string;
    other_deposited: string; // wei
    deadline: number;
    status: LifecycleStatus;
    tx_block: number | null;
    signed_at: string;
    resolved_at: string | null;
}

export interface IAnomalyRow {
    id: number;
    kind: string;
    chain: Chain | null;
    token: string | null;
    wallet: string | null;
    detail: unknown;
    created_at: string;
}

export type LedgerKind = "deposit" | "withdraw";

// One itemized event (§18). Wei stays a string; `net` is null for deposits, `ts` null if unresolved.
export interface ILedgerEvent {
    chain: Chain;
    token: string;
    symbol: TokenSymbol;
    wallet: string;
    kind: LedgerKind;
    amount: bigint;
    net: bigint | null;
    cumulative: bigint;
    block: number;
    txHash: string;
    logIndex: number;
    ts: number | null;
}

export interface ILedgerRow {
    chain: Chain;
    token: string;
    symbol: TokenSymbol;
    wallet: string;
    kind: LedgerKind;
    amount: string;          // wei
    net: string | null;      // wei; null for deposit
    cumulative: string;      // wei
    block: number;
    tx_hash: string;
    log_index: number;
    ts: number | null;       // block unix seconds
}

// Optional filters for the global transaction feed (listLedger). Any combination; omit for "all".
export interface ILedgerListFilter {
    chain?: Chain;
    symbol?: TokenSymbol;
    kind?: LedgerKind;
}

// All report SQL. numeric(78,0)/bigint come back from pg as strings — kept as strings/bigints, never coerced
// to JS number (a uint256 would lose precision). Nothing here is on the signing path.
export default class BridgeReportRepository {
    readonly #db: IDatabase;

    constructor(db: IDatabase) {
        this.#db = db;
    }

    // ── Tail cursor ─────────────────────────────────────────────────────────────────────────────────
    // null when the chain has never been seen (distinct from block 0), so bootstrap can tell first run apart.
    async getCursorRaw(chain: Chain): Promise<number | null> {
        const {rows} = await this.#db.query<{block_number: string}>(
            `SELECT block_number FROM bridge_indexer_cursor WHERE chain = $1`, [chain],
        );
        return rows.length ? Number(rows[0].block_number) : null;
    }

    async getCursor(chain: Chain): Promise<number> {
        return (await this.getCursorRaw(chain)) ?? 0;
    }

    // Jump the cursor forward to the current head on startup (GREATEST guards against ever rewinding).
    async jumpCursor(chain: Chain, block: number): Promise<void> {
        await this.#db.query(
            `INSERT INTO bridge_indexer_cursor (chain, block_number) VALUES ($1, $2)
             ON CONFLICT (chain) DO UPDATE SET block_number = GREATEST(bridge_indexer_cursor.block_number, EXCLUDED.block_number)`,
            [chain, block],
        );
    }

    async setCursor(chain: Chain, block: number): Promise<void> {
        await this.#db.query(
            `UPDATE bridge_indexer_cursor SET block_number = $2 WHERE chain = $1 AND block_number < $2`,
            [chain, block],
        );
    }

    // ── Backfill gaps (ranges the tail never covered) ─────────────────────────────────────────────────
    async insertGap(chain: Chain, fromBlock: number, toBlock: number): Promise<void> {
        await this.#db.query(
            `INSERT INTO bridge_indexer_gap (chain, from_block, to_block) VALUES ($1, $2, $3)
             ON CONFLICT (chain, from_block, to_block) DO NOTHING`,
            [chain, fromBlock, toBlock],
        );
    }

    async listOpenGaps(): Promise<IGapRow[]> {
        const {rows} = await this.#db.query<IGapRow>(
            `SELECT id, chain, from_block, to_block FROM bridge_indexer_gap
             WHERE status = 'open' ORDER BY chain, from_block`,
        );
        return rows.map((r) => ({...r, id: Number(r.id), from_block: Number(r.from_block), to_block: Number(r.to_block)}));
    }

    async markGapFilled(id: number): Promise<void> {
        await this.#db.query(
            `UPDATE bridge_indexer_gap SET status = 'filled', filled_at = now() WHERE id = $1`, [id],
        );
    }

    // Full row incl. status — the on-demand gap actions (sweep / discard) check current state before acting.
    async getGapById(id: number): Promise<(IGapRow & {status: string}) | null> {
        const {rows} = await this.#db.query<IGapRow & {status: string}>(
            `SELECT id, chain, from_block, to_block, status FROM bridge_indexer_gap WHERE id = $1`, [id],
        );
        if (!rows.length) return null;
        const r = rows[0];
        return {...r, id: Number(r.id), from_block: Number(r.from_block), to_block: Number(r.to_block)};
    }

    // Close a gap WITHOUT scanning — the range is known to hold nothing worth indexing (e.g. a solo-test window).
    async markGapDiscarded(id: number): Promise<void> {
        await this.#db.query(
            `UPDATE bridge_indexer_gap SET status = 'discarded', filled_at = now() WHERE id = $1`, [id],
        );
    }

    // ── Wallet state (counters are monotonic → GREATEST keeps out-of-order logs safe) ─────────────────
    async upsertDeposited(
        chain: Chain, token: string, symbol: TokenSymbol, wallet: string, deposited: bigint, block: number,
    ): Promise<void> {
        await this.#db.query(
            `INSERT INTO bridge_wallet_state (chain, token, symbol, wallet, deposited, updated_block)
             VALUES ($1, $2, $3, $4, $5, $6)
             ON CONFLICT (chain, token, wallet) DO UPDATE SET
                deposited = GREATEST(bridge_wallet_state.deposited, EXCLUDED.deposited),
                updated_block = GREATEST(bridge_wallet_state.updated_block, EXCLUDED.updated_block),
                updated_at = now()`,
            [chain, token.toLowerCase(), symbol, wallet.toLowerCase(), deposited.toString(), block],
        );
    }

    async upsertWithdrawn(
        chain: Chain, token: string, symbol: TokenSymbol, wallet: string, withdrawn: bigint, block: number,
    ): Promise<void> {
        await this.#db.query(
            `INSERT INTO bridge_wallet_state (chain, token, symbol, wallet, withdrawn, updated_block)
             VALUES ($1, $2, $3, $4, $5, $6)
             ON CONFLICT (chain, token, wallet) DO UPDATE SET
                withdrawn = GREATEST(bridge_wallet_state.withdrawn, EXCLUDED.withdrawn),
                updated_block = GREATEST(bridge_wallet_state.updated_block, EXCLUDED.updated_block),
                updated_at = now()`,
            [chain, token.toLowerCase(), symbol, wallet.toLowerCase(), withdrawn.toString(), block],
        );
    }

    async sumDeposited(chain: Chain, token: string): Promise<bigint> {
        const {rows} = await this.#db.query<{s: string | null}>(
            `SELECT COALESCE(SUM(deposited), 0)::text AS s FROM bridge_wallet_state WHERE chain = $1 AND token = $2`,
            [chain, token.toLowerCase()],
        );
        return BigInt(rows[0]?.s ?? "0");
    }

    // ── Withdraw lifecycle ──────────────────────────────────────────────────────────────────────────
    async insertLifecycleSigned(
        chain: Chain, token: string, symbol: TokenSymbol, wallet: string, otherDeposited: bigint, deadline: number,
    ): Promise<void> {
        await this.#db.query(
            `INSERT INTO bridge_withdraw_lifecycle (chain, token, symbol, wallet, other_deposited, deadline)
             VALUES ($1, $2, $3, $4, $5, $6)`,
            [chain, token.toLowerCase(), symbol, wallet.toLowerCase(), otherDeposited.toString(), deadline],
        );
    }

    async pendingLifecycle(): Promise<IPendingLifecycle[]> {
        const {rows} = await this.#db.query<IPendingLifecycle>(
            `SELECT id, chain, token, wallet, other_deposited::text AS other_deposited, deadline
             FROM bridge_withdraw_lifecycle WHERE status = 'signed' ORDER BY id ASC LIMIT 200`,
        );
        return rows.map((r) => ({...r, id: Number(r.id), deadline: Number(r.deadline)}));
    }

    async markLifecycle(id: number, status: LifecycleStatus, txBlock?: number): Promise<void> {
        await this.#db.query(
            `UPDATE bridge_withdraw_lifecycle SET status = $2, tx_block = $3, resolved_at = now() WHERE id = $1`,
            [id, status, txBlock ?? null],
        );
    }

    // ── Anomaly detection ───────────────────────────────────────────────────────────────────────────
    // Cross-chain per-user breach: withdrawn on a chain exceeds deposited on the OPPOSITE chain (same token).
    async perUserOverWithdrawn(): Promise<IOverWithdrawRow[]> {
        const {rows} = await this.#db.query<IOverWithdrawRow>(
            `SELECT w.chain, w.symbol, w.wallet, w.withdrawn::text AS withdrawn,
                    COALESCE(d.deposited, 0)::text AS opposite_deposited
             FROM bridge_wallet_state w
             LEFT JOIN bridge_wallet_state d
                    ON d.wallet = w.wallet AND d.symbol = w.symbol AND d.chain <> w.chain
             WHERE w.withdrawn > 0 AND w.withdrawn > COALESCE(d.deposited, 0)`,
        );
        return rows;
    }

    // Dedupe: skip if an OPEN anomaly with the same identity (kind, chain, token, wallet) already exists, so a
    // persistent condition doesn't spam a new row every tick. Returns true only when a row was actually inserted.
    async insertAnomaly(
        kind: string, detail: object, chain?: Chain, token?: string, wallet?: string,
    ): Promise<boolean> {
        const {rowCount} = await this.#db.query(
            `INSERT INTO bridge_anomaly (kind, chain, token, wallet, detail)
             SELECT $1, $2, $3, $4, $5::jsonb
             WHERE NOT EXISTS (
                 SELECT 1 FROM bridge_anomaly
                 WHERE status = 'open' AND kind = $1
                   AND chain IS NOT DISTINCT FROM $2
                   AND token IS NOT DISTINCT FROM $3
                   AND wallet IS NOT DISTINCT FROM $4
             )`,
            [kind, chain ?? null, token?.toLowerCase() ?? null, wallet?.toLowerCase() ?? null, JSON.stringify(detail)],
        );
        return (rowCount ?? 0) > 0;
    }

    async getAnomalyById(id: number): Promise<(IAnomalyRow & {status: string}) | null> {
        const {rows} = await this.#db.query<IAnomalyRow & {status: string}>(
            `SELECT id, kind, chain, token, wallet, detail, status, created_at FROM bridge_anomaly WHERE id = $1`,
            [id],
        );
        if (!rows.length) return null;
        return {...rows[0], id: Number(rows[0].id)};
    }

    async resolveAnomaly(id: number): Promise<void> {
        await this.#db.query(
            `UPDATE bridge_anomaly SET status = 'resolved', resolved_at = now() WHERE id = $1`, [id],
        );
    }

    // ── Monitor read surface (REST — §16, no signing-path coupling) ───────────────────────────────────
    async listWallets(chain?: Chain, symbol?: TokenSymbol, limit = 1000): Promise<IWalletStateRow[]> {
        const conds: string[] = [];
        const params: any[] = [];
        if (chain) {
            params.push(chain);
            conds.push(`chain = $${params.length}`);
        }
        if (symbol) {
            params.push(symbol);
            conds.push(`symbol = $${params.length}`);
        }
        const where = conds.length ? `WHERE ${conds.join(' AND ')}` : '';
        params.push(limit);
        const {rows} = await this.#db.query<IWalletStateRow>(
            `SELECT chain, token, symbol, wallet, deposited::text AS deposited, withdrawn::text AS withdrawn,
                    updated_block, updated_at
             FROM bridge_wallet_state ${where} ORDER BY updated_at DESC LIMIT $${params.length}`,
            params,
        );
        return rows.map((r) => ({...r, updated_block: Number(r.updated_block)}));
    }

    async getWallet(wallet: string): Promise<IWalletStateRow[]> {
        const {rows} = await this.#db.query<IWalletStateRow>(
            `SELECT chain, token, symbol, wallet, deposited::text AS deposited, withdrawn::text AS withdrawn,
                    updated_block, updated_at
             FROM bridge_wallet_state WHERE wallet = $1 ORDER BY chain, symbol`,
            [wallet.toLowerCase()],
        );
        return rows.map((r) => ({...r, updated_block: Number(r.updated_block)}));
    }

    async listLifecycle(status?: LifecycleStatus, limit = 200): Promise<ILifecycleRow[]> {
        const params: any[] = [];
        let where = '';
        if (status) {
            params.push(status);
            where = `WHERE status = $1`;
        }
        params.push(limit);
        const {rows} = await this.#db.query<ILifecycleRow>(
            `SELECT id, chain, token, symbol, wallet, other_deposited::text AS other_deposited, deadline,
                    status, tx_block, signed_at, resolved_at
             FROM bridge_withdraw_lifecycle ${where} ORDER BY id DESC LIMIT $${params.length}`,
            params,
        );
        return rows.map((r) => ({
            ...r, id: Number(r.id), deadline: Number(r.deadline),
            tx_block: r.tx_block === null ? null : Number(r.tx_block),
        }));
    }

    // Open (unresolved) anomalies only — a live recheck flips resolved ones to 'resolved' so they drop off here.
    async listAnomalies(limit = 200): Promise<IAnomalyRow[]> {
        const {rows} = await this.#db.query<IAnomalyRow>(
            `SELECT id, kind, chain, token, wallet, detail, created_at
             FROM bridge_anomaly WHERE status = 'open' ORDER BY created_at DESC LIMIT $1`,
            [limit],
        );
        return rows.map((r) => ({...r, id: Number(r.id)}));
    }

    // ── Itemized event ledger (§18) ───────────────────────────────────────────────────────────────────
    // Idempotent by (chain, tx_hash, log_index): the forward sweep and a CSV backfill can both touch the same
    // event without duplicating it. Returns true if a row was actually inserted (false = already present).
    async insertLedgerEvent(ev: ILedgerEvent): Promise<boolean> {
        const {rowCount} = await this.#db.query(
            `INSERT INTO bridge_event_ledger
                (chain, token, symbol, wallet, kind, amount, net, cumulative, block, tx_hash, log_index, ts)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
             ON CONFLICT (chain, tx_hash, log_index) DO NOTHING`,
            [
                ev.chain, ev.token.toLowerCase(), ev.symbol, ev.wallet.toLowerCase(), ev.kind,
                ev.amount.toString(), ev.net === null ? null : ev.net.toString(), ev.cumulative.toString(),
                ev.block, ev.txHash.toLowerCase(), ev.logIndex, ev.ts,
            ],
        );
        return (rowCount ?? 0) > 0;
    }

    // Early skip for the CSV backfill (§18.3): true if any event of this tx is already recorded, so its receipt
    // is never re-fetched. A tx that failed mid-backfill left nothing, so it retries next time.
    async ledgerHasTx(chain: Chain, txHash: string): Promise<boolean> {
        const {rows} = await this.#db.query(
            `SELECT 1 FROM bridge_event_ledger WHERE chain = $1 AND tx_hash = $2 LIMIT 1`,
            [chain, txHash.toLowerCase()],
        );
        return rows.length > 0;
    }

    // Per-wallet timeline across BOTH chains, OLDEST FIRST by block timestamp (the only cross-network-comparable
    // ordering — block numbers from two chains are not). `block` is only a tiebreaker WITHIN the same chain, for
    // the rare same-second ties; rows with an unresolved timestamp (null ts) sort last.
    async getLedger(wallet: string, limit = 1000): Promise<ILedgerRow[]> {
        const {rows} = await this.#db.query<ILedgerRow>(
            `SELECT chain, token, symbol, wallet, kind, amount::text AS amount, net::text AS net,
                    cumulative::text AS cumulative, block, tx_hash, log_index, ts
             FROM bridge_event_ledger WHERE wallet = $1
             ORDER BY ts ASC NULLS LAST, chain ASC, block ASC, log_index ASC LIMIT $2`,
            [wallet.toLowerCase(), limit],
        );
        return rows.map((r) => ({
            ...r, block: Number(r.block), log_index: Number(r.log_index),
            ts: r.ts === null ? null : Number(r.ts),
        }));
    }

    // Global transaction feed, newest first, with optional chain/symbol/kind filters and offset pagination.
    // Returns the page rows plus the total matching count so the page can render "trang X / Y". Ordered by ts
    // then block+log_index so txs with an unresolved timestamp still sort deterministically at the bottom.
    async listLedger(filter: ILedgerListFilter, limit: number, offset: number): Promise<{rows: ILedgerRow[]; total: number}> {
        const conds: string[] = [];
        const params: unknown[] = [];
        if (filter.chain) { params.push(filter.chain); conds.push(`chain = $${params.length}`); }
        if (filter.symbol) { params.push(filter.symbol); conds.push(`symbol = $${params.length}`); }
        if (filter.kind) { params.push(filter.kind); conds.push(`kind = $${params.length}`); }
        const where = conds.length ? `WHERE ${conds.join(' AND ')}` : '';

        const countRes = await this.#db.query<{c: string}>(
            `SELECT COUNT(*)::text AS c FROM bridge_event_ledger ${where}`, params,
        );
        const total = Number(countRes.rows[0]?.c ?? 0);

        const {rows} = await this.#db.query<ILedgerRow>(
            `SELECT chain, token, symbol, wallet, kind, amount::text AS amount, net::text AS net,
                    cumulative::text AS cumulative, block, tx_hash, log_index, ts
             FROM bridge_event_ledger ${where}
             ORDER BY ts DESC NULLS LAST, block DESC, log_index DESC
             LIMIT $${params.length + 1} OFFSET $${params.length + 2}`,
            [...params, limit, offset],
        );
        return {
            rows: rows.map((r) => ({
                ...r, block: Number(r.block), log_index: Number(r.log_index),
                ts: r.ts === null ? null : Number(r.ts),
            })),
            total,
        };
    }
}
