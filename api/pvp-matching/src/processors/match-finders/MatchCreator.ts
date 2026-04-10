import md5 from "md5";
import {IMatch, IMatchRule, IMatchTeam, IUser, PvpMode} from "../../consts/PvpData";
import {assert} from "console";
import {IServerDetail} from "../NetworkAddress";

export default class MatchCreator {
    public createMatch(zone: string, detail: IServerDetail, mode: PvpMode, users: IUser[], options?: IOptions): IMatch {
        const timestamp = Date.now();
        const id = md5(`${users.map(item => item.id).join(`_`)}_${timestamp}`);
        return this.createMatchWithId(id, zone, detail, mode, users, options?.heroProfile ?? 0, options?.desc ?? "");
    }

    private createMatchWithId(id: string, zone: string, detail: IServerDetail, mode: PvpMode, users: IUser[], heroProfile: number, desc: string): IMatch {
        const rule = createRule(mode);
        const team = createTeam(rule);
        const timestamp = Date.now();
        return {
            id: id,
            zone,
            serverDetail: JSON.stringify(detail),
            mode,
            team,
            rule,
            users: users.map(user => createHeroByProfile(user, heroProfile, mode)),
            desc,
            timestamp,
        };
    }
}

function createHeroByProfile(user: IUser, heroProfile: number, mode: PvpMode): IUser {
    switch (heroProfile) {
        case 1:
            return fixedPlayData(user, createHeroProfileForTournamentEasyMode(), mode);
        case 2:
            return fixedPlayData(user, createHeroProfileForTournamentNormalMode(), mode);
        default:
            return user;
    }
}

/**
 * start: 2 speed - 2 range - 2 bomb
 * fixed: hp 3 - dmg 3
 * max: 5 speed - 5 range - 5 bomb
 */
function createHeroProfileForTournamentEasyMode(): IFixedHeroInfo {
    return {
        speed: 2,
        bomb_range: 2,
        bomb_count: 2,

        health: 3,
        damage: 3,

        max_health: 3,
        max_damage: 3,

        max_speed: 5,
        max_bomb_range: 5,
        max_bomb_count: 5,
    };
}

/**
 * start: 2 speed - 2 range - 2 bomb
 * fixed: hp 5 - dmg 3
 * max: 5 speed - 5 range - 5 bomb
 */
function createHeroProfileForTournamentNormalMode(): IFixedHeroInfo {
    return {
        speed: 2,
        bomb_range: 2,
        bomb_count: 2,

        health: 5,
        damage: 3,

        max_health: 5,
        max_damage: 3,

        max_speed: 5,
        max_bomb_range: 5,
        max_bomb_count: 5,
    };
}


function createRule(mode: PvpMode) {
    const dict: { [key: number]: IMatchRule } = {
        [PvpMode.FFA_2]: {roomSize: 2, teamSize: 1, canDraw: true, round: 1, isTournament: false},
        [PvpMode.FFA_3]: {roomSize: 3, teamSize: 1, canDraw: true, round: 1, isTournament: false},
        [PvpMode.FFA_4]: {roomSize: 4, teamSize: 1, canDraw: true, round: 1, isTournament: false},
        [PvpMode.Team_2v2]: {roomSize: 4, teamSize: 2, canDraw: true, round: 1, isTournament: false},
        [PvpMode.FFA_2_B3]: {roomSize: 2, teamSize: 1, canDraw: false, round: 3, isTournament: true},
        [PvpMode.FFA_2_B5]: {roomSize: 2, teamSize: 1, canDraw: false, round: 5, isTournament: true},
        [PvpMode.FFA_2_B7]: {roomSize: 2, teamSize: 1, canDraw: false, round: 7, isTournament: true},
    };
    const rule = dict[mode];
    assert(rule.roomSize % rule.teamSize === 0);
    return rule;
}

function createTeam(rule: IMatchRule) {
    const teamCount = rule.roomSize / rule.teamSize;
    const team: IMatchTeam[] = [...Array(teamCount).keys()].map(teamId => ({
        slots: [...Array(rule.teamSize).keys()].map(teamSlotId =>
            teamId * rule.teamSize + teamSlotId
        ),
    }));
    return team;
}

function fixedPlayData(user: IUser, hero: IFixedHeroInfo, mode: PvpMode): IUser {
    // Clone.
    const it: IUser = JSON.parse(JSON.stringify(user));
    it.data.boosters = [];
    it.data.availableBoosters = new Map<number, number>();
    // Don't assign, original data may contain aux data.
    it.mode = mode;
    it.data.hero.health = hero.health;
    it.data.hero.speed = hero.speed;
    it.data.hero.damage = hero.damage;
    it.data.hero.bombCount = hero.bomb_count;
    it.data.hero.bombRange = hero.bomb_range;
    it.data.hero.maxHealth = hero.max_health;
    it.data.hero.maxSpeed = hero.max_speed;
    it.data.hero.maxDamage = hero.max_damage;
    it.data.hero.maxBombCount = hero.max_bomb_count;
    it.data.hero.maxBombRange = hero.max_bomb_range;
    return it;
}

interface IFixedHeroInfo {
    health: number;
    speed: number;
    damage: number;
    bomb_count: number;
    bomb_range: number;
    max_health: number;
    max_speed: number;
    max_damage: number;
    max_bomb_count: number;
    max_bomb_range: number;
}

export interface IOptions {
    heroProfile?: number;
    desc?: string;
}