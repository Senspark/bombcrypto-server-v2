import {IMatch} from "../../consts/PvpData";

export interface IFindResult {
    /**
     * Matches found
     */
    matchesFound: IMatch[];

    /**
     * Users temporarily waiting for the next rounds (Fixture)
     */
    pendingUsers: string[];
}