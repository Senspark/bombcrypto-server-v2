export default interface IRedisDatabase {
  testConnection(): Promise<boolean>;

  getAllKeys(pattern: string): Promise<string[]>;

  get(key: string): Promise<string | null>;

  mGet(keys: string[]): Promise<(string | null)[]>;

  set(key: string, value: string): Promise<boolean>;

  setWithTTL(key: string, value: string, ttlSeconds: number): Promise<boolean>;

  del(key: string): Promise<void>;

  mDel(keys: string[]): Promise<void>;

  delAllKeys(pattern: string): Promise<void>;

  // =========== Set<string> ===========

  readSet(key: string): Promise<string[]>;

  addToSet(key: string, values: string[]): Promise<boolean>;

  removeFromSet(key: string, values: string[]): Promise<boolean>;


  // =========== Hash<string,string> ===========

  readHash(key: string): Promise<MultipleFields>;

  readHashFields(key: string, fieldsNames: string[]): Promise<(string | undefined)[]>;

  addToHash(key: string, fieldsValues: SingleField | MultipleFields): Promise<boolean>;

  /**
   * Set a single hash field and give it a per-field TTL via HEXPIRE.
   * Requires Redis 7.4+. Expired fields are removed by the server automatically.
   */
  setHashFieldWithTTL(key: string, field: string, value: string, ttlSeconds: number): Promise<void>;

  removeFromHash(key: string, fieldsNames: string[]): Promise<boolean>;
}

export type SingleField = [fieldKey: string, fieldValue: string];
export type MultipleFields = Map<string, string>;