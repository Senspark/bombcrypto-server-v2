import IRedisDatabase, {IRedisField, IRedisHash, IRedisSet} from "../services/IRedisDatabase";
import {StreamEntry} from "../consts/Consts";

export default class NullCacheDatabase implements IRedisDatabase {
    fields: IRedisField;
    hashes: IRedisHash;
    sets: IRedisSet;

    delAllKeys(pattern: string): Promise<void> {
        return Promise.resolve(undefined);
    }

    getAllKeys(pattern: string): Promise<string[]> {
        return Promise.resolve([]);
    }

    testConnection(): Promise<boolean> {
        return Promise.resolve(false);
    }

    readStreamFromTimeToTime(streamKey: string, startTime: number, endTime: number): Promise<StreamEntry[]> {
        return Promise.resolve([]);
    }

}