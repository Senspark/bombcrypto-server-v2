import {Request, Response, Router} from "express";
import ILogger from "../services/ILogger";
import {Chain, TokenSymbol} from "../consts/Consts";
import BridgeReportRepository, {LedgerKind, LifecycleStatus} from "../indexer/BridgeReportRepository";
import MonitorService from "./MonitorService";

// Read-only monitor REST (§16). No auth: the page is served/reached over localhost only (user decision),
// and nothing here can write on-chain — the aggregate is a live contract read, the rest are report-DB reads,
// and /backfill only upserts the report DB from an operator-supplied explorer export.
export default function buildMonitorRouter(
    logger: ILogger, repo: BridgeReportRepository, service: MonitorService,
    buildInfo: {version: string; code: string},
): Router {
    const log = logger.clone('[MonitorApi]');
    const router = Router();

    // Build identity of the running image (§ops), shown on the page. `version` = deploy label; `code` = a hash
    // of the running bytes — an unchanged `code` after a "rebuild" flags that the new code did NOT take effect.
    // Reachable through the page's /monitor proxy.
    router.get('/version', (_req, res) => {
        res.json(buildInfo);
    });

    const wrap = (fn: (req: Request, res: Response) => Promise<unknown>) =>
        (req: Request, res: Response): void => {
            fn(req, res).catch((e) => {
                log.error(e);
                res.status(500).json({error: "internal error"});
            });
        };

    router.get('/aggregate', wrap(async (req, res) => {
        res.json(await service.aggregate(isForced(req.query.refresh)));
    }));

    router.get('/health', wrap(async (req, res) => {
        res.json(await service.health(isForced(req.query.refresh)));
    }));

    router.get('/gaps', wrap(async (_req, res) => {
        res.json(await service.listGaps());
    }));

    router.get('/anomalies', wrap(async (req, res) => {
        res.json(await repo.listAnomalies(clampLimit(req.query.limit)));
    }));

    // Re-evaluate one anomaly against current on-chain state; resolves it if the condition is gone.
    router.post('/anomalies/:id/recheck', wrap(async (req, res) => {
        const id = Number(req.params.id);
        if (!Number.isFinite(id)) {
            res.status(400).json({error: "bad anomaly id"});
            return;
        }
        res.json(await service.recheckAnomaly(id));
    }));

    router.get('/lifecycle', wrap(async (req, res) => {
        res.json(await repo.listLifecycle(parseStatus(req.query.status), clampLimit(req.query.limit)));
    }));

    router.get('/wallets', wrap(async (req, res) => {
        res.json(await repo.listWallets(parseChain(req.query.chain), parseSymbol(req.query.token), clampLimit(req.query.limit, 1000)));
    }));

    router.get('/wallet/:wallet', wrap(async (req, res) => {
        res.json(await repo.getWallet(req.params.wallet));
    }));

    // Itemized per-user timeline across both chains (§18) — kept separate from the counter endpoint above.
    router.get('/wallet/:wallet/ledger', wrap(async (req, res) => {
        res.json(await repo.getLedger(req.params.wallet, clampLimit(req.query.limit, 1000)));
    }));

    // Global transaction feed, newest first, paginated + filterable by chain / token / kind (deposit|withdraw).
    router.get('/transactions', wrap(async (req, res) => {
        const pageSize = clampPageSize(req.query.pageSize);
        const page = clampPage(req.query.page);
        const filter = {
            chain: parseChain(req.query.chain),
            symbol: parseSymbol(req.query.token),
            kind: parseKind(req.query.kind),
        };
        const {rows, total} = await repo.listLedger(filter, pageSize, (page - 1) * pageSize);
        res.json({rows, total, page, pageSize});
    }));

    router.post('/backfill', wrap(async (req, res) => {
        const chain = parseChain(req.body?.chain);
        if (!chain) {
            res.status(400).json({error: "chain required (BSC|POLYGON)"});
            return;
        }
        const csv = req.body?.csv;
        if (typeof csv !== "string" || !csv.trim()) {
            res.status(400).json({error: "csv required (bscscan Transactions export text)"});
            return;
        }
        const gapId = req.body?.gapId !== undefined ? Number(req.body.gapId) : undefined;
        res.json(await service.ingestBackfill(chain, csv, gapId));
    }));

    // Auto-fill a gap server-side via getLogs (small ranges) — no CSV needed. READ + report-DB upsert only.
    router.post('/gaps/:id/sweep', wrap(async (req, res) => {
        const id = Number(req.params.id);
        if (!Number.isFinite(id)) {
            res.status(400).json({error: "bad gap id"});
            return;
        }
        res.json(await service.sweepGap(id));
    }));

    // Mark a gap done without scanning (range known empty). Report-DB state change only.
    router.post('/gaps/:id/discard', wrap(async (req, res) => {
        const id = Number(req.params.id);
        if (!Number.isFinite(id)) {
            res.status(400).json({error: "bad gap id"});
            return;
        }
        res.json(await service.discardGap(id));
    }));

    return router;
}

function parseChain(v: unknown): Chain | undefined {
    const s = typeof v === "string" ? v.toUpperCase() : "";
    if (s === Chain.BSC) return Chain.BSC;
    if (s === Chain.POLYGON) return Chain.POLYGON;
    return undefined;
}

function parseSymbol(v: unknown): TokenSymbol | undefined {
    const s = typeof v === "string" ? v.toUpperCase() : "";
    if (s === TokenSymbol.BCOIN) return TokenSymbol.BCOIN;
    if (s === TokenSymbol.SEN) return TokenSymbol.SEN;
    return undefined;
}

function parseStatus(v: unknown): LifecycleStatus | undefined {
    const s = typeof v === "string" ? v.toLowerCase() : "";
    return s === "signed" || s === "executed" || s === "expired" ? s : undefined;
}

function parseKind(v: unknown): LedgerKind | undefined {
    const s = typeof v === "string" ? v.toLowerCase() : "";
    return s === "deposit" || s === "withdraw" ? s : undefined;
}

function clampPage(v: unknown): number {
    const n = typeof v === "string" ? parseInt(v, 10) : NaN;
    return Number.isFinite(n) && n >= 1 ? n : 1;
}

function clampPageSize(v: unknown, def = 50): number {
    const n = typeof v === "string" ? parseInt(v, 10) : NaN;
    if (!Number.isFinite(n) || n <= 0) return def;
    return Math.min(n, 200);
}

// "Refresh" button → ?refresh=1 → bypass the cache and re-read the chain.
function isForced(v: unknown): boolean {
    return v === "1" || v === "true";
}

function clampLimit(v: unknown, def = 200): number {
    const n = typeof v === "string" ? parseInt(v, 10) : NaN;
    if (!Number.isFinite(n) || n <= 0) return def;
    return Math.min(n, 1000);
}
