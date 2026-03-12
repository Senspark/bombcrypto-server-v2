import {Request} from "express";
import {ILogger} from "../Services";
import {ServerError} from "../consts/ServerError";
import {JWTPayload, jwtVerify} from "jose";

export default class BearerService {
    constructor(private readonly _logger: ILogger, private jwtBearerSecret: string) {
    }

    public async verifyBearer(req: Request): Promise<boolean> {
        const authHeader = req.headers['authorization'];
        if (!authHeader) {
            throw new ServerError('Auth token is required');
        }
        const authToken = authHeader.replace('Bearer ', '');
        return await this.verifyToken(authToken);
    }

    private async verifyToken(token: string): Promise<boolean> {
        const encoder = new TextEncoder();
        const key = encoder.encode(this.jwtBearerSecret);
        try {
            const {payload} = await jwtVerify(token, key);
            if(payload)
                return true;

            this._logger.error('Invalid Auth token');
            return false;
        } catch (e) {
            this._logger.error(e.message);
            return false;
        }
    }
}