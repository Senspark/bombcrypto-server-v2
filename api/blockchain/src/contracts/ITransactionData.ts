import {BigNumber} from "ethers";

export interface ITransactionData {
  blockNumber: number,
  timeStamp: Date,
  hash: string,
  nonce: number,
  blockHash: string,
  transactionIndex: number,
  from: string,
  to: string,
  value: number,
  gas: number,
  gasPrice: number,
  /**
   * "0": no Error
   */
  isError: number,
  /**
   * "1" : success
   */
  txreceipt_status: number,
  input: string,
  contractAddress: string,
  cumulativeGasUsed: number,
  gasUsed: number,
  confirmations: number,
  methodId: string,
  functionName: string
}

export interface ILogsData {
  address: string,
  topics: string[],
  data: string,
  blockNumber: BigNumber,
  blockHash: string,
  timeStamp: BigNumber,
  gasPrice: BigNumber,
  gasUsed: BigNumber,
  logIndex: BigNumber,
  transactionHash: string,
  transactionIndex: BigNumber
}