// Central constants: chains, on-chain config, ABI, stream keys, enums.
// Phase 9: the service is a STATELESS SIGNER — it reads source-chain `deposited` and signs an EIP-191
// withdraw authorization; the user relays it themselves. No tx submission, no DB, no pending state.
// Wei everywhere is exact integer (bigint / numeric-string over the wire) — never a JS number.

export enum Chain {
    BSC = "BSC",
    POLYGON = "POLYGON",
}

export enum TokenSymbol {
    BCOIN = "BCOIN",
    SEN = "SEN",
}

// reward_type (BCOIN_BRIDGE | SEN_BRIDGE) → token symbol on-chain.
export const REWARD_TYPE_TO_TOKEN: Record<string, TokenSymbol> = {
    BCOIN_BRIDGE: TokenSymbol.BCOIN,
    SEN_BRIDGE: TokenSymbol.SEN,
};

// Bridge is directional: deposit on chain X credits withdraw capacity on the OTHER chain.
export function oppositeChain(chain: Chain): Chain {
    return chain === Chain.BSC ? Chain.POLYGON : Chain.BSC;
}

export interface INetworkConfig {
    chain: Chain;
    chainId: number;
    bridgeAddress: string;
    tokens: Record<TokenSymbol, string>;
    explorerTxUrl: string; // tx-link base, e.g. https://bscscan.com/tx/
}

// Chain → blockchain-center-api network key (see utils/network.js normalizeNetwork). All RPC I/O
// routes through the center gateway; ap-deposit-bridge never opens a JsonRpcProvider itself.
export const CENTER_NETWORK: Record<Chain, string> = {
    [Chain.BSC]: "bsc",
    [Chain.POLYGON]: "polygon",
};

// Confirmed depth for the signer's SOURCE-chain `deposited` read (§13.2 / test 3-7, 9-4): read at
// latest-N so a value about to reorg out cannot inflate `otherDeposited`. N small per-network — full
// nodes retain ~128 recent blocks, so no archive node is needed. Ops-tunable.
export const CONFIRMED_DEPTH: Record<Chain, number> = {
    [Chain.BSC]: 5,
    [Chain.POLYGON]: 15,
};

// Per-environment on-chain config. IS_PROD (see EnvConfig) selects the whole block:
// true → mainnet (BSC 56 / Polygon 137), false → testnet (BSC 97 / Amoy 80002). The prod
// blockchain-center-api only holds mainnet RPCs, so CENTER_NETWORK keys stay 'bsc'/'polygon'.
export const NETWORKS_TESTNET: Record<Chain, INetworkConfig> = {
    [Chain.BSC]: {
        chain: Chain.BSC,
        chainId: 97,
        bridgeAddress: "0xb34e148821082C7c73f094Be1E194b3474209bba",
        tokens: {
            [TokenSymbol.BCOIN]: "0x648a9cf8e95c73110d28e7e2329b2d0910bd36b8",
            [TokenSymbol.SEN]: "0x4B5828F31550aFe15C61D7a765D9597ad4282325",
        },
        explorerTxUrl: "https://testnet.bscscan.com/tx/",
    },
    [Chain.POLYGON]: {
        chain: Chain.POLYGON,
        chainId: 80002, // Amoy
        bridgeAddress: "0x97D80e2914bcBd6F957eE804c7B6fe844A0A1cd7",
        tokens: {
            [TokenSymbol.BCOIN]: "0xcF693b54F86c49bbBa54Ff887488Bbf84C5D05BF",
            [TokenSymbol.SEN]: "0x93567522610828695F36178b180989996082404A",
        },
        explorerTxUrl: "https://amoy.polygonscan.com/tx/",
    },
};

