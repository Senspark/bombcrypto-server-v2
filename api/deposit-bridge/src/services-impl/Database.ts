import pg from "pg";
import ILogger from "../services/ILogger";
import IDatabase from "../services/IDatabase";

// pg returns numeric/int8 as strings by default, which is exactly what we want for wei (numeric(78,0))
// and block numbers — never let a uint256 fall into a JS number. So no type parsers are overridden here.

const {Pool} = pg;

export default class Database implements IDatabase {
    readonly #pool: pg.Pool;
    readonly #logger: ILogger;

    constructor(connectionString: string, logger: ILogger) {
        if (!connectionString) throw new Error("INDEXER_DATABASE_URL is required when INDEXER_ENABLED=true");
        this.#logger = logger.clone('[DB]');
        this.#pool = new Pool({
            connectionString,
            max: 10,
            idleTimeoutMillis: 30_000,
            connectionTimeoutMillis: 10_000,
        });
        // An idle client erroring out must not crash the process — log and let the pool recycle it.
        this.#pool.on('error', (e) => this.#logger.error(e));
    }

    async query<T = any>(text: string, params: any[] = []): Promise<{rows: T[]; rowCount: number}> {
        const res = await this.#pool.query(text, params);
        return {rows: res.rows as T[], rowCount: res.rowCount ?? 0};
    }

    async close(): Promise<void> {
        await this.#pool.end();
    }
}
