// A decoded event log as returned by the center /getLogs endpoint (raw topics + data, undecoded).
export interface ICenterLog {
    blockNumber: number;
    transactionHash: string;
    logIndex: number;
    topics: string[];
    data: string;
}

// A log inside a transaction receipt (ethers v6 Log.toJSON shape: `index`, not `logIndex`). Includes `address`
// so the ledger backfill can keep only the bridge's own logs out of a receipt that touches many contracts.
export interface ICenterReceiptLog {
    address: string;
    topics: string[];
    data: string;
    blockNumber: number;
    transactionHash?: string;
    index?: number;
    logIndex?: number;
}

// The subset of a transaction receipt the ledger backfill needs. `null` when the tx is unknown to the RPC.
export interface ICenterReceipt {
    logs: ICenterReceiptLog[];
}

// HTTP client over the shared blockchain-center-api gateway (managed multi-RPC pool + failover + egress).
// Phase 9 signer uses reads only — no key ever leaves; there is no broadcast path here.
export default interface IBlockchainCenter {
    // Read a contract view. `confirmations` > 0 reads at blockTag = latest - confirmations (reorg-safe,
    // §13.2); 0/omitted reads latest. Returns the raw center result (bigints arrive as numeric strings).
    callContract(
        network: string,
        contractAddress: string,
        abi: readonly string[],
        methodName: string,
        args: any[],
        confirmations?: number,
    ): Promise<any>;

    // Fetch event logs for an address over [fromBlock, toBlock]. `topics` follows the eth_getLogs shape
    // (topics[0] may be an array of alternative event signatures). Indexer reconcile sweep only.
    getLogs(
        network: string,
        address: string,
        topics: (string | string[] | null)[],
        fromBlock: number,
        toBlock: number,
    ): Promise<ICenterLog[]>;

    // Latest block number the gateway's RPC pool has seen for the network.
    getLatestBlockNumber(network: string): Promise<number>;

    // Unix-seconds timestamp of a block (for showing a gap's block range as a date range in the monitor).
    getBlockTimestamp(network: string, blockNumber: number): Promise<number>;

    // Full receipt of a mined tx — the ledger backfill (§18) decodes its Deposit/Withdraw logs. `null` if the
    // RPC has no receipt for the hash (unknown/dropped tx).
    getTransactionReceipt(network: string, txHash: string): Promise<ICenterReceipt | null>;
}