export const NETWORKS_MAINNET: Record<Chain, INetworkConfig> = {
    [Chain.BSC]: {
        chain: Chain.BSC,
        chainId: 56,
        bridgeAddress: "0xC6aC72C83f72e7F86B7de91f5733c266Fc4F2273",
        tokens: {
            [TokenSymbol.BCOIN]: "0x00e1656e45f18ec6747F5a8496Fd39B50b38396D",
            [TokenSymbol.SEN]: "0xb43Ac9a81eDA5a5b36839d5b6FC65606815361b0",
        },
        explorerTxUrl: "https://bscscan.com/tx/",
    },
    [Chain.POLYGON]: {
        chain: Chain.POLYGON,
        chainId: 137,
        bridgeAddress: "0xD84E8aCAcE2Bddb2Da8975340E83A165dA51FFc3",
        tokens: {
            [TokenSymbol.BCOIN]: "0xB2C63830D4478cB331142FAc075A39671a5541dC",
            [TokenSymbol.SEN]: "0xFe302B8666539d5046cd9aA0707bB327F5f94C22",
        },
        explorerTxUrl: "https://polygonscan.com/tx/",
    },
};

// ethers v6 human-readable ABI. The signer only READS the source-chain deposited counters; the contract
// self-computes the payable amount, so no writer and no other getters are needed here. Kept minimal and
// separate from the indexer ABI so the isolated signing read path shares no surface with anything.
export const DEPOSIT_BRIDGE_ABI = [
    "function deposited(address user, address token) view returns (uint256)",
    "function totalDeposited(address token) view returns (uint256)",
];

// Richer ABI used ONLY by the Indexer/Monitor module (read-only report path, never the signer). Adds the
// remaining counters, the enable flags, and the Deposit/Withdraw events replayed by the reconcile sweep.
export const BRIDGE_INDEXER_ABI = [
    "function deposited(address user, address token) view returns (uint256)",
    "function withdrawn(address user, address token) view returns (uint256)",
    "function totalDeposited(address token) view returns (uint256)",
    "function totalWithdrawn(address token) view returns (uint256)",
    "function collectedFees(address token) view returns (uint256)",
    "function sweptFees(address token) view returns (uint256)",
    "function totalFunded(address token) view returns (uint256)",
    "function feePercent() view returns (uint256)",
    "function depositEnabled() view returns (bool)",
    "function withdrawEnabled() view returns (bool)",
    "event Deposit(address indexed user, address indexed token, uint256 amount, uint256 depositedTotal)",
    "event Withdraw(address indexed user, address indexed token, uint256 amount, uint256 net, uint256 otherDeposited, uint256 deadline, uint256 withdrawnTotal)",
];

// Legacy Phase 8 Withdraw event (different signature → different topic0 than the Phase 9 event above). Both the
// old `withdraw` and the removed relayer `withdrawTo` emitted THIS event with `user` as the indexed beneficiary,
// so the historical ledger (§18) recovers the real user even for relayer-submitted `withdrawTo` txs. Only the
// ledger backfill decodes it — the forward sweep only ever sees new Phase 9 events.
export const BRIDGE_LEGACY_EVENT_ABI = [
    "event Withdraw(address indexed user, address indexed token, uint256 gross, uint256 net, uint256 withdrawnTotal)",
];

// DepositBridge has NO balanceOf of its own — liquidity is the ERC20 balance of the bridge address.
export const ERC20_ABI = [
    "function balanceOf(address account) view returns (uint256)",
];

// getLogs window per sweep chunk — RPC providers cap the block range of a single eth_getLogs.
export const INDEXER_BLOCK_STEP = 499;

// Reverse-lookup a token symbol from its on-chain address for a chain (indexer decodes events by address).
export function symbolOfToken(cfg: INetworkConfig, tokenAddress: string): TokenSymbol | null {
    const addr = tokenAddress.toLowerCase();
    for (const sym of Object.values(TokenSymbol)) {
        if (cfg.tokens[sym].toLowerCase() === addr) return sym;
    }
    return null;
}

// Resolve the per-chain network config for the running environment (IS_PROD → mainnet, else testnet).
export function buildNetworkConfigs(isProduction: boolean): Map<Chain, INetworkConfig> {
    const base = isProduction ? NETWORKS_MAINNET : NETWORKS_TESTNET;
    const configs = new Map<Chain, INetworkConfig>();
    for (const chain of Object.values(Chain)) {
        configs.set(chain, {...base[chain]});
    }
    return configs;
}

