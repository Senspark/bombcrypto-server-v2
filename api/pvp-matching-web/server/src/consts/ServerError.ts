export default class ServerError extends Error {
    constructor(message: string) {
        super(message);
    }
}

export class ValidationError extends Error {
    constructor(message: string) {
        super(message);
    }
}