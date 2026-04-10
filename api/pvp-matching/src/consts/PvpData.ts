export interface IJoinQueueRequestBody {
    userName: string,
    pings: Map<string, number>,
    data: {
        serverId: string,
        matchId?: string,
        mode: number,
        isBot: boolean,
        displayName: string,
        totalMatchCount: number, // Used to determine whether user should play with bot.
        rank: number,
        point: number,
        boosters: number[],
        availableBoosters: Map<number, number>,
        hero: IMatchHeroInfo,
        avatar: number,
        // Other aux data, may vary.
    },
    timestamp?: number,
    newServer?: boolean,
}

export interface ILeaveQueueRequestBody {
    userName: string,
}

export interface IMatchHeroInfo {
    /** Hero ID. */
    heroId: number;

    /** Appearance stats. */
    color: number;
    skin: number;
    skinChests: Map<number, number[]>;

    health: number;
    speed: number;
    damage: number;
    bombCount: number;
    bombRange: number;
    maxHealth: number;
    maxSpeed: number;
    maxDamage: number;
    maxBombCount: number;
    maxBombRange: number;
}

export interface IUser {
    /** User name. */
    id: string;

    /** Server ID (sa, sea). */
    serverId: string;

    /** Network stats. */
    networkStats: INetworkStats;

    /** Preferrable match ID. */
    matchId?: string;

    /** Match mode mask. */
    mode: number;

    /** Total number played match (used to check play with bot). */
    totalMatchCount: number;

    /** Pvp rank. */
    rank: number;

    /** Pvp point. */
    point: number;

    /** Matching timestamp. */
    timestamp: number;

    /**
     * Thời gian này sẽ update lại mỗi khi user keep joining queue
     */
    refreshTimestamp: number;

    /**
     * Kiểm tra client dùng server cũ hay mới
     */
    newServer?: boolean;

    /** Aux data, may vary. */
    data: {
        isBot: boolean,

        displayName: string,

        boosters: number[],
        availableBoosters: Map<number, number>,
        avatar: number,
        // Other aux data.

        // Hero data.
        hero: IMatchHeroInfo,
    },
}

export interface INetworkStats {
    pings: Map<string, number>;
    lowestPingZone: string;
    zones: string[];

    /** Gets ping latency from the specified server zone. */
    getPing(zone: string): number;
}

export interface IZoneInfo {
    id: string,
    host: string,
}

export interface IServerInfo {
    id: string,
    zone: string,
    host: string,
    port: number,
    use_ssl: boolean,
    udp_host: string,
    udp_port: number,
}

export interface IMatch {
    /** Gets the match token id. */
    id: string;

    /** server id support client cũ. */
    zone: string;
    // cho client mới
    serverDetail: string

    mode: PvpMode;

    rule: IMatchRule;

    team: IMatchTeam[],

    /** Gets the users in this match. */
    users: IUser[];

    /** Gets the match timestamp. */
    timestamp: number,

    /**
     *  Thông tin hiển thị bổ sung thêm
     */
    desc: string;
}

export interface IMatchTeam {
    slots: number[],
}

export interface IMatchRule {
    roomSize: number;

    teamSize: number;

    /** Whether a draw result is allowed. */
    canDraw: boolean;

    /** Minimum number of rounds. */
    round: number;

    isTournament: boolean;
}

export interface IPvpReport {
    queuing_users: IPvpReportUser[];
    matches: IPvpReportMatch[];
}

export interface IPvpReportUser {
    user_id: string;
    is_bot: boolean;
    zone: string;
    rank: number;
    point: number;
    time_stamp: number;
    refresh_time_stamp: number;
}

export interface IPvpReportMatch {
    match_id: string;
    zone: string;
    mode: number;
    times_stamp: number;
    players: IPvpReportUser[];
    desc: string;
}

export enum PvpMode {
    FFA_2 = 1 << 0, // 1
    FFA_3 = 1 << 1, // 2
    FFA_4 = 1 << 2, // 4
    Team_2v2 = 1 << 3, // 8
    FFA_2_B3 = 1 << 4, // 16
    FFA_2_B5 = 1 << 5, // 32
    FFA_2_B7 = 1 << 6, // 64
}

export interface IPvpResultInfo {
    id: string;
    serverId: string;
    timestamp: number;
    mode: PvpMode;
    isDraw: boolean;
    winningTeam: number;
    scores: number[];
    duration: number;
    rule: IMatchRuleInfo;
    team: IMatchTeamInfo[];
    info: IPvpResultUserInfo[];
}

export interface IPvpResultUserInfo {
    serverId: string;
    isBot: boolean;
    teamId: number;
    userId: number;
    username: string;
    rank: number;
    point: number;
    matchCount: number;
    winMatchCount: number;
    deltaPoint: number;
    usedBoosters: Map<number, number>;
    quit: boolean;
    heroId: number;
    damageSource: number;
    rewards: Map<number, number>;
    collectedItems: number[];
}

export interface IMatchRuleInfo {
    roomSize: number;
    teamSize: number;
    round: number;
    canDraw: boolean;
    isTournament: boolean;
}

export interface IMatchTeamInfo {
    slots: number[];
}