import pg, {PoolConfig} from 'pg';
import {IConfig} from "../Config";
import IDatabaseManager from "./IDatabaseManager";

const QUERY_TIMEOUT = 10 * 1000; // 10 seconds

export default class DatabaseManager implements IDatabaseManager {
    _pool: pg.Pool;

    constructor(
        envConfig: IConfig
    ) {
        const url = new URL(envConfig.postgresConnectionString);
        const password = decodeURIComponent(url.password);
        const config: PoolConfig = {
            user: url.username,
            host: url.hostname,
            database: url.pathname.substring(1),
            password: password,
            port: parseInt(url.port),
            ssl: false,
            max: 3,
        };
        this._pool = new pg.Pool(config);
    }

    async testConnection(): Promise<void> {
        const client = await this.getClient();
        await client.query(`SELECT NOW()`);
        client.release();
    }

    /* eslint-disable  @typescript-eslint/no-explicit-any */
    async query(sql: string, params: any[]): Promise<pg.QueryResult> {
        const client = await this.getClient();
        const res = await client.query(sql, params);
        client.release();

        return res;
    }

    private async getClient(): Promise<pg.PoolClient> {
        /* eslint-disable  @typescript-eslint/no-explicit-any */
        const client = await this._pool.connect() as any;
        const query = client.query;
        const release = client.release;

        // set a timeout, after which we will log this client's last query
        const timeout = setTimeout(() => {
            this.logError(`Last timeout query: ${client.lastQuery}`);
        }, QUERY_TIMEOUT);

        // monkey patch the query method to keep track of the last query executed
        client.query = (...args: any[]) => {
            client.lastQuery = args;
            return query.apply(client, args);
        };

        client.release = () => {
            // clear our timeout
            clearTimeout(timeout);
            // set the methods back to their old un-monkey-patched version
            client.query = query;
            client.release = release;
            return release.apply(client);
        };
        return client;
    }

    private combineQueryString(sql: string, params: any[]): string {
        let combinedSql = sql;
        params.forEach((param, i) => {
            combinedSql = combinedSql.replace(`$${i + 1}`, param);
        });
        return combinedSql;
    }

    private log(msg: unknown) {
        console.info(msg)
    }

    private logError(msg: unknown) {
        console.error(msg)
    }
}

