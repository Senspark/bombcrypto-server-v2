import {IBlockchainConfig} from './BlockchainConfig';
import {IExtendedDecodedDetails, IShortDetails} from "./IDecodedDetails";
import HeroSApi from "./contracts/HeroSApi";
import {StakeAmount} from "./IHeroStakeApi";

export interface IBlockchainApi {
  config: IBlockchainConfig

  heroSApi: HeroSApi;

  getLatestBlock(): Promise<number>

  getLogs(fromBlock: number, toBlock: number, address: string, topic: string): Promise<any[]>

  getTotalCoins(): Promise<number>;
  getCoinBalance(address: string): Promise<number>

  getHeroOwner(id: number): Promise<string>
  getHeroTokenIdCounter(): Promise<number>

  getHeroStakedAmount(heroId: number): Promise<StakeAmount>
  refreshHeroStakedAmount(heroId: number, uid?: number): Promise<StakeAmount>
  
  getHeroDetails(options: any): Promise<any[]>
  getDecodedHeroDetails(options: any): Promise<IExtendedDecodedDetails[] | IShortDetails[]>

  getHouseOwner(id: number): Promise<string>
  getHouseTokenIdCounter(): Promise<number>
  getHouseDetails(options: any): Promise<any[]>
  getDecodedHouseDetails(options: any): Promise<any[]>

  getDepositedV3(address: string): Promise<number[]>
  getDepositedV2(address: string): Promise<number[]>

  getTokenList(): Promise<string[]>
}