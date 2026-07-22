import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

export const CHAIN_NAMES = {
    BNB: 'BSC',
    POLYGON: 'POL',
};

export function normalizeNetwork(raw: string | undefined | null): string | null {
    if (!raw) return null;
    switch (raw.toLowerCase()) {
        case 'bsc':
        case 'bnb':
            return CHAIN_NAMES.BNB;
        case 'pol':
        case 'polygon':
            return CHAIN_NAMES.POLYGON;
        default:
            return null;
    }
}

// Kotlin `EnumConstants.DataType` consumes V4 sync streams via `DataType.valueOf(type)` —
// `BSC` matches, but internal `POL` does not (Kotlin enum is `POLYGON`). Convert at the
// publish boundary so the wire format stays aligned with the consumer enum name.
export function networkToStreamType(network: string): string {
    switch (network) {
        case CHAIN_NAMES.BNB:
            return 'BSC';
        case CHAIN_NAMES.POLYGON:
            return 'POLYGON';
        default:
            return network;
    }
}

interface IBlockchainConfig {
    name: string;
    isProduction: boolean;
    apiUrl: string;
    explorerUrl: string;
    bcoinTokenAddress: string;
    bheroTokenAddress: string;
    bheroSAddress: string;
    bheroStakeAddress: string;
    bhouseTokenAddress: string;
    depositAddress: string;
    claimAddress: string;
    lockedAddresses: string[];
    oldSenTokenAddress: string;
    senTokenAddress: string;
    addressToSymbol: { [key: string]: string };
}

interface IChainConfigFile {
    apiUrl: string;
    explorerUrl: string;
    bcoinTokenAddress: string;
    bheroTokenAddress: string;
    bheroSAddress: string;
    bheroStakeAddress: string;
    bhouseTokenAddress: string;
    depositAddress: string;
    claimAddress: string;
    lockedAddresses: string[];
    oldSenTokenAddress: string;
    senTokenAddress: string;
}

const ADDRESS_PATTERN = /^0x[a-fA-F0-9]{40}$/;
const REQUIRED_ADDRESS_FIELDS: (keyof IChainConfigFile)[] = [
    'bcoinTokenAddress',
    'bheroTokenAddress',
    'bheroSAddress',
    'bheroStakeAddress',
    'bhouseTokenAddress',
    'depositAddress',
    'claimAddress',
    'senTokenAddress',
];

const CONFIG_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'config');

function validateChainConfig(chain: string, raw: unknown): IChainConfigFile {
    if (!raw || typeof raw !== 'object') {
        throw new Error(`[BlockchainConfig] ${chain}: entry must be an object`);
    }
    const c = raw as Record<string, unknown>;

    for (const field of REQUIRED_ADDRESS_FIELDS) {
        const value = c[field];
        if (typeof value !== 'string' || !ADDRESS_PATTERN.test(value)) {
            throw new Error(`[BlockchainConfig] ${chain}.${field} must match 0x<40-hex>, got: ${JSON.stringify(value)}`);
        }
    }

    if (typeof c.oldSenTokenAddress !== 'string' || (c.oldSenTokenAddress !== '' && !ADDRESS_PATTERN.test(c.oldSenTokenAddress))) {
        throw new Error(`[BlockchainConfig] ${chain}.oldSenTokenAddress must be empty string or 0x<40-hex>, got: ${JSON.stringify(c.oldSenTokenAddress)}`);
    }

    if (typeof c.apiUrl !== 'string') {
        throw new Error(`[BlockchainConfig] ${chain}.apiUrl must be a string`);
    }
    if (typeof c.explorerUrl !== 'string') {
        throw new Error(`[BlockchainConfig] ${chain}.explorerUrl must be a string`);
    }

    if (!Array.isArray(c.lockedAddresses) || !c.lockedAddresses.every(a => typeof a === 'string' && ADDRESS_PATTERN.test(a))) {
        throw new Error(`[BlockchainConfig] ${chain}.lockedAddresses must be an array of 0x<40-hex> strings`);
    }

    return c as unknown as IChainConfigFile;
}

function buildAddressToSymbol(c: IChainConfigFile): Record<string, string> {
    const result: Record<string, string> = {
        [c.bcoinTokenAddress]: 'BCOIN',
    };
    if (c.oldSenTokenAddress.length > 0) {
        result[c.oldSenTokenAddress] = 'SEN';
    }
    if (c.senTokenAddress.length > 0) {
        result[c.senTokenAddress] = 'SEN';
    }
    return result;
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
        const lowered: IChainConfigFile = {
            apiUrl: validated.apiUrl,
            explorerUrl: validated.explorerUrl,
            bcoinTokenAddress: validated.bcoinTokenAddress.toLowerCase(),
            bheroTokenAddress: validated.bheroTokenAddress.toLowerCase(),
            bheroSAddress: validated.bheroSAddress.toLowerCase(),
            bheroStakeAddress: validated.bheroStakeAddress.toLowerCase(),
            bhouseTokenAddress: validated.bhouseTokenAddress.toLowerCase(),
            depositAddress: validated.depositAddress.toLowerCase(),
            claimAddress: validated.claimAddress.toLowerCase(),
            lockedAddresses: validated.lockedAddresses.map(a => a.toLowerCase()),
            oldSenTokenAddress: validated.oldSenTokenAddress.toLowerCase(),
            senTokenAddress: validated.senTokenAddress.toLowerCase(),
        };
        result.set(chainKey, {
            ...lowered,
            name: chainKey,
            isProduction,
            addressToSymbol: buildAddressToSymbol(lowered),
        });
    }

    return result;
}

export {
    IBlockchainConfig,
    loadBlockchainConfigs,
};
