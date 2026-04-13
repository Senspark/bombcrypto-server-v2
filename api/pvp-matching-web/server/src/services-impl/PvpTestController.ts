import {IDependencies} from "../Services";
import {CachedKeys} from "../consts/Consts";
import * as console from "node:console";

export default class PvpTestController {
    constructor(
        private readonly _deps: IDependencies,
    ) {
    }

    async registerMatch(data: IRequestData): Promise<void> {
        console.log(JSON.stringify(data));
        this._deps.logger.assert(data && data.user1 && data.user2 && data.zone, 'Invalid data');

        const players = [
            {user1: data.user1.toLowerCase(), user2: data.user2.toLowerCase(), zone: data.zone, id: data.id},
        ];

        const testMap = new Map<string, IRequestData[]>();
        testMap.set(data.user1.toLowerCase(), players);

        if (data.user2 !== "bot") {
            testMap.set(data.user2.toLowerCase(), players);
        }

        // Convert the Map to a format suitable for Redis hash
        const fieldsValues = new Map<string, string>();
        for (const [key, value] of testMap.entries()) {
            fieldsValues.set(key, JSON.stringify(value));
        }
        // Add the data to Redis hash
        await this._deps.redis.hashes.add(CachedKeys.AP_PVP_TEST_MATCHES, fieldsValues);
    }

    async getRegisteredMatch(): Promise<Map<string, IRequestData>> {
        const result = await this._deps.redis.hashes.read(CachedKeys.AP_PVP_TEST_MATCHES);
        console.log("Call redis " + JSON.stringify(result));
        if (result.size === 0) {
            return new Map<string, IRequestData>();
        }
        const resultMap = new Map<string, IRequestData>();
        result.forEach((value, key) => {
            resultMap.set(key, JSON.parse(value));
        });

        return resultMap;
    }

    async unregisterMatch(registeredIds: string[]): Promise<void> {
        this._deps.logger.assert(registeredIds && registeredIds.length > 0, 'Invalid data');

        await this._deps.redis.hashes.remove(CachedKeys.AP_PVP_TEST_MATCHES, registeredIds);
    }
}

export interface IRequestData {
    user1: string;
    user2: string;
    zone: string;
    id?: number;
}
