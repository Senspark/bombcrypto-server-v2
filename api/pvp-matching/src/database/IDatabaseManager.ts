import {QueryResult} from "pg";

export default interface IDatabaseManager {
    testConnection(): Promise<void>;

    /* eslint-disable  @typescript-eslint/no-explicit-any */
    query(sql: string, params: any[]): Promise<QueryResult>;
}