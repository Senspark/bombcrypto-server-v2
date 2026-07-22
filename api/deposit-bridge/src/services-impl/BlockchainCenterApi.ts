import ILogger from "../services/ILogger";
import IBlockchainCenter, {ICenterLog, ICenterReceipt} from "../services/IBlockchainCenter";

interface ICenterResponse<T> {
    success: boolean;
    errorString: string;
    result: T;
}

const MAX_RETRIES = 3;
// A receipt for a tx we KNOW exists (it came from an explorer export) can still come back null when the
// round-robin lands on a pruned RPC — and the gateway does not retry on null, only on throw. Re-post so the
// pool round-robins to another (hopefully archive) RPC.
const RECEIPT_NULL_RETRIES = 4;

export default class BlockchainCenterApi implements IBlockchainCenter {
    readonly #baseUrl: string;
    readonly #logger: ILogger;

    constructor(baseUrl: string, logger: ILogger) {
        if (!baseUrl) throw new Error("BLOCKCHAIN_CENTER_API is required");
        this.#baseUrl = baseUrl.replace(/\/+$/, '');
        this.#logger = logger.clone('[Center]');
    }

    async callContract(
        network: string,
        contractAddress: string,
        abi: readonly string[],
        methodName: string,
        args: any[],
        confirmations = 0,
    ): Promise<any> {
        return this.post<any>('/callContract', {network, contractAddress, abi, methodName, args, confirmations});
    }

    async getLogs(
        network: string,
        address: string,
        topics: (string | string[] | null)[],
        fromBlock: number,
        toBlock: number,
    ): Promise<ICenterLog[]> {
        return this.post<ICenterLog[]>('/getLogs', {network, address, topics, fromBlock, toBlock});
    }

    async getLatestBlockNumber(network: string): Promise<number> {
        return this.get<number>(`/latestBlockNumber?network=${encodeURIComponent(network)}`);
    }

    async getBlockTimestamp(network: string, blockNumber: number): Promise<number> {
        return this.post<number>('/getBlockTimestamp', {network, blockNumber});
    }

    async getTransactionReceipt(network: string, txHash: string): Promise<ICenterReceipt | null> {
        for (let attempt = 1; attempt <= RECEIPT_NULL_RETRIES; attempt++) {
            const receipt = await this.post<ICenterReceipt | null>('/getTransactionReceipt', {network, txHash});
            if (receipt && Array.isArray(receipt.logs)) return receipt;
            this.#logger.warn(`/getTransactionReceipt null for ${txHash} (attempt ${attempt}/${RECEIPT_NULL_RETRIES}) — retrying on another RPC`);
            if (attempt < RECEIPT_NULL_RETRIES) await this.sleep(500 * attempt);
        }
        return null;
    }

    // Shared POST with retry + backoff. Throws on non-2xx or success=false after exhausting retries.
    private async post<T>(path: string, body: object): Promise<T> {
        let lastError: Error | null = null;

        for (let attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                const response = await fetch(`${this.#baseUrl}${path}`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(body),
                });

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status} ${response.statusText}`);
                }

                const data = await response.json() as ICenterResponse<T>;
                if (!data.success) {
                    throw new Error(`center error: ${data.errorString}`);
                }

                return data.result;
            } catch (e) {
                lastError = e as Error;
                this.#logger.warn(`${path} retry ${attempt + 1}/${MAX_RETRIES}: ${lastError.message}`);
                if (attempt < MAX_RETRIES - 1) await this.sleep(1000 * (attempt + 1));
            }
        }

        throw lastError ?? new Error(`${path} failed`);
    }

    // Shared GET with the same retry + backoff and {success,result} envelope as post().
    private async get<T>(path: string): Promise<T> {
        let lastError: Error | null = null;

        for (let attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                const response = await fetch(`${this.#baseUrl}${path}`);
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status} ${response.statusText}`);
                }
                const data = await response.json() as ICenterResponse<T>;
                if (!data.success) {
                    throw new Error(`center error: ${data.errorString}`);
                }
                return data.result;
            } catch (e) {
                lastError = e as Error;
                this.#logger.warn(`${path} retry ${attempt + 1}/${MAX_RETRIES}: ${lastError.message}`);
                if (attempt < MAX_RETRIES - 1) await this.sleep(1000 * (attempt + 1));
            }
        }

        throw lastError ?? new Error(`${path} failed`);
    }

    private sleep(ms: number): Promise<void> {
        return new Promise((resolve) => setTimeout(resolve, ms));
    }
}
