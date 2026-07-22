import IDatabaseManager from "./IDatabaseManager";

export interface IHouseData {
  houseId: number;
  details: string;
}

export class UserTokensDatabase {
  constructor(
    private readonly _database: IDatabaseManager,
    private readonly _network: string,
  ) {
  }

  async getHouse(ownerWallet: string): Promise<IHouseData[]> {
    const sql =
      `SELECT *
       FROM house h
              LEFT JOIN wallet w ON w.uid = h.uid
       WHERE w.address = $1
         and w.network = $2;`;
    const res = await this._database.query(sql, [ownerWallet, this._network]);
    return res.rows.map((row) => {
      return {
        houseId: row.token_id,
        details: row.details,
      };
    });
  }

  async getTokenDetails(tokenId: number): Promise<string> {
    const sql =
      `SELECT details
       FROM house
       WHERE token_id = $1
         and network = $2;`;
    const res = await this._database.query(sql, [tokenId, this._network]);
    return res.rows[0].details;
  }

  async addHouse(ownerWallet: string, houseId: number, details: string): Promise<void> {
    const sql = `CALL sp_add_house($1, $2, $3, $4);`;
    await this._database.query(sql, [ownerWallet, this._network, houseId, details]);
  }
}