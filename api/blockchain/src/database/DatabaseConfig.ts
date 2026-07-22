import IDatabaseManager from "./IDatabaseManager";

interface IDatabaseConfigData {
  latestBlockPolygon: number;
  latestBlockBsc: number;
  latestBlockHousePolygon: number;
  latestBlockHouseBsc: number;
}

export enum ConfigKeys {
  None,
  LatestBlockPolygon,
  LatestBlockBsc,
  LatestBlockHousePolygon,
  LatestBlockHouseBsc,
}

const StrKeys = {
  LATEST_BLOCK_POLYGON: 'latest_block_polygon',
  LATEST_BLOCK_BSC: 'latest_block_bsc',
  LATEST_BLOCK_HOUSE_POLYGON: 'latest_block_house_polygon',
  LATEST_BLOCK_HOUSE_BSC: 'latest_block_house_bsc',
}

export class DatabaseConfig {

  private _config: IDatabaseConfigData = {
    latestBlockPolygon: 0,
    latestBlockBsc: 0,
    latestBlockHousePolygon: 0,
    latestBlockHouseBsc: 0
  };

  constructor(
    private readonly _isProduction: boolean,
    private readonly _database: IDatabaseManager) {
  }

  private static _default: DatabaseConfig;

  public static createDefault(isProduction: boolean, database: IDatabaseManager): DatabaseConfig {
    if (!this._default) {
      this._default = new DatabaseConfig(isProduction, database);
    }
    return this._default;
  }

  async initialize() {
    const sql =
      `SELECT *
       FROM config;`;
    const res = await this._database.query(sql, []);
    for (const r of res.rows) {
      this.setConfigObject(this.getKeyFromStr(r.key), r.value)
    }
  }

  public getConfig(): IDatabaseConfigData {
    return this._config;
  }

  public async setConfig(key: ConfigKeys, value: string): Promise<void> {
    const sql =
      `UPDATE config
       SET value = $2
       WHERE key = $1;`;
    await this._database.query(sql, [this.getStrFromKey(key), value]);
    this.setConfigObject(key, value);
  }

  private getStrFromKey(key: ConfigKeys) {
    if (key === ConfigKeys.LatestBlockPolygon) {
      return StrKeys.LATEST_BLOCK_POLYGON;
    } else if (key === ConfigKeys.LatestBlockBsc) {
      return StrKeys.LATEST_BLOCK_BSC;
    } else if (key === ConfigKeys.LatestBlockHousePolygon) {
      return StrKeys.LATEST_BLOCK_HOUSE_POLYGON;
    } else if (key === ConfigKeys.LatestBlockHouseBsc) {
      return StrKeys.LATEST_BLOCK_HOUSE_BSC;
    }
    throw new Error(`Invalid key: ${key}`);
  }

  private getKeyFromStr(str: string) {
    if (str === StrKeys.LATEST_BLOCK_POLYGON) {
      return ConfigKeys.LatestBlockPolygon;
    }
    if (str === StrKeys.LATEST_BLOCK_BSC) {
      return ConfigKeys.LatestBlockBsc;
    }
    if (str === StrKeys.LATEST_BLOCK_HOUSE_POLYGON) {
      return ConfigKeys.LatestBlockHousePolygon;
    }
    if (str === StrKeys.LATEST_BLOCK_HOUSE_BSC) {
      return ConfigKeys.LatestBlockHouseBsc;
    }
    return ConfigKeys.None;
  }

  private setConfigObject(key: ConfigKeys, value: string) {
    if (key === ConfigKeys.LatestBlockBsc) {
      this._config.latestBlockBsc = parseInt(value);
      console.info(`Set latest block BSC: ${value}`);
    } else if (key === ConfigKeys.LatestBlockPolygon) {
      this._config.latestBlockPolygon = parseInt(value);
      console.info(`Set latest block Polygon: ${value}`);
    } else if (key === ConfigKeys.LatestBlockHouseBsc) {
      this._config.latestBlockHouseBsc = parseInt(value);
      console.info(`Set latest block House BSC: ${value}`);
    } else if (key === ConfigKeys.LatestBlockHousePolygon) {
      this._config.latestBlockHousePolygon = parseInt(value);
      console.info(`Set latest block House Polygon: ${value}`);
    }
  }
}

