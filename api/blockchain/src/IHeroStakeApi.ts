import {BigNumber} from "ethers";

export interface IHeroStakeApi {
  name: string;

  getStakedAmount(heroId: number): Promise<StakeAmount>;

  getStakedAmounts(...heroIds: number[]): Promise<StakeAmount[]>;

  getStakedAmountsBig(...heroIds: number[]): Promise<StakeAmountBig[]>;
  
  setStakedAmount(heroId: number, amount: StakeAmountBig): Promise<StakeAmount>;

  broadcastStakedAmount(heroId: number, amount: StakeAmountBig, uid?: number): Promise<void>;
}

export type StakeAmount = [bcoin: number, sen: number];
export type StakeAmountBn = [bcoin: bigint, sen: bigint];
export type StakeAmountBig = [bcoin: BigNumber, sen: BigNumber];