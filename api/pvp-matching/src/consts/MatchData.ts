import {PvpMode} from "./PvpData";

export interface IPvpRoomInfo {
    match_info: IPvpMatchInfo;
    match_data: IPvpMatchData;
}

// Must match IMatchInfo.kt
export interface IPvpMatchInfo {
    id: string;
    mode: number;
    team: IPvpMatchTeamInfo[];
    info: IPvpMatchUserInfo[];
    // Aux.
}

// Must match IMatchData.kt
export interface IPvpMatchData {
    id: string;
    status: PvpMatchStatus;
    observer_count: number;
    start_timestamp: number;
    ready_start_timestamp: number;
    round_start_timestamp: number;
    results: IPvpMatchResultInfo[];
}

export enum PvpMatchStatus {
    Ready,
    Started,
    Finished,
    Uninitialized,
}

export interface IPvpMatchResultInfo {
    is_draw: boolean;
    winning_team: number;
}

export interface IPvpMatchTeamInfo {
    slots: number[];
}

export interface IPvpMatchUserInfo {
    user_id: number;
    username: string;
    display_name: string;
    rank: number;
}

// Must match IPvpFixtureMatchInfo.kt.
export interface IPvpTournamentMatchInfo {
    id: string;
    status: PvpTournamentMatchStatus;

    find_begin_timestamp: number;
    find_end_timestamp: number;
    finish_timestamp: number;

    mode: PvpMode;
    info: IPvpTournamentMatchUserInfo[];
}

export enum PvpTournamentMatchStatus {
    Pending,
    Aborted,
    Completed,
}

// Must match IPvpFixtureMatchInfo.kt
export interface IPvpTournamentMatchUserInfo {
    /** Score result. */
    score: number;

    user_id: number;
    username: string;
    display_name: string;
    rank: number;
}

export interface ITournamentMatchListData {
    id: string;
    status: TournamentMatchListStatus;
    mode: PvpMode;
    observer_count: number;
    find_begin_timestamp: number;
    find_end_timestamp: number;
    start_timestamp: number
    finish_timestamp: number;
    info: IPvpTournamentMatchUserInfo[];
}

export enum TournamentMatchListStatus {
    Waiting,
    Finding,
    Playing,
    Finished,
    Abandoned
}