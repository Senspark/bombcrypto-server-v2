import IDatabaseManager from "./IDatabaseManager";

export default class NullDatabaseManager implements IDatabaseManager {
    async testConnection(): Promise<void> {
        return;
    }

    async query(sql: string, params: any[]): Promise<any> {
        return Promise.resolve(null);
    }
}