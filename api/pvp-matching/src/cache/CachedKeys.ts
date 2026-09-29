export const CachedKeys = {
    AP_PVP_FIXTURE_MATCHES: 'AP_PVP_FIXTURE_MATCHES',
    AP_PVP_TOURNAMENT_MATCHES: 'AP_PVP_TOURNAMENT_MATCHES',
    AP_PVP_TEST_MATCHES: 'AP_PVP_TEST_MATCHES',
    AP_PVP_MY_MATCH: 'AP_PVP_MY_MATCH',
};

export const Channels = {
    SV_PVP_CHANNEL: "SV_PVP_CHANNEL",
};

// Must match PvpBusTypes in CachedKeys.kt
export const PvpBusTypes = {
    PVP_JOIN_QUEUE: "PVP_JOIN_QUEUE", // server game -> api pvp-matching
    PVP_LEAVE_QUEUE: "PVP_LEAVE_QUEUE", // server game -> api pvp-matching
    PVP_MATCH_FOUND: "PVP_MATCH_FOUND", // api pvp-matching -> server game
    PVP_MATCH_UPDATED: "PVP_MATCH_UPDATED", // server pvp -> api pvp-matching
    PVP_MATCH_FINISHED: "PVP_MATCH_FINISHED", // server pvp -> server game, api pvp-matching
};
