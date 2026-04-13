const BetterJson = {
    toJson(obj: any): string {
        return JSON.stringify(obj, mapJsonReplacer);
    },
    fromJson(json: string): any {
        return JSON.parse(json, mapReviver);
    },
    mapToObject(map: Map<any, any>): {} {
        return Object.fromEntries(map);
    },
    mapToArray(map: Map<any, any>): [any, any][] {
        return Array.from(map.entries());
    },
    objectToMap(obj: {}): Map<any, any> {
        return new Map(Object.entries(obj));
    },
    arrayToMap(arr: [any, any][]): Map<any, any> {
        return new Map(arr);
    }
}

export default BetterJson;

function mapJsonReplacer(key, value) {
    if (value instanceof Map) {
        return {
            dataType: 'Map',
            value: Array.from(value.entries()),
        };
    }
    return value;
}

function mapReviver(key, value) {
    if (value && value.dataType === 'Map') {
        return new Map(value.value); // Convert array of key-value pairs back to Map
    }
    return value;
}