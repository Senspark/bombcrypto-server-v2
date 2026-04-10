export default interface IRedisDatabase {
    testConnection(): Promise<boolean>;

    get(key: string): Promise<string | null>;

    mGet(keys: string[]): Promise<(string | null)[]>;

    getAllKeys(pattern: string): Promise<string[]>;

    set(key: string, value: string): Promise<boolean>;

    setWithTTL(key: string, value: string, ttlSeconds: number): Promise<boolean>;

    del(key: string): Promise<void>;

    mDel(keys: string[]): Promise<void>;

    delAllKeys(pattern: string): Promise<void>;

    // =========== Hash<string,string> ===========
    addToHash(key: string, fieldsValues: Map<string, string>): Promise<boolean>;

    readHash(key: string): Promise<Map<string, string>>;

    removeFromHash(key: string, fields: string[]): Promise<boolean>;
}