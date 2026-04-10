import {IPvpReport, IUser} from "../consts/PvpData";

export interface IPvpQueue {
    addUser(user: IUser): void;

    removeUser(userId: string): void;

    report(): IPvpReport;
}