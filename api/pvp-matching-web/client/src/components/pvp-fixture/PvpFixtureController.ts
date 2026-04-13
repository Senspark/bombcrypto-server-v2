import {sendGetRequest, sendPostRequest} from "../../utils/NetworkUtils";
import Urls from "../../consts/Urls";

export default class PvpFixtureController {

    async getRegisteredMatches(): Promise<IRegisterMatchOutput[]> {
        try {
            const result = await sendGetRequest<IRegisterMatchOutput[]>(Urls.FixtureGetRegisteredMatches, true);
            return result ?? [];
        } catch (e) {
            console.error(e);
            return [];
        }
    }

    async registerMatchGroup(data: IRegisterMatchGroupData): Promise<boolean | string> {
        try {
            const result = await sendPostRequest<boolean>(Urls.FixtureRegisterMatchesGroup, data, true);
            return result ?? false;
        } catch (e: any) {
            console.error(e);
            return e['message'] ?? 'Error';
        }
    }

    async unregisterMatch(registeredIds: number[]): Promise<boolean> {
        try {
            const data = {id: registeredIds};
            const result = await sendPostRequest<boolean>(Urls.FixtureUnregisterMatches, data, true);
            return result ?? false;
        } catch (e) {
            console.error(e);
            return false;
        }
    }
}

export interface IRegisterMatchOutput {
    registeredId: number;
    userIds: number[];
    userNames: string[];
    fromTime: Date;
    toTime: Date;
    heroProfile: number;
    fixedZone: string;
    mode: number;
}

export interface IRegisterPlayersGroup {
    userName1: string;
    userName2: string;
    fromTime: Date;
    toTime: Date;
}

export interface IRegisterMatchGroupData {
    usersData: IRegisterPlayersGroup[];
    heroProfile: number;
    fixedZone: string;
    mode: number;
}