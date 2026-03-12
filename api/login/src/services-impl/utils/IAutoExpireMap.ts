export default interface IAutoExpireMap<K, V> {
    add(key: K, value: V, expireSeconds: number): Promise<void>;

    get(key: K): Promise<V | null>;

    extendExpireTime(key: K, expireSeconds: number): Promise<void>;

    remove(key: K): Promise<void>;
}