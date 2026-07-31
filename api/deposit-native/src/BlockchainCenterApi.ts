import ILogger from './services/ILogger';

interface ICallContractResponse {
  success: boolean;
  errorString: string;
  result: unknown;
}

// Per-attempt cap so a stalled socket can't blow past the caller's own correlationId timeout.
const ATTEMPT_TIMEOUT_MS = 3_000;
const MAX_RETRIES = 3;
const BACKOFF_MS = 500;

// Thin client for the shared blockchain-center-api /callContract; `confirmations` selects the center's
// reorg-safe read depth (blockTag = latest - confirmations).
export class BlockchainCenterApi {
  private readonly _logger: ILogger;

  constructor(
    private readonly _baseUrl: string,
    logger: ILogger,
  ) {
    this._logger = logger.clone('[CENTER]');
  }

  async callContract(
    network: string,
    contractAddress: string,
    abi: object[],
    methodName: string,
    args: unknown[],
    confirmations: number,
  ): Promise<unknown> {
    const tag = `${network}.${methodName}(${args.join(',')})@-${confirmations}`;
    let lastError: Error | null = null;

    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      const startedAt = Date.now();
      try {
        const response = await fetch(`${this._baseUrl}/callContract`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({network, contractAddress, abi, methodName, args, confirmations}),
          signal: AbortSignal.timeout(ATTEMPT_TIMEOUT_MS),
        });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }
        const data = (await response.json()) as ICallContractResponse;
        if (!data.success) {
          throw new Error(`center returned success=false: ${data.errorString}`);
        }
        // Only log the slow ones — a healthy read happens on every login and would drown the log.
        const tookMs = Date.now() - startedAt;
        if (tookMs >= ATTEMPT_TIMEOUT_MS / 2 || attempt > 1) {
          this._logger.warn(`${tag} ok on attempt ${attempt}/${MAX_RETRIES} took=${tookMs}ms`);
        }
        return data.result;
      } catch (error) {
        lastError = error as Error;
        const reason = lastError.name === 'TimeoutError'
          ? `timed out after ${ATTEMPT_TIMEOUT_MS}ms`
          : lastError.message;
        this._logger.warn(`${tag} attempt ${attempt}/${MAX_RETRIES} failed (${Date.now() - startedAt}ms): ${reason}`);
        if (attempt < MAX_RETRIES) {
          await sleep(BACKOFF_MS * attempt);
        }
      }
    }

    this._logger.error(`${tag} gave up after ${MAX_RETRIES} attempts: ${lastError?.message}`);
    throw lastError;
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
