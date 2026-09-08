export const CachedKeys = {
  AP_BL_HERO_STAKED: 'AP_BL_HERO_STAKED_V2',
  AP_BL_COINS_PRICE: 'AP_BL_COINS_PRICE',
  // Same payload as AP_BL_COINS_PRICE but with a short TTL: this one is the serve-from-cache window,
  // the other is the never-expiring last-known-good used when CoinGecko fails.
  AP_BL_COINS_PRICE_FRESH: 'AP_BL_COINS_PRICE_FRESH',
  // Short-window dedupe cache for /v4/hero. Redis hash keyed by `${network}:${address}` with per-field TTL (HEXPIRE, Redis 7.4+).
  AP_BL_V4_HERO_DETAILS: 'AP_BL_V4_HERO_DETAILS',
}

export const StreamKeys = {
  AP_BL_HERO_STAKE_STR: 'AP_BL_HERO_STAKE_STR',
  AP_BL_SYNC_HERO: 'AP_BL_SYNC_HERO',
  AP_BL_SYNC_HOUSE: 'AP_BL_SYNC_HOUSE',
  AP_BL_SYNC_DEPOSIT: 'AP_BL_SYNC_DEPOSIT',
}