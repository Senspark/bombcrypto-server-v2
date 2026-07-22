# Signature API

Server-side signer for the Bombcrypto game. Issues ECDSA signatures that the on-chain claim/voucher contracts accept, and exposes read-only validators for the same contracts (has-claimed? voucher usable? hero balance sufficient?).

All chain reads (contract calls + wallet nonce) go through `blockchain-center-api`, so this service holds no RPC URLs or API keys. Signing is local and uses `PRIVATE_KEY` only.

Supports two networks: **BSC** (`bsc` / `bnb`) and **Polygon** (`polygon`).

## Requirements

- Node `>=24`
- npm `>=10.2.4`
- Running instance of `blockchain-center-api` reachable at `BLOCKCHAIN_CENTER_API`

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
| `IS_GCLOUD` | no (default `false`) | `true` when running on GCP |
| `IS_PROD` | yes | `true` to load `config/prod.json`, `false` to load `config/test.json` |
| `PORT` | no (default `8080`) | HTTP listen port |
| `DEFAULT_NETWORK` | yes | `BSC` or `POL` — used when a request omits `?network=` |
| `PRIVATE_KEY` | yes | Signer wallet private key (the signatures come from this key) |
| `JWT_SECRET` | yes | HMAC secret used to verify bearer tokens on `/sign/*` endpoints |
| `JWT_PAYLOAD_KEY` | yes | Expected `key` field inside verified JWT payloads |
| `BLOCKCHAIN_CENTER_API` | yes | Base URL of the upstream blockchain-center service (e.g. `http://localhost:3005`) |
| `LOG_NAME` | no | Instance id appended to remote log tags |
| `LOG_REMOTE_HOST` | no | Fluentd host (`host:port`); remote logging disabled if unset |
| `ENABLE_REQUEST_LOGGING` | no (default `false`) | Log every request/response |

### Chain config (`config/{prod,test}.json`)

Per-network contract addresses. RPC URLs are owned by `blockchain-center-api`, not this service.

| Field | Description |
|---|---|
| `claimAirdropAddress` | Airdrop claim contract (BSC only; empty on Polygon) |
| `claimManageAddress` | Claim-manager contract |
| `bheroBscAddress` | BHero token on BSC, used by Polygon to verify cross-chain hero balance (Polygon only; empty on BSC) |

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
- `/signature`
- `/polygon` (legacy; defaults `network` to `POL`)
- `/polygon/signature` (legacy)

### Network selection

Endpoints accept a `network` query parameter:

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
| `1` | Bad request |
| `2` | Invalid address |
| `3` | Invalid voucher |
| `4` | Invalid authorization |
| `100` | Internal error |

### Authentication

All `/sign/*` endpoints require an `Authorization: Bearer <jwt>` header. The JWT must be signed with `JWT_SECRET` (HS256) and carry a `key` claim equal to `JWT_PAYLOAD_KEY`. See [src/routes/Middlewares.ts](src/routes/Middlewares.ts) and [src/services/JwtService.ts](src/services/JwtService.ts).

To mint a token using the current `.env`:

```bash
npm run issue-jwt --silent
```

Prints a JWT that `/sign/*` will accept. Source: [scripts/issueJwt.ts](scripts/issueJwt.ts).

## Endpoints

### Health

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Warm-up — `200 OK` |
| `GET` | `/_ah/warmup` | GCP App Engine warm-up probe |

### Sign ([src/routes/SignatureHandlers.ts](src/routes/SignatureHandlers.ts)) *

| Method | Path | Params | Description |
|---|---|---|---|
| `GET` | `/sign/claim-airdrop` | `network`, `userAddress`, `numClaim`, `eventId` | BSC-only. Signs a claim for `numClaim` airdrop NFTs for `eventId` |
| `POST` | `/sign/claim-token` | body: `{userAddress, tokenType, amount, details?}` | Signs a token-claim tx |
| `POST` | `/sign/upgrade-hero-shield` | body: `{wallet_address, hero_id}` | Signs a hero-shield upgrade |
| `GET` | `/sign/voucher` | `network`, `userAddress`, `tokenPay`, `voucherType` | Polygon voucher flow: validates voucher + BSC hero balance, then signs purchase |

`*` = requires bearer token.

### Validate ([src/routes/ValidateHandlers.ts](src/routes/ValidateHandlers.ts))

| Method | Path | Params | Description |
|---|---|---|---|
| `GET` | `/validate/airdrop/user` | `network`, `userAddress`, `eventId` | BSC-only. Has the user already claimed for this event? |
| `GET` | `/validate/check-total-claim-token` | `network`, `userAddress`, `tokenType` | Polygon. Total claimed amount for a token type (formatted when ≥ 10^12) |
| `GET` | `/validate/can-use-voucher` | `network`, `userAddress`, `voucherType` | Polygon. Can this user use this voucher? |
| `GET` | `/validate/check-hero1-balance` | `network`, `userAddress` | Polygon. Does the user hold ≥ 15 BHero tokens on BSC? |
| `POST` | `/validate/recover-address` | body: `{message, hash}` | Recover the signer of an EIP-191 signature |

## Project layout

```
src/
  Server.ts                 entry point
  App.ts                    Express app wiring + route table
  Config.ts                 envalid-typed env config
  BlockchainConfig.ts       loads config/{prod,test}.json
  Abi.ts                    contract ABIs (airdrop, claim-manage, erc721)
  Error.ts, ErrorCode.ts    typed errors + numeric codes
  Utility.ts                ethers helpers (isAddress, hashMessage)
  apis/                     ValidateApi (contract reads via blockchain-center-api)
  providers/                BlockchainCenterApi client (callContract, getTransactionCount)
  services/                 Dependencies, SignatureService, JwtService, loggers
  routes/                   HTTP handlers + middlewares
  consts/                   Express extension, voucher table, error types
config/
  prod.json                 mainnet contract addresses (BSC, POL)
  test.json                 testnet contract addresses
```
