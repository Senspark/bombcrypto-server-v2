import ILogger from "../services/ILogger";
import IBlockchainCenter from "../services/IBlockchainCenter";
import {Chain, CENTER_NETWORK, CONFIRMED_DEPTH, DEPOSIT_BRIDGE_ABI, INetworkConfig} from "../consts/Consts";

interface ICacheEntry {
    value: bigint;
    expiresAt: number;
}

// The ISOLATED read path (residual §4.2) — the ONLY server-trusted number in the whole system. It reads
// `deposited[user][token]` on the SOURCE chain at confirmed depth, bounds it ≤ `totalDeposited(source)`,
// and caches per-wallet for a few seconds (monotonic-safe; anti-spam). Fail-closed: any RPC error throws,
// so the caller does NOT sign. Deliberately shares no state with any other logic — one bug can't reach it.
export default class DepositReader {
    readonly #logger: ILogger;
    readonly #center: IBlockchainCenter;
    readonly #configs: Map<Chain, INetworkConfig>;
    readonly #cacheTtlMs: number;
    readonly #cache = new Map<string, ICacheEntry>();

    constructor(logger: ILogger, center: IBlockchainCenter, configs: Map<Chain, INetworkConfig>, cacheTtlMs: number) {
        this.#logger = logger.clone('[Reader]');
        this.#center = center;
        this.#configs = configs;
        this.#cacheTtlMs = cacheTtlMs;
    }

    // Returns the source-chain deposited counter to be signed as `otherDeposited`.
    async readOtherDeposited(srcChain: Chain, userWallet: string, tokenS: string): Promise<bigint> {
        const key = `${srcChain}:${userWallet.toLowerCase()}:${tokenS.toLowerCase()}`;
        const now = Date.now();
        const cached = this.#cache.get(key);
        if (cached && cached.expiresAt > now) return cached.value;

        const network = CENTER_NETWORK[srcChain];
        const bridge = this.#config(srcChain).bridgeAddress;
        const depth = CONFIRMED_DEPTH[srcChain];

        const depositedRaw = await this.#center.callContract(
            network, bridge, DEPOSIT_BRIDGE_ABI, 'deposited', [userWallet, tokenS], depth,
        );
        const totalRaw = await this.#center.callContract(
            network, bridge, DEPOSIT_BRIDGE_ABI, 'totalDeposited', [tokenS], depth,
        );

        const deposited = BigInt(depositedRaw);
        const total = BigInt(totalRaw);

        // PREVENT (§4.2 / test 9-1): a read bug returning more than the whole source pool is impossible
        // on-chain → refuse to sign rather than authorize an over-withdraw. Also caps bot bypass at signer.
        if (deposited > total) {
            throw new Error(`deposited ${deposited} exceeds totalDeposited ${total} on ${srcChain} — refusing to sign`);
        }

        this.#cache.set(key, {value: deposited, expiresAt: now + this.#cacheTtlMs});
        return deposited;
    }

    #config(chain: Chain): INetworkConfig {
        const cfg = this.#configs.get(chain);
        if (!cfg) throw new Error(`no network config for ${chain}`);
        return cfg;
    }
}
