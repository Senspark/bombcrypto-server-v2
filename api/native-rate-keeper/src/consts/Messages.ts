// Redis stream contract with the game server. Must stay in step with the SmartFox side:
// server/.../constant/CachedKeys.kt (StreamKeys.SV_NATIVE_RATE_STR) and
// .../extension/schedulers/ExtensionSchedulerBnbPol.kt (publishNativeRate).

export const StreamKeys = {
  SV_NATIVE_RATE_STR: 'SV_NATIVE_RATE_STR', // server -> keeper, fire-and-forget, one message per network per tick
} as const;

/** SV_NATIVE_RATE_STR — SmartFox → keeper. */
export interface INativeRateMessage {
  network: string;        // BSC | POLYGON
  nativePerBcoin: number; // for logs only
  rateWei: string;        // native wei per 1 BCOIN, 18 decimals, decimal string — what gets written
  at: number;             // unix ms when the game server stored the rate
}
