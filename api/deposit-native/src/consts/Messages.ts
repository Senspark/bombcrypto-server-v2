// Redis stream contract with the game server. Must stay in step with the SmartFox side:
// server/.../constant/CachedKeys.kt (StreamKeys) and .../manager/nativeDeposit/dto/NativeDepositDtos.kt.
// Wei quantities travel as decimal strings end-to-end — a 1-wei rounding breaks signature verification.

export const StreamKeys = {
  SV_DEPNATIVE_REQUEST_STR: 'SV_DEPNATIVE_REQUEST_STR', // server -> signer
  AP_DEPNATIVE_RESULT_STR: 'AP_DEPNATIVE_RESULT_STR',   // signer -> server, by correlationId
} as const;

export const RequestKind = {
  COUNTERS: 'counters',
  WITHDRAW_SIGN: 'withdraw-sign',
} as const;

export type RequestKindValue = (typeof RequestKind)[keyof typeof RequestKind];

/** SV_DEPNATIVE_REQUEST_STR — SmartFox → signer. */
export interface INativeRequest {
  correlationId: string;
  kind: RequestKindValue;
  uid: number;
  wallet: string;             // user EOA
  network: string;            // BSC | POLYGON
  allowedCumulative?: string; // withdraw-sign only: exact wei, already committed by the game server
}

/** AP_DEPNATIVE_RESULT_STR — signer → SmartFox (matched by correlationId). */
export interface INativeResult {
  correlationId: string;
  code: number;             // 0 ok, 100 error
  errorMessage?: string;    // client-safe text only (never raw exception / RPC strings)
  deposited?: string;       // counters: cumulative deposited wei
  withdrawn?: string;       // counters: cumulative withdrawn wei
  signature?: string;       // withdraw-sign: EIP-191 over (user, allowed, deadline, contract, chainId)
  deadline?: number;        // unix seconds
  contractAddress?: string;
  chainId?: number;
}

export const CODE_OK = 0;
export const CODE_ERROR = 100;

// What the game server is allowed to show a player. Every real cause is logged here instead.
export const SAFE_ERROR = 'Unable to process your request right now. Please try again later.';
