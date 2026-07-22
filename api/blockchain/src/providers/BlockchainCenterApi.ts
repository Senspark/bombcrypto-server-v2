interface ILogEntry {
    blockNumber: number;
    transactionHash: string;
    data: string;
}

interface IGetLogsResponse {
    success: boolean;
    errorString: string;
    result: ILogEntry[];
}

interface ILatestBlockResponse {
    success: boolean;
    errorString: string;
    result: number;
}

interface ITransactionResult {
    blockNumber: number;
    from: string;
    to: string | null;
    data: string;
    hash: string;
    value: string;
}

interface ITransactionResponse {
    success: boolean;
    errorString: string;
    result: ITransactionResult | null;
}

interface ITransactionReceiptLog {
    address: string;
    topics: string[];
    data: string;
    blockNumber: number;
    transactionHash: string;
}

interface ITransactionReceiptResult {
    status: number;
    blockNumber: number;
    from: string;
    to: string | null;
    logs: ITransactionReceiptLog[];
}

interface ITransactionReceiptResponse {
    success: boolean;
    errorString: string;
    result: ITransactionReceiptResult | null;
}

interface ICallContractResponse {
    success: boolean;
    errorString: string;
    result: any;
}

export class BlockchainCenterApi {
    constructor(
        private readonly _baseUrl: string,
        private readonly _logPrefix: string
    ) {}

    async getLogs(
        network: string,
        address: string,
        topics: string[],
        fromBlock: number,
        toBlock: number
    ): Promise<ILogEntry[]> {
        const maxRetries = 3;
        let lastError: Error | null = null;
        const ctx = `getLogs(${network}, ${address}, ${fromBlock}-${toBlock})`;

        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                const response = await fetch(`${this._baseUrl}/getLogs`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        network,
                        address,
                        topics,
                        fromBlock,
                        toBlock,
                    }),
                });

                if (!response.ok) {
                    throw new Error(`HTTP error: ${response.status} ${response.statusText}`);
                }

                const data = await response.json() as IGetLogsResponse;

                if (!data.success) {
                    throw new Error(`API error: ${data.errorString}`);
                }

                return data.result;
            } catch (error) {
                lastError = error as Error;
                const waitTime = 1000 * (attempt + 1); // 1s, 2s, 3s
                console.warn(`${this._logPrefix} [BlockchainCenterApi] ${ctx} retry ${attempt + 1}/${maxRetries}: ${lastError.message}`);

                if (attempt < maxRetries - 1) {
                    await this.sleep(waitTime);
                }
            }
        }

        console.error(`${this._logPrefix} [BlockchainCenterApi] ${ctx} failed: ${lastError?.message}`);
        throw lastError;
    }

    async getLatestBlockNumber(network: string): Promise<number | null> {
        const maxRetries = 3;
        let lastError: Error | null = null;
        const ctx = `getLatestBlockNumber(${network})`;

        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                const response = await fetch(
                    `${this._baseUrl}/latestBlockNumber?network=${network}`
                );

                if (!response.ok) {
                    throw new Error(`HTTP error: ${response.status} ${response.statusText}`);
                }

                const data = await response.json() as ILatestBlockResponse;

                if (!data.success) {
                    throw new Error(`API error: ${data.errorString}`);
                }

                return data.result;
            } catch (error) {
                lastError = error as Error;
                console.warn(`${this._logPrefix} [BlockchainCenterApi] ${ctx} retry ${attempt + 1}/${maxRetries}: ${lastError.message}`);

                if (attempt < maxRetries - 1) {
                    await this.sleep(1000 * (attempt + 1));
                }
            }
        }

        console.error(`${this._logPrefix} [BlockchainCenterApi] ${ctx} failed: ${lastError?.message}`);
        return null;
    }

    async getTransaction(network: string, txHash: string): Promise<ITransactionResult | null> {
        const maxRetries = 3;
        let lastError: Error | null = null;
        const ctx = `getTransaction(${network}, ${txHash})`;

        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                const response = await fetch(`${this._baseUrl}/getTransaction`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ network, txHash }),
                });

                if (!response.ok) {
                    throw new Error(`HTTP error: ${response.status} ${response.statusText}`);
                }

                const data = await response.json() as ITransactionResponse;

                if (!data.success) {
                    throw new Error(`API error: ${data.errorString}`);
                }

                return data.result;
            } catch (error) {
                lastError = error as Error;
                console.warn(`${this._logPrefix} [BlockchainCenterApi] ${ctx} retry ${attempt + 1}/${maxRetries}: ${lastError.message}`);

                if (attempt < maxRetries - 1) {
                    await this.sleep(1000 * (attempt + 1));
                }
            }
        }

        console.error(`${this._logPrefix} [BlockchainCenterApi] ${ctx} failed: ${lastError?.message}`);
        return null;
    }

    async getTransactionReceipt(network: string, txHash: string): Promise<ITransactionReceiptResult | null> {
        const maxRetries = 3;
        let lastError: Error | null = null;
        const ctx = `getTransactionReceipt(${network}, ${txHash})`;

        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                const response = await fetch(`${this._baseUrl}/getTransactionReceipt`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ network, txHash }),
                });

                if (!response.ok) {
                    throw new Error(`HTTP error: ${response.status} ${response.statusText}`);
                }

                const data = await response.json() as ITransactionReceiptResponse;

                if (!data.success) {
                    throw new Error(`API error: ${data.errorString}`);
                }

                return data.result;
            } catch (error) {
                lastError = error as Error;
                console.warn(`${this._logPrefix} [BlockchainCenterApi] ${ctx} retry ${attempt + 1}/${maxRetries}: ${lastError.message}`);

                if (attempt < maxRetries - 1) {
                    await this.sleep(1000 * (attempt + 1));
                }
            }
        }

        console.error(`${this._logPrefix} [BlockchainCenterApi] ${ctx} failed: ${lastError?.message}`);
        return null;
    }

    async callContract(
        network: string,
        contractAddress: string,
        abi: object[],
        methodName: string,
        args: any[]
    ): Promise<any> {
        const maxRetries = 3;
        let lastError: Error | null = null;
        const ctx = `callContract(${network}, ${methodName} @ ${contractAddress})`;

        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                const response = await fetch(`${this._baseUrl}/callContract`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        network,
                        contractAddress,
                        abi,
                        methodName,
                        args,
                    }),
                });

                if (!response.ok) {
                    throw new Error(`HTTP error: ${response.status} ${response.statusText}`);
                }

                const data = await response.json() as ICallContractResponse;

                if (!data.success) {
                    throw new Error(`API error: ${data.errorString}`);
                }

                return data.result;
            } catch (error) {
                lastError = error as Error;
                console.warn(`${this._logPrefix} [BlockchainCenterApi] ${ctx} retry ${attempt + 1}/${maxRetries}: ${lastError.message}`);

                if (attempt < maxRetries - 1) {
                    await this.sleep(1000 * (attempt + 1));
                }
            }
        }

        console.error(`${this._logPrefix} [BlockchainCenterApi] ${ctx} failed: ${lastError?.message}`);
        throw lastError;
    }

    private sleep(ms: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

export { ILogEntry, ITransactionResult, ITransactionReceiptResult };
