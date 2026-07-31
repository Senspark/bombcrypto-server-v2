// Canonical network keys used by this service and the game server.
export const NETWORKS = {
  BSC: 'BSC',
  POLYGON: 'POLYGON',
} as const;

// Per-network DepositNative config. This service is the authority for the contract address, chainId,
// reorg-safe read depth, and signature window — the game server never handles any of these. All values
// come from env (see Config.ts).
export interface NativeNetworkConfig {
  network: string;            // BSC | POLYGON (canonical)
  centerNetwork: string;      // what the blockchain center expects (bsc | polygon)
  depositNativeAddress: string;
  chainId: number;
  confirmations: number;      // read at latest - confirmations (BSC 5, POL 15)
  signWindowSeconds: number;  // deadline = now + signWindowSeconds
}

const CENTER_NETWORK: Record<string, string> = {
  [NETWORKS.BSC]: 'bsc',
  [NETWORKS.POLYGON]: 'polygon',
};

/** Normalize a caller-supplied network label to the canonical key, or null if unsupported. */
export function normalizeNetwork(raw: string | undefined): string | null {
  switch ((raw ?? '').toLowerCase()) {
    case 'bsc':
    case 'bnb':
      return NETWORKS.BSC;
    case 'polygon':
    case 'pol':
      return NETWORKS.POLYGON;
    default:
      return null;
  }
}

// Hardcoded per-chain constants, selected by IS_PROD. depositNativeAddress is filled in after each
// DepositNative deploy (empty = that network not yet live → requests for it error). confirmations
// match the deposit-bridge reorg depth (BSC 5 / POL 15).
const CONFIG: Record<'test' | 'prod', Record<string, {address: string; chainId: number; confirmations: number; signWindowSeconds: number}>> = {
  test: {
    [NETWORKS.BSC]: {address: '0xa32ff6Ea5805cF2D4c5c0900692EACC7d9122340', chainId: 97, confirmations: 5, signWindowSeconds: 300},
    [NETWORKS.POLYGON]: {address: '0x775861412413b8CeACf37069B3476a2C9Be8eFe3', chainId: 80002, confirmations: 15, signWindowSeconds: 300},
  },
  prod: {
    [NETWORKS.BSC]: {address: '0xd3dDD6A5559fF902E58eA1508d1F6171bB5a1eec', chainId: 56, confirmations: 5, signWindowSeconds: 300},
    [NETWORKS.POLYGON]: {address: '0xEe58fF994552c3314303459AE2D3e4Abde092e5B', chainId: 137, confirmations: 15, signWindowSeconds: 300},
  },
};

export function loadNativeConfigs(isProduction: boolean): Map<string, NativeNetworkConfig> {
  const table = isProduction ? CONFIG.prod : CONFIG.test;
  const result = new Map<string, NativeNetworkConfig>();
  for (const network of [NETWORKS.BSC, NETWORKS.POLYGON]) {
    const c = table[network];
    result.set(network, {
      network,
      centerNetwork: CENTER_NETWORK[network],
      depositNativeAddress: c.address.toLowerCase(),
      chainId: c.chainId,
      confirmations: c.confirmations,
      signWindowSeconds: c.signWindowSeconds,
    });
  }
  return result;
}
