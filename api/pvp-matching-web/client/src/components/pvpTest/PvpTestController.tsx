import {sendGetRequest, sendPostRequest} from "../../utils/NetworkUtils";
import Urls from "../../consts/Urls";

export default class PvpTestController {

    async getRegisteredMatches(): Promise<IRegisterMatchTestData[]> {
        try {
            const result = await sendGetRequest<IRegisterMatchTestData[]>(Urls.GetRegisterTestMatch, true);
            return result ?? [];
        } catch (e) {
            console.error(e);
            return [];
        }
    }


    async registerMatchTest(data: IRegisterMatchTestData): Promise<boolean | string> {
        try {
            const result = await sendPostRequest<boolean>(Urls.RegisterTestMatch, data, true);
            return result ?? false;
        } catch (e: any) {
            console.error(e);
            return e['message'] ?? 'Error';
        }
    }

    async unregisterMatch(registeredIds: string[]): Promise<boolean> {
        try {
            const data = {id: registeredIds};
            console.log(JSON.stringify(data));
            const result = await sendPostRequest<boolean>(Urls.UnRegisterTestMatch, data, true);
            return result ?? false;
        } catch (e) {
            console.error(e);
            return false;
        }
    }
}


export interface IRegisterMatchTestData {
    user1: string;
    user2: string;
    zone: string;
    id?: number;
}