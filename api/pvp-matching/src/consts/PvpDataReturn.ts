import {IMatchRule, IMatchTeam, IUser, PvpMode} from "./PvpData";


export interface IMatchHeroInfoReturn {
    /** Hero ID. */
    heroId: number;
    hero_id: number;

    /** Appearance stats. */
    color: number;
    skin: number;
    skinChests: Map<number, number[]>;
    skin_chests: Map<number, number[]>;

    health: number;
    speed: number;
    damage: number;
    bombCount: number;
    bomb_count: number;

    bombRange: number;
    bomb_range: number;

    maxHealth: number;
    max_health: number;

    maxSpeed: number;
    max_speed: number;

    maxDamage: number;
    max_damage: number;

    maxBombCount: number;
    max_bomb_count: number;

    maxBombRange: number;
    max_bomb_range: number;
}


export interface IMatchReturn {
    /** Gets the match token id. */
    id: string;

    /** Gets the match zone. */
    zone: string;

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

