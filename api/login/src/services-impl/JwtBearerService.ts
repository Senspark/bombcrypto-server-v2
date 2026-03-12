import JwtService from "./JwtService";
import {Request} from "express";
import {ILogger} from "../Services";
import {ServerError} from "../consts/ServerError";

export default class JwtBearerService {
    constructor(private readonly _logger: ILogger, jwtBearerSecret: string) {
        this.#jwtService = new JwtService(_logger, jwtBearerSecret);
    }

    #jwtService: JwtService;

    public async verifyBearer(req: Request): Promise<void> {
        const authHeader = req.headers['authorization'];
        if (!authHeader) {
            throw new ServerError('Auth token is required');
        }
        const authToken = authHeader.replace('Bearer ', '');
        return await this.verifyBearerJwt(authToken);
    }

    public async verifyBearerJwt(jwt: string): Promise<void> {
        const payload = await this.#jwtService.verifyToken(jwt);
        if (!payload) {
            throw new ServerError('Invalid Auth token');
        }
    }

}