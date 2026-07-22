import {IHeroStakeApi, StakeAmount, StakeAmountBig} from "../IHeroStakeApi";
import IDependencies from "../services/IDependencies";
import {formatNumber} from "../Utility";
import {BigNumber} from "ethers";

const ZERO = BigNumber.from(0);

/**
 * Giống như class HeroStakeApi.Blockchain
 * Nhưng sẽ query database trước
 */
export class HeroStakeApiDatabase implements IHeroStakeApi {

  constructor(
    private readonly _dep: IDependencies,
    public readonly network: string,
  ) {
  }

  public get name(): string {
    return this.network;
  }

  async getStakedAmount(heroId: number): Promise<StakeAmount> {
    if (isNaN(heroId)) {
      return [0, 0];
    }
    const result = await this.getStakedAmounts(heroId);
    if (result.length === 0) {
      return [0, 0];
    }
    return result[0];
  }

  async getStakedAmounts(...heroIds: number[]): Promise<StakeAmount[]> {
    const result = await this.getStakedAmountsBig(...heroIds);
    return result.map(e => [formatNumber(e[0]), formatNumber(e[1])]);
  }

  async getStakedAmountsBig(...heroIds: number[]): Promise<StakeAmountBig[]> {
    if (heroIds.length === 0) {
      return [];
    }
    const sql =
      `SELECT *
       FROM fn_get_heroes_stake_v2($1, $2);`;
    const res = await this._dep.database.query(sql, [this.network, heroIds]);

    const result: StakeAmountBig[] = [];

    // return table(hero_id:number, bcoin:bigint, sen:bigint)
    for (let i = 0; i < heroIds.length; i++) {
      const id = heroIds[i];
      const row = res.rows.find((row: any) => row.hero_id === id);
      if (row) {
        result.push([BigNumber.from(row.bcoin), BigNumber.from(row.sen)]);
      } else {
        result.push([ZERO, ZERO]);
      }
    }
    return result;
    // return arr.map((value: number | null) => value?.toLocaleString('fullwide', {useGrouping: false}) || 0);
  }

  async setStakedAmount(heroId: number, amount: StakeAmountBig): Promise<StakeAmount> {
    const bcoin = amount[0];
    const sen = amount[1];

    const sql =
      `INSERT INTO hero_stake (token_id, network, amount, amount_sen, modified_at)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (token_id, network) DO UPDATE SET amount     = EXCLUDED.amount,
                                                     amount_sen = EXCLUDED.amount_sen;`;
    await this._dep.database.query(sql, [heroId, this.network, bcoin.toString(), sen.toString(), new Date()]);

    return [formatNumber(bcoin), formatNumber(sen)];
  }

  broadcastStakedAmount(heroId: number, amount: StakeAmountBig, uid?: number): Promise<void> {
    return Promise.resolve(undefined);
  }
}