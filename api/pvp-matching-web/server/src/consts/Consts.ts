export const CachedKeys = {
    AP_PVP_TEST_MATCHES: 'AP_PVP_TEST_MATCHES',
}

export interface StreamEntry {
    id: string;
    timestamp: Date;
    fields: Record<string, string>;
}
