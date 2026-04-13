import {IDependencies} from "../Services";
import ServerError from "../consts/ServerError";
import {QueryResult} from "pg";

const DEFAULT_PVP_MODE = 16;
const DEFAULT_TO_TIME = 15 * 60 * 1000; // 15 minutes

export default class PvpFixtureController {
    constructor(
        private readonly _deps: IDependencies,
    ) {
    }

    async registerMatch(data: IRegisterMatchData): Promise<number> {
        this._deps.logger.assert(data, 'Invalid data');
        this._deps.logger.assert(data.userNames && data.userNames.length === 2, 'Invalid data');

        data.mode = data.mode ?? DEFAULT_PVP_MODE;
        data.fromTime = data.fromTime ?? new Date();
        data.toTime = data.toTime ?? new Date(data.fromTime.getTime() + DEFAULT_TO_TIME);
        data.fixedZone = data.fixedZone ?? "";
        data.heroProfile = data.heroProfile ?? 0;

        const sql = `SELECT public.fn_tournament_create_match($1, $2, $3, $4, $5, $6, $7) AS id;`;

        const params = [
            data.userNames[0],
            data.userNames[1],
            data.mode,
            data.heroProfile,
            data.fixedZone,
            data.fromTime,
            data.toTime,
        ];

        let registeredIds: QueryResult<any>;
        try {
            registeredIds = await this._deps.database.query(sql, params);
        } catch (e) {
            this._deps.logger.error(e);
            throw new ServerError(e.message);
        }

        if (registeredIds.rowCount === 0) {
            throw new ServerError('Failed to register match');
        }

        return registeredIds.rows[0].id;
    }

    async registerMatchGroup(data: IRegisterMatchGroupData): Promise<number[]> {
        this._deps.logger.assert(data, 'Invalid data');
        this._deps.logger.assert(data.usersData.length > 0, 'Invalid data');

        data.usersData.forEach(e => {
            e.userName1 = e.userName1.toLowerCase();
            e.userName2 = e.userName2.toLowerCase();
        });

        data.mode = data.mode ?? DEFAULT_PVP_MODE;
        data.fixedZone = data.fixedZone ?? "";
        data.heroProfile = data.heroProfile ?? 0;

        const response: number[] = [];

        for (const userData of data.usersData) {
            const input: IRegisterMatchData = {
                userNames: [userData.userName1, userData.userName2],
                fromTime: userData.fromTime,
                toTime: userData.toTime,
                heroProfile: data.heroProfile,
                fixedZone: data.fixedZone,
                mode: data.mode,
            };
            response.push(await this.registerMatch(input));
        }
        return response;
    }

    async unregisterMatches(registeredIds: number[]): Promise<void> {
        this._deps.logger.assert(registeredIds && registeredIds.length > 0, 'Invalid data');
        const formattedIds = registeredIds.join(',');
        const sql =
            `DELETE
             FROM public.pvp_fixture_matches
             WHERE id IN (${formattedIds});
            DELETE
            FROM public.pvp_tournament
            WHERE id IN (${formattedIds})
              AND status <> 'COMPLETED';`;
        await this._deps.database.query(sql, []);
    }

    async getRegisteredMatches(): Promise<IRegisterMatchData[]> {
        const sql =
            `SELECT *
             FROM public.pvp_fixture_matches
             ORDER BY id DESC;`;
        const result = await this._deps.database.query(sql, []);
        return result.rows.map(r => {
            return {
                registeredId: r.id,
                userIds: [r.player_1_uid, r.player_2_uid],
                userNames: [r.player_1_username, r.player_2_username],
                fromTime: r.from_time,
                toTime: r.to_time,
                heroProfile: r.hero_profile,
                fixedZone: r.fixed_zone,
                mode: r.mode,
            };
        });
    }
}

export interface IRegisterMatchData {
    userNames: string[]; // for output only
    registeredId?: number; // for output only
    fromTime?: Date;
    toTime?: Date;
    heroProfile?: number;
    fixedZone?: string;
    mode?: number;
}

export interface IRegisterPlayersGroup {
    userName1: string;
    userName2: string;
    fromTime: Date;
    toTime: Date;
}

export interface IRegisterMatchGroupData {
    usersData: IRegisterPlayersGroup[];
    heroProfile?: number;
    fixedZone?: string;
    mode?: number;
}

export interface IUserIdName {
    userId: number;
    userName: string;
}