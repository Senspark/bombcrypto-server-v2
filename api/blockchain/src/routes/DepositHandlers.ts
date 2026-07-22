import {Request, Response} from "express";
import {IBlockchainApi} from "../IBlockchainApi";
import * as ErrorCode from "../ErrorCode";
import IRedisDatabase from "../cache/IRedisDatabase";
import {StreamKeys} from "../cache/CachedKeys";
import IMessengerService from "../cache/IMessengerService";
import ILogger from "../services/ILogger";

let TokenList = new Map<IBlockchainApi, string[] | undefined>();

async function getTokenList(api: IBlockchainApi): Promise<string[]> {
  if (TokenList.has(api)) {
    const list = TokenList.get(api);
    if (list && list.length > 0) {
      return list;
    }
  }

  console.log(`Fetching token list for ${api.config.name}...`);
  let list = await api.getTokenList();
  if (list && list.length > 0) {
    console.log(`Fetched token list for ${api.config.name}:`, list.join(', '));
    TokenList.set(api, list);
  }
  return list;
}

/**
 * Trước mắt sẽ cache theo kiểu Stale-While-Revalidate
 * Tức khi cache có data cũ thì response về ngay. Rồi sau đó cứ update data mới sau.
 * Từ từ có giải pháp tốt hơn sau
 */
export class DepositHandlers {

  constructor(
    private readonly messenger: IMessengerService,
    private readonly logger: ILogger,
    private readonly apis: IBlockchainApi[],
  ) {
    this.initialize().then();
  }

  async initialize() {
    try {
      for (const api of this.apis) {
        getTokenList(api).then()
      }
    } catch (e) {
      console.error(`Failed to initialize DepositHandlers: ${e.message}`, e.stack?.split(`\n`));
    }
  }

  async getDepositedV3(req: Request, res: Response) {
    try {
      const locals = res.locals;
      const blockchainApi: IBlockchainApi = locals.api;
      const query = req.query;
      const address = query.address as string;
      const userId = query.uid && parseInt(query.uid as string) as number;

      const tokenList = await getTokenList(blockchainApi);

      const depositedBalances = await blockchainApi.getDepositedV3(address);

      if (tokenList.length !== depositedBalances.length) {
        console.error(`Token list length (${tokenList.length}) does not match deposited balances length (${depositedBalances.length})`);
        res.status(400).send({
          code: ErrorCode.CODE_INTERNAL,
          message: "Blockchain data invalid",
        });
      }
      const mappers: { [key: string]: number } = {};
      tokenList.forEach((token, index) => {
        const type = blockchainApi.config.addressToSymbol[token];
        const value = depositedBalances[index];
        mappers[type] ??= 0;
        mappers[type] += value;
      });
      const result = Object.entries(mappers).map(([type, value]) => ({
        type,
        value,
      }));
      res.send({
        code: ErrorCode.CODE_OK,
        message: result,
      });

      if (userId) {
        this.messenger.send(StreamKeys.AP_BL_SYNC_DEPOSIT, {
          uid: userId,
          type: (query.network as string)?.toUpperCase(),
          data: result,
        }).catch(e => {
          this.logger.error(e);
        });
      }

    } catch (e) {
      console.error(e.message, e.stack?.split(`\n`));
      res.status(e.httpCode ?? 500).send({
        code: e.code ?? ErrorCode.CODE_INTERNAL,
        message: e.message,
      });
    }
  }

}