// Redis streams (envelope: XADD <key> * data <json-string>; correlate via correlationId).
export const StreamKeys = {
    // SmartFox → signer (withdraw-sign request).
    SV_DEPBRIDGE_REQUEST_STR: "SV_DEPBRIDGE_REQUEST_STR",
    // signer → SmartFox (result, matched by correlationId). No PUSH stream: signing is synchronous, no late leg.
    AP_DEPBRIDGE_RESULT_STR: "AP_DEPBRIDGE_RESULT_STR",
    // SmartFox → indexer (bridge activity feed, fire-and-forget). A report optimization, never a safety gate:
    // each notify just kicks the sweeper into fast mode so it drains to head right when activity happens;
    // anything missed is still fully covered by the idle getLogs sweep.
    SV_DEPBRIDGE_NOTIFY_STR: "SV_DEPBRIDGE_NOTIFY_STR",
};

// The four bridge activities SmartFox reports. The two *_prepare/request kinds fire before the tx is mined
// (warm the sweeper up); the two *_done kinds fire after success (catch the just-landed tx once it reaches
// confirmed depth). The indexer treats all four identically — accelerate the sweeper for `chain`.
export enum NotifyKind {
    DEPOSIT_PREPARE = "deposit_prepare",
    DEPOSIT_DONE = "deposit_done",
    WITHDRAW_REQUEST = "withdraw_request",
    WITHDRAW_DONE = "withdraw_done",
}

// SV_DEPBRIDGE_NOTIFY_STR payload. SmartFox always sends the FULL shape (wallet from the session, never the
// client) so a future backend can index per-wallet activity without a SmartFox redeploy — SF can't redeploy
// cheaply (it drops player connections) whereas the backend can. Today the consumer only reads `chain`.
export interface IBridgeNotify {
    kind: string;        // NotifyKind
    wallet: string;
    chain: string;       // BSC | POLYGON — the chain the on-chain action happens on
    rewardType: string;  // BCOIN_BRIDGE | SEN_BRIDGE
    txHash?: string;     // present on the *_done kinds (client has it after the tx succeeds)
    ts?: number;         // SmartFox-side unix ms when the activity was reported
}

// Plain Redis keys (GET/SET, not streams) — the monitor's on-chain snapshots are cached here. The aggregate is
// cached PER (chain, token) cell (+ per-chain meta) so the sweep can invalidate just the one cell that changed
// instead of the whole table; the frontend then re-reads only that cell on its next poll. Prefix PROJECT:MODULE:NAME.
export const RedisKeys = {
    // Per-cell aggregate: `${prefix}${chain}:${symbol}` → one ITokenCell snapshot (the 6 RPC-read counters).
    MONITOR_CELL_PREFIX: "AP_DEPBRIDGE:MONITOR:CELL:",
    // Per-chain meta: `${prefix}${chain}` → enable flags + feePercent (unchanged by deposit/withdraw).
    MONITOR_CHAINMETA_PREFIX: "AP_DEPBRIDGE:MONITOR:CHAINMETA:",
    MONITOR_INDEXER_STATUS: "AP_DEPBRIDGE:MONITOR:INDEXER_STATUS",
    // One HASH per network (`${prefix}${network}`), field = block number, value = timestamp. Block timestamps
    // are immutable → cached forever (no TTL); a hash keeps the many entries out of the top-level keyspace.
    MONITOR_BLOCKTIME_PREFIX: "AP_DEPBRIDGE:MONITOR:BLOCKTIME:",
};

export const monitorCellKey = (chain: Chain, symbol: TokenSymbol): string =>
    `${RedisKeys.MONITOR_CELL_PREFIX}${chain}:${symbol}`;

export const monitorChainMetaKey = (chain: Chain): string =>
    `${RedisKeys.MONITOR_CHAINMETA_PREFIX}${chain}`;

export enum RequestKind {
    WITHDRAW = "withdraw",
}

export const CODE_OK = 0;
export const CODE_ERROR = 100;

// Client-facing error text. NEVER return raw exception / RPC error strings to the client. The real error
// is always logged server-side; the client only sees these.
export const ClientErrors = {
    INVALID_REQUEST: "Invalid withdraw request.",
    NOTHING_TO_WITHDRAW: "You have nothing to withdraw on this network.",
    WITHDRAW_UNAVAILABLE: "Withdraw is temporarily unavailable. Please try again later.",
} as const;
