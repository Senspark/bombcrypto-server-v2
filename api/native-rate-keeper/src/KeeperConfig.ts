// Canonical network keys, same as the game server's EnumConstants.DataType names.
export const NETWORKS = {
  BSC: 'BSC',
  POLYGON: 'POLYGON',
} as const;

export interface KeeperNetworkConfig {
  network: string;      // BSC | POLYGON
  rpcUrl: string;
  chainId: number;
  // The BHero token, not HeroDesign: the design address is read from BHero.design() at startup, so a
  // redeployed design is picked up without touching this table.
  bheroAddress: string;
}

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

// Same BHero addresses the client uses (unity-web-template BscAddress.ts / PolygonAddress.ts).
const CONFIG: Record<'test' | 'prod', Record<string, {bhero: string; chainId: number}>> = {
  test: {
    [NETWORKS.BSC]: {bhero: '0xC1A4C06426B4Df799E455964A20FDe866E86fbd1', chainId: 97},
    [NETWORKS.POLYGON]: {bhero: '0xF9f21032bcCCe8997bB29Ab9FBE19502191B7596', chainId: 80002},
  },
  prod: {
    [NETWORKS.BSC]: {bhero: '0x30cc0553f6fa1faf6d7847891b9b36eb559dc618', chainId: 56},
    [NETWORKS.POLYGON]: {bhero: '0xd8a06936506379dbBe6e2d8aB1D8C96426320854', chainId: 137},
  },
};

export function loadKeeperConfigs(isProduction: boolean, rpcUrls: Record<string, string>): Map<string, KeeperNetworkConfig> {
  const table = isProduction ? CONFIG.prod : CONFIG.test;
  const result = new Map<string, KeeperNetworkConfig>();
  for (const network of [NETWORKS.BSC, NETWORKS.POLYGON]) {
    const c = table[network];
    result.set(network, {
      network,
      rpcUrl: rpcUrls[network],
      chainId: c.chainId,
      bheroAddress: c.bhero,
    });
  }
  return result;
}
