export default interface IRedisDatabase {
    testConnection(): Promise<boolean>;

    getAllKeys(pattern: string): Promise<string[]>;

    delAllKeys(pattern: string): Promise<void>;

    fields: IRedisField;
    sets: IRedisSet;
    hashes: IRedisHash;
}

export interface IRedisField {
    get(key: string): Promise<string | null>;

    mGet(keys: string[]): Promise<(string | null)[]>;

    set(key: string, value: string): Promise<boolean>;

    setWithTTL(key: string, value: string, ttlSeconds: number): Promise<boolean>;

    del(key: string): Promise<void>;

    mDel(keys: string[]): Promise<void>;
}

export interface IRedisSet {
    read(key: string): Promise<string[]>;

    fieldExists(key: string, fieldName: string): Promise<boolean>;

    add(key: string, values: string[]): Promise<boolean>;

    remove(key: string, values: string[]): Promise<boolean>;
}

export interface IRedisHash {
    read(key: string): Promise<MultipleFields>;

    readFields(key: string, fieldsNames: string[]): Promise<(string | undefined)[]>;

    readField(key: string, fieldName: string): Promise<string | undefined>;

    add(key: string, fieldsValues: SingleField | MultipleFields): Promise<boolean>;

    addWithTTL(key: string, fieldsValues: SingleField | MultipleFields, ttlSeconds: number): Promise<boolean>;

    setTTL(key: string, fields: string[], ttlSeconds: number): Promise<boolean>;

    remove(key: string, fieldsNames: string[]): Promise<boolean>;
}

export type SingleField = [fieldKey: string, fieldValue: string];
export type MultipleFields = Map<string, string>;