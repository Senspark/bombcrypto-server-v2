// Thin async query surface over the dedicated report Postgres. Kept minimal — the Indexer module is the
// only consumer, and it shares no connection with the signing path.
export default interface IDatabase {
    query<T = any>(text: string, params?: any[]): Promise<{rows: T[]; rowCount: number}>;

    close(): Promise<void>;
}
