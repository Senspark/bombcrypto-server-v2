import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

export const CHAIN_NAMES = {
    BNB: 'BSC',
    POLYGON: 'POL',
};

/** Normalize a caller-supplied network label to the canonical CHAIN_NAMES value. */
export function normalizeNetwork(raw: string | undefined, defaultNetwork: string): string {
    if (!raw) {
        return defaultNetwork;
    }
    switch (raw.toLowerCase()) {
        case 'bsc':
        case 'bnb':
            return CHAIN_NAMES.BNB;
        case 'polygon':
        case 'pol':
            return CHAIN_NAMES.POLYGON;
        default:
            return defaultNetwork;
    }
}

interface IBlockchainConfig {
    name: string;
    isProduction: boolean;
    /** Airdrop claim contract (BSC only; empty on Polygon). */
    claimAirdropAddress: string;
    /** Claim-manager contract address. Present on both networks. */
    claimManageAddress: string;
    /** BHero token address on BSC; used by Polygon to read cross-chain hero balance. Empty on BSC. */
    bheroBscAddress: string;
}

interface IChainConfigFile {
    claimAirdropAddress: string;
    claimManageAddress: string;
    bheroBscAddress: string;
}

const ADDRESS_PATTERN = /^0x[a-fA-F0-9]{40}$/;
const OPTIONAL_ADDRESS_FIELDS: (keyof IChainConfigFile)[] = [
    'claimAirdropAddress',
    'claimManageAddress',
    'bheroBscAddress',
];

const CONFIG_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'config');

function validateChainConfig(chain: string, raw: unknown): IChainConfigFile {
    if (!raw || typeof raw !== 'object') {
        throw new Error(`[BlockchainConfig] ${chain}: entry must be an object`);
    }
    const c = raw as Record<string, unknown>;

    for (const field of OPTIONAL_ADDRESS_FIELDS) {
        const value = c[field];
        if (typeof value !== 'string' || (value !== '' && !ADDRESS_PATTERN.test(value))) {
            throw new Error(`[BlockchainConfig] ${chain}.${field} must be empty string or 0x<40-hex>, got: ${JSON.stringify(value)}`);
        }
    }

    return c as unknown as IChainConfigFile;
}

function loadBlockchainConfigs(isProduction: boolean): Map<string, IBlockchainConfig> {
    const fileName = isProduction ? 'prod.json' : 'test.json';
    const filePath = path.join(CONFIG_DIR, fileName);

    let raw: string;
    try {
        raw = fs.readFileSync(filePath, 'utf-8');
    } catch (err) {
        if ((err as NodeJS.ErrnoException).code === 'ENOENT') {
            console.error(`[BlockchainConfig] Missing ${filePath}. Copy ${fileName}.example to ${fileName} and fill in the contract addresses. Starting with no blockchain configs loaded.`);
            return new Map();
        }
        throw new Error(`[BlockchainConfig] Failed to read ${filePath}: ${(err as Error).message}`);
    }

    let parsed: unknown;
    try {
        parsed = JSON.parse(raw);
    } catch (err) {
        throw new Error(`[BlockchainConfig] Failed to parse ${filePath}: ${(err as Error).message}`);
    }

    if (!parsed || typeof parsed !== 'object') {
        throw new Error(`[BlockchainConfig] ${filePath}: top-level must be an object`);
    }

    const result = new Map<string, IBlockchainConfig>();
    for (const [chainKey, chainRaw] of Object.entries(parsed as Record<string, unknown>)) {
        const validated = validateChainConfig(chainKey, chainRaw);
        result.set(chainKey, {
            name: chainKey,
            isProduction,
            claimAirdropAddress: validated.claimAirdropAddress.toLowerCase(),
            claimManageAddress: validated.claimManageAddress.toLowerCase(),
            bheroBscAddress: validated.bheroBscAddress.toLowerCase(),
        });
    }

    return result;
}

export {
    IBlockchainConfig,
    loadBlockchainConfigs,
};
