interface ICallContractResponse {
    success: boolean;
    errorString: string;
    result: any;
}

interface ITransactionCountResponse {
    success: boolean;
    errorString: string;
    result: number | null;
}

export class BlockchainCenterApi {
    constructor(
        private readonly _baseUrl: string,
        private readonly _logPrefix: string,
    ) {
    }

    async callContract(
        network: string,
        contractAddress: string,
        abi: object[],
        methodName: string,
        args: any[],
    ): Promise<any> {
        const maxRetries = 3;
        let lastError: Error | null = null;

        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                const response = await fetch(`${this._baseUrl}/callContract`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({network, contractAddress, abi, methodName, args}),
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
                console.warn(`${this._logPrefix} [BlockchainCenterApi] callContract retry ${attempt + 1}/${maxRetries}: ${lastError.message}`);

                if (attempt < maxRetries - 1) {
                    await this.sleep(1000 * (attempt + 1));
                }
            }
        }

        console.error(`${this._logPrefix} [BlockchainCenterApi] callContract failed: ${lastError?.message}`);
        throw lastError;
    }

    async getTransactionCount(network: string, address: string): Promise<number> {
        const maxRetries = 3;
        let lastError: Error | null = null;

        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                const response = await fetch(`${this._baseUrl}/getTransactionCount`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({network, address}),
                });

                if (!response.ok) {
                    throw new Error(`HTTP error: ${response.status} ${response.statusText}`);
                }

                const data = await response.json() as ITransactionCountResponse;

                if (!data.success || data.result === null) {
                    throw new Error(`API error: ${data.errorString}`);
                }

                return data.result;
            } catch (error) {
                lastError = error as Error;
                console.warn(`${this._logPrefix} [BlockchainCenterApi] getTransactionCount retry ${attempt + 1}/${maxRetries}: ${lastError.message}`);

                if (attempt < maxRetries - 1) {
                    await this.sleep(1000 * (attempt + 1));
                }
            }
        }

        console.error(`${this._logPrefix} [BlockchainCenterApi] getTransactionCount failed: ${lastError?.message}`);
        throw lastError;
    }

    private sleep(ms: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}
