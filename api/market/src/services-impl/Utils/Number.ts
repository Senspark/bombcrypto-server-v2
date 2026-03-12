import {ILogger} from "@project/Services";

export function toNumberOrZero(value: any): number {
    if (typeof value === 'number') {
        return value;
    }
    if (value === null || value === undefined) {
        return 0;
    }
    const num = Number(value);
    return isNaN(num) ? 0 : num;
}

export function toNumberOr(value: any, defaultValue: number): number {
    if (typeof value === 'number') {
        return value;
    }
    if (value === null || value === undefined) {
        return defaultValue;
    }
    const num = Number(value);
    return isNaN(num) ? defaultValue : num;
}

export function validateData(caller: string, logger: ILogger, ...params: any[]): void {
    for (const param of params) {
        if (param === null || param === undefined) {
            logger.error(`${caller} Invalid parameter: ${param} is null or undefined`);
            throw new Error(`Parameter ${param} must not be null or undefined`);
        }
        if (param <= 0) {
            logger.error(`${caller}  Invalid parameter: ${param} is not greater than 0`);
            throw new Error(`Parameter ${param} must be greater than 0`);
        }
    }
}

export function validateNumberList(caller: string, logger: ILogger, value: any): void {
    if (!Array.isArray(value)) {
        logger.error(`${caller} Invalid parameter: Expected a List<Int>, but got ${typeof value}`);
        throw new Error("Invalid parameter type. Expected a List<Int>.");
    }

    // Ensure all elements in the array are numbers
    if (!value.every(num => typeof num === "number" && Number.isInteger(num))) {
        logger.error(`${caller} Invalid parameter: List contains non-integer values`);
        throw new Error("Invalid parameter: List must contain only integers.");
    }

    for (const num of value) {
        if (num <= 0) {
            logger.error(`${caller} Invalid parameter: ${num} is not greater than 0`);
            throw new Error(`Invalid value in the list: ${num}. All values must be greater than 0.`);
        }
    }
}