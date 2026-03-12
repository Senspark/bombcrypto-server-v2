import {JWTPayload, jwtVerify, SignJWT} from 'jose';
import {ILogger} from "../Services";
import {CHAIN} from "../dto/CHAIN";
import {Request} from "express";
import {JwtDapp, JwtPayload, JwtPayloadAccount, JwtPayloadGuest} from "../consts/Consts";
import {ServerError} from "../consts/ServerError";

export default class JwtService {
    constructor(_logger: ILogger, private readonly _secret: string) {
        this.#logger = _logger;
    }

    readonly #logger: ILogger;

    public createAuthToken = this.buildCreateToken<AuthToken>('1y');
    public createLoginToken = this.buildCreateToken<AuthToken>('15m');


    public createPayloadToken = this.buildCreateToken<PayloadToken>('15m');

    public async verifyToken(token: string): Promise<JWTPayload | null> {
        const encoder = new TextEncoder();
        const key = encoder.encode(this._secret);
        try {
            const {payload} = await jwtVerify(token, key);
            return payload;
        } catch (e) {
            this.#logger.error(e.message);
            return null;
        }
    }

    public buildCreateToken<T extends JWTPayload>(expirationTime: string): (payload: T) => Promise<string> {
        return async (payload: T) => {
            const encoder = new TextEncoder();
            const key = encoder.encode(this._secret);
            return new SignJWT(payload)
                .setProtectedHeader({alg: 'HS256'})
                .setIssuedAt()
                .setExpirationTime(expirationTime)
                .sign(key);
        };
    }

    /**
     * Use for client unity
     * @param req - The request object containing the JWT in the authorization header.
     * @param userName - The username to verify against the JWT payload.
     * @returns A promise that resolves to true if the JWT is valid and matches the userName, false otherwise.
     */
    public async verifyJwtHeader(req: Request, userName: string): Promise<boolean> {
        const jwtHeader = req.headers['authorization'];
        if (!jwtHeader) {
            this.#logger.error('Missing jwt');
            return false;
        }

        const jwt = jwtHeader.replace('Bearer ', '');
        const payload = await this.verifyToken(jwt);

        if (!payload) {
            this.#logger.error('Invalid jwt');
            return false;
        }

        // BscWalletService
        if ((payload as JwtPayload).address !== undefined) {
            return payload.address === userName;
        }

        //Account senspark
        else if ((payload as JwtPayloadAccount).userName !== undefined) {
            return payload.userName === userName;
        }

        //Guest
        else if ((payload as JwtPayloadGuest).userName !== undefined) {
            return payload.userName === userName;
        }

        //Dapp
        else if ((payload as JwtDapp).userName !== undefined) {
            return payload.userName === userName;
        }

        this.#logger.error('Jwt not match any type');
        return false;

    }

    /**
     * Gets JWT from request header and converts it to JwtDapp
     * @param req - The request object containing the JWT in the authorization header
     * @returns A promise that resolves to JwtDapp object with the userName property
     * @throws ServerError if the token is missing, invalid, or doesn't match JwtDapp structure
     */
    public async getJwtFromHeader(req: Request): Promise<JwtDapp> {
        const authHeader = req.headers['authorization'];
        if (!authHeader) {
            throw new ServerError('Auth token is required');
        }
        const jwt = authHeader.replace('Bearer ', '');
        const payload = await this.verifyToken(jwt);
        if (!payload) {
            throw new ServerError('Invalid Auth token');
        }
        
        // Check if payload contains the userName property
        if ((payload as JwtDapp).userName === undefined) {
            this.#logger.error('JWT payload does not contain userName property');
            throw new ServerError('Invalid JWT payload structure');
        }
        
        return payload as JwtDapp;
    }
}

/**
 * Payload of the token.
 */
export type AuthToken = {
    address: string;
    network: CHAIN;
};

export type PayloadToken = {
    payload: string;
};