import {IUser} from "../consts/PvpData";
import {IFindResult} from "./match-finders/Data";

export default interface IMatchFinder {
    find(users: IUser[]): Promise<IFindResult>;
}