export function isEvmWalletAddress(s: string): boolean {
    return /^0x[a-fA-F0-9]{40}$/.test(s);
}

// Lowercase only when input is an EVM wallet (42 chars, 0x + 40 hex).
// Senspark account usernames are case-sensitive identities — must be preserved.
export function normalizeAuthInput(s: string): string {
    return isEvmWalletAddress(s) ? s.toLowerCase() : s;
}
