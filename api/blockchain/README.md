# Blockchain API

Read-only service that exposes on-chain data for the Bombcrypto game (Heroes, Houses, token balances, deposits, staking). It fronts a shared `BLOCKCHAIN_CENTER_API` provider, caches results in Redis, and persists hero-stake events to Postgres.

Supports two networks: **BSC** (`bsc` / `bnb`) and **Polygon** (`polygon`).

## Requirements

- Node `>=24`
- npm `>=10.2.4`
- Redis
- Postgres

## Setup

```bash
cp .env.example .env
# fill in required values (see below)
npm install
npm run dev
```

### Environment variables

| Key | Required | Description |
|---|---|---|
| `IS_GCLOUD` | yes | `true` when running on GCP (enables in-process scheduler) |
| `IS_PROD` | yes | `true` to load `config/prod.json`, `false` to load `config/test.json` |
| `PORT` | no (default `8080`) | HTTP listen port |
| `DEFAULT_NETWORK` | yes | `BSC` or `POL` — used when a request omits `?network=` |
| `REDIS_CONNECTION_STRING` | yes | e.g. `redis://@localhost:6379/0` |
| `POSTGRES_CONNECTION_STRING` | yes | e.g. `postgres://user:pass@host:5432/blockchain` |
| `STAKE_CACHE_SECONDS` | no (default `900`) | TTL for cached hero-stake values |
| `SCHEDULER_INTERVAL_SEC` | no (default `[5,30]`) | `[min,max]` seconds between subscriber polls |
| `JUMP_TO_LATEST_BLOCK` | no | Subscriber skips historical blocks on startup when `true` |
| `USE_SUBSCRIBER_ONLY` | no | Run only the event subscriber (no HTTP server) when `true` |
| `BLOCKCHAIN_CENTER_API` | yes | Base URL of the upstream blockchain-center service |
| `LOG_NAME` | no | Instance id appended to remote log tags |
| `LOG_REMOTE_HOST` | no | Fluentd host (`host:port`); remote logging disabled if unset |

## Build & run

```bash
npm run dev        # run from source with vite-node
npm run build      # compile to dist/
node dist/Server.js
npm test           # vitest
npm run lint
```

A `Dockerfile` is included for a two-stage production image.

## Request routing

The same router is mounted at multiple base paths (all equivalent):

- `/`
- `/blockchain`
- `/polygon` (legacy; defaults `network` to `POL`)
- `/polygon/blockchain` (legacy)

Examples below use `/` for brevity.

### Network selection

Most endpoints accept a `network` query parameter:

- `bsc` or `bnb` → BSC
- `polygon` → Polygon
- omitted → `DEFAULT_NETWORK` (or `POL` when the request path contains `/polygon`)

### Response envelope

```json
{ "code": 0, "message": <payload> }
```

Error codes ([src/ErrorCode.ts](src/ErrorCode.ts)):

| Code | Meaning |
|---|---|
| `0` | OK |
| `1` | Invalid address |
| `2` | Invalid date |
| `3` | Invalid block |
| `4` | Invalid authorization |
| `100` | Internal error |

## Endpoints

### Health

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Warm-up — `200 OK` |
| `GET` | `/_ah/warmup` | GCP App Engine warm-up probe |

### Tokens ([src/routes/TokenHandlers.ts](src/routes/TokenHandlers.ts))

| Method | Path | Query | Description |
|---|---|---|---|
| `GET` | `/total_coins` | `network` | Total BCOIN supply minus balances held at known contracts |
| `GET` | `/coins_price` | — | Current USD prices for `{bnb_bcoin, bnb_sen, bnb_native, polygon_bcoin, polygon_sen, polygon_native}` from CoinGecko (one `/simple/price` call, cached 60s); falls back to the last known good set in Redis on upstream failure |
| `GET` | `/circulating_supply` | `network` | Total coins minus balances held at `lockedAddresses` |
| `GET` | `/coin_balance` | `network`, `address` | BCOIN balance for a wallet. CORS enabled |
| `GET` | `/total_hero` | `network` | Current BHero token-id counter |
| `GET` | `/total_house` | `network` | Current BHouse token-id counter |

### Heroes ([src/routes/HeroHandlers.ts](src/routes/HeroHandlers.ts))

| Method | Path | Params | Description |
|---|---|---|---|
| `GET` | `/hero` | `network`, `address?`, `id?`, `details?`, `mode?`, `clear?`, `uid?` | Decoded hero metadata. Publishes to the `AP_BL_SYNC_HERO` Redis stream when `uid` is given |
| `GET` | `/hero_details` | `network`, `address?`, `id?`, `clear?` | Raw on-chain `tokenDetails` value |
| `GET` | `/hero_owner` | `network`, `id` | Owner address of a hero token |
| `GET` | `/hero_stake` | `network`, `id` | Legacy: staked BCOIN amount for a hero (single number) |
| `GET` | `/hero_stake_v2` | `network`, `id` | `{stake_bcoin, stake_sen}` |
| `POST` | `/hero_decode` | body: `{details: string}` | Decode an on-chain hero `details` blob into [`IDecodedDetails`](src/IDecodedDetails.ts) |
| `POST` | `/hero_encode` | body: `IDecodedDetails` | Encode `IDecodedDetails` back into the on-chain blob |
| `POST` | `/create_rock` * | body: `{tx, wallet_address, hero_ids[]}` | Verify a `createRock` transaction burned the claimed heroes |
| `GET` | `/query_rock_burn_tx` | `network`, `tx` | Inspect a createRock tx and return `{tx, wallet_address, hero_ids}` |
| `GET` | `/refresh_stake` * | `network`, `id` | Invalidate cache and re-fetch `{stake_bcoin, stake_sen}` |

`*` = requires bearer token.

### Houses ([src/routes/HouseHandlers.ts](src/routes/HouseHandlers.ts))

| Method | Path | Params | Description |
|---|---|---|---|
| `GET` | `/house` | `network`, `address?`, `id?`, `details?`, `mode?`, `clear?`, `uid?` | Decoded house metadata. Publishes to `AP_BL_SYNC_HOUSE` stream when `uid` is given |
| `GET` | `/house_details` | `network`, `address?`, `id?`, `clear?` | Raw on-chain `tokenDetails` value |
| `GET` | `/house_owner` | `network`, `id` | Owner address of a house token |

### Deposits ([src/routes/DepositHandlers.ts](src/routes/DepositHandlers.ts))

| Method | Path | Params | Description |
|---|---|---|---|
| `GET` | `/v3/deposited` | `network`, `address`, `uid?` | Deposited balances for a wallet, grouped by token symbol (`BCOIN`, `SEN`, …). Publishes to `AP_BL_SYNC_DEPOSIT` stream when `uid` is given |

## Project layout

```
src/
  Server.ts                 entry point
  App.ts                    Express app wiring + route table
  Config.ts                 envalid-typed env config
  BlockchainConfig.ts       loads config/{prod,test}.json
  apis/                     IBlockchainApi implementation
  contracts/                ethers contract wrappers (Coin, Hero, HeroS, House, NFT, Deposit)
  providers/                BlockchainCenterApi client
  routes/                   HTTP handlers + middlewares
  cache/                    Redis client, messenger, cached NFT details/owner
  database/                 pg pool + typed data access
  services/                 logger, JWT, dependency container
  subscribers/              long-running scheduler (hero-stake events)
config/
  prod.json                 mainnet contract addresses (BSC, POL)
  test.json                 testnet contract addresses
```
