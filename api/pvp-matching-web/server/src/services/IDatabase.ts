import pg from "pg";

export default interface IDatabase {
    query(sql: string, params: any[]): Promise<pg.QueryResult>;

    testConnection(): Promise<void>;
}