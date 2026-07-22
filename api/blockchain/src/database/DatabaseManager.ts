import pg, {ClientConfig, PoolClient} from 'pg';
import {IConfig} from "../Config";
import IDatabaseManager from "./IDatabaseManager";
import * as console from "node:console";

const QUERY_TIMEOUT = 10 * 1000; // 10 seconds

export default class DatabaseManager implements IDatabaseManager {
  #pool: pg.Pool;

  constructor(
    envConfig: IConfig
  ) {
    const url = new URL(envConfig.postgresConnectionString);
    const password = decodeURIComponent(url.password);
    const config: ClientConfig = {
      user: url.username,
      host: url.hostname,
      database: url.pathname.substring(1),
      password: password,
      port: parseInt(url.port),
      ssl: false
    };
    this.#pool = new pg.Pool(config);
    console.info(`DatabaseManager created at ${url.hostname}:${url.port}/${url.pathname.substring(1)}`);
  }

  async testConnection(): Promise<void> {
    const client = await this.getClient();
    await client.query(`SELECT NOW()`);
    client.release();
  }

  /* eslint-disable  @typescript-eslint/no-explicit-any */
  async query(sql: string, params: any[]): Promise<pg.QueryResult> {
    let client: PoolClient | undefined;
    try {
      client = await this.getClient();
      return await client.query(sql, params);
    } catch (e) {
      throw e;
    } finally {
      client?.release();
    }
  }

  private async getClient(): Promise<pg.PoolClient> {
    /* eslint-disable  @typescript-eslint/no-explicit-any */
    const client = await this.#pool.connect() as any;
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

  private readConnectionString(connectionString: string): Map<string, string> {
    // Sample: "host=localhost,user=postgres,database=postgres,port=5432"

    const parts = connectionString.split(",");
    const connectionInfo: Map<string, string> = new Map();

    parts.forEach(part => {
      const [key, value] = part.split("=");
      connectionInfo.set(key, value);
    });

    return connectionInfo;
  }

  private log(msg: unknown) {
    console.info(msg)
  }

  private logError(msg: unknown) {
    console.error(msg)
  }
}

