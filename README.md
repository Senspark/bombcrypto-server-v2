# BombCrypto Server V2

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Version](https://img.shields.io/badge/version-v2.0-blue)
![License](https://img.shields.io/badge/license-AGPLv3-blue)

Open-source game server backend for BombCrypto V2, featuring Treasure Hunt and Adventure game modes. The backend consists of three microservices that communicate over a shared Docker network.

## 📚 Documentation
- **[Architecture Maps](docs/architecture/c4-model.md)**: Explore the system context and container diagrams.
- **[Database Schemas](docs/architecture/database-schema.md)**: View the entity-relationship models.
- **[API Reference](docs/api/login-api.md)**: Endpoints for authentication and marketplace.
- **[Developer Manual](docs/manuals/developer-guide.md)**: Learn how to set up, build, and contribute.

## Overview

### Architecture

```
                         base_default network
                    ┌──────────────────────────────────────────────────────┐
                    │                                                      │
Unity Client ───────┼── HTTP :8120 ──────► [ap-login]  ──► PostgreSQL      │
                    │                                      (backend DB)    │
                    ├── HTTP :9120 ──────► [ap-market] ──► PostgreSQL      │
                    │                                      (bombcrypto DB) │
                    ├── TCP/UDP :9933 ──► [sfs-game-1] ──► PostgreSQL      │
                    │   HTTP :8080          (SmartFox)      (bombcrypto DB)│
                    │                          │                           │
                    │                          └──────────► Redis          │
                    └──────────────────────────────────────────────────────┘
```

### Services

| Service | Technology | Port(s) | Description |
|---------|-----------|---------|-------------|
| **[ap-login](api/login/)** | Node.js / TypeScript | 8120 | Authentication — email/password, TON, Solana, Web3 wallets |
| **[ap-market](api/market/)** | Node.js / TypeScript | 9120 | In-game item marketplace API |
| **[sfs-game-1](server/)** | Java/Kotlin + SmartFox Server 2.19 | 8080, 9933 | Game server — Treasure Hunt & Adventure modes |

### Databases

| Database | Used By | Schema                                                       | Description |
|----------|---------|--------------------------------------------------------------|-------------|
| `backend` | ap-login | [`api/login/db/schema.sql`](api/login/db/schema.sql)         | User accounts and authentication |
| `bombcrypto` | sfs-game-1, ap-market | [`server/db/schema.sql`](server/db/schema.sql) | Game data, items, marketplace |

## Prerequisites

- **Docker** and **Docker Compose** v2+
- **PostgreSQL 17 client tools** (`createdb`, `psql`) — for initial database setup only
- **Git**
- *(Optional)* **JDK 17+** and **Gradle** — only needed if rebuilding SmartFox game extensions from source

## Quick Start

```bash
# 1. Create Docker network
docker network create base_default

# 2. Start PostgreSQL and Redis
docker run -d --name postgres --network base_default \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=123456 \
  -p 5432:5432 postgres:17

docker run -d --name redis --network base_default \
  -p 6379:6379 redis:8

# 3. Create databases and load schemas
createdb -h localhost -U postgres backend && \
psql -h localhost -U postgres -d backend -f api/login/db/schema.sql

createdb -h localhost -U postgres bombcrypto && \
psql -h localhost -U postgres -d bombcrypto \
  -f server/db/schema.sql \
  -f server/db/init.sql \
  -f server/db/pvp_season_1.sql \
  -f server/db/first_user_add_data.sql

# 4. Configure environment files (see Section 3 for details)
cp api/login/.env.example api/login/.env
cp api/market/.env.example api/market/.env
cp server/.env.example server/.env

# 5. Build and start services
docker compose -f api/login/compose.yaml up -d
docker compose -f api/market/compose.yaml up -d

docker build -f server/deploy/Dockerfile.arm64 -t smartfox server/deploy/
docker compose -f server/deploy/compose.yaml up -d

# 6. Verify
docker ps --filter "network=base_default"
```

> **Note:** Step 4 requires editing the `.env` files with proper values. See [Section 3](#3-environment-configuration) for the recommended configuration.

---

## 1. Infrastructure Setup

### Docker Network

All services use an external Docker network called `base_default`. Create it before starting any service:

```bash
docker network create base_default
```

### PostgreSQL

Run a PostgreSQL 17 container on the shared network:

```bash
docker run -d \
  --name postgres \
  --network base_default \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=123456 \
  -e POSTGRES_DB=postgres \
  -v postgres_data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:17
```

> **Warning:** The default password `123456` is for local development only. Use a strong password in production.

### Redis

```bash
docker run -d \
  --name redis \
  --network base_default \
  -p 6379:6379 \
  redis:8
```

## 2. Database Setup

The project uses two PostgreSQL databases.

### Database: `backend`

Used by **ap-login** for user accounts and authentication. Schema: [`api/login/db/schema.sql`](api/login/db/schema.sql)

```bash
createdb -h localhost -U postgres backend
psql -h localhost -U postgres -d backend -f api/login/db/schema.sql
```

### Database: `bombcrypto`

Used by **sfs-game-1** and **ap-market** for game data. Schema and seed files are in [`server/db/`](server/db/).

```bash
createdb -h localhost -U postgres bombcrypto
psql -h localhost -U postgres -d bombcrypto \
  -f server/db/schema.sql \
  -f server/db/init.sql \
  -f server/db/pvp_season_1.sql \
  -f server/db/first_user_add_data.sql
```

| SQL File | Purpose |
|----------|---------|
| [`schema.sql`](server/db/schema.sql) | Tables, functions, types, and partitioned logging |
| [`init.sql`](server/db/init.sql) | Initial game configuration (heroes, items, quests, etc.) |
| [`pvp_season_1.sql`](server/db/pvp_season_1.sql) | Initializes PvP season 1 ranking |
| [`first_user_add_data.sql`](server/db/first_user_add_data.sql) | Seeds test data for user ID 1 (3 heroes, 1 house, currencies) |

## 3. Environment Configuration

Each service requires a `.env` file. You can copy from `.env.example` and adjust, or use the recommended configurations below.

### ap-login ([`api/login/.env`](api/login/.env.example))

```bash
cp api/login/.env.example api/login/.env
```

Recommended `.env` content:

```env
IS_GCLOUD=false
IS_PROD=false
PORT=8120
LOG_NAME=
LOG_REMOTE_HOST=

REDIS_CONNECTION_STRING="redis://@localhost:6379/0"
POSTGRES_CONNECTION_STRING_BACKEND="postgres://postgres:123456@localhost:5432/backend"
POSTGRES_CONNECTION_STRING_BOMBCRYPTO="postgres://postgres:123456@localhost:5432/bombcrypto2"
ALLOWED_DOMAINS=""
TELEGRAM_BOT_TOKEN=","

# JWT secret for client login tokens
JWT_LOGIN_SECRET="your_client_login_token"

# JWT secret for SmartFox server Bearer authentication
JWT_BEARER_SECRET="your_server_login_token"
SYNC_BANNED_LIST=false
VERSION_WEB=1
VERSION_SOL=1

TON_SERVER_MAINTENANCE=false
SOL_SERVER_MAINTENANCE=false
WEB_SERVER_MAINTENANCE=false

MAIL_SENDER=
MAIL_PASSWORD=
RESET_PASSWORD_LINK="http://localhost:3000/account/forgot/change?token="
RESET_TOKEN_EXPIRE=3600

ENABLE_REQUEST_LOGGING=true

AES_SECRET="NtiflzWBc+2Gwvyz6HixfENY8smw9Ip9GWxpdwA2LFo="
GAME_SIGN_PADDING="a3322-"
DAPP_SIGN_PADDING="a3322-"
OBFUSCATE_BYTES_APPEND=1
RSA_DELIMITER="*"
```

> **Note:** When running via Docker Compose, [`compose.yaml`](api/login/compose.yaml) overrides `PORT`, `REDIS_CONNECTION_STRING`, and `POSTGRES_CONNECTION_STRING_*` to use Docker hostnames (`redis`, `postgres`). The `.env` values above are for local non-Docker development.

### ap-market ([`api/market/.env`](api/market/.env.example))

```bash
cp api/market/.env.example api/market/.env
```

Recommended `.env` content:

```env
IS_GCLOUD=false
IS_PROD=false
PORT=9120
ALLOWED_DOMAINS=""
IS_MARKET_OPEN=true
TRANSACTION_TIMEOUT=60
CHECK_INTERVAL=1

REDIS_CONNECTION_STRING="redis://@localhost:6379/0"
POSTGRES_CONNECTION_STRING="postgres://postgres:123456@localhost:5432/bombcrypto"

JWT_BEARER_SECRET="your_server_login_token"

LOG_NAME=
LOG_REMOTE_HOST=

ENABLE_REQUEST_LOGGING=true
```

> **Important:** `JWT_BEARER_SECRET` must match the value set in ap-login.

> **Note:** Similar to ap-login, [`compose.yaml`](api/market/compose.yaml) overrides connection strings for Docker deployment.

### sfs-game-1 ([`server/.env`](server/.env.example))

```bash
cp server/.env.example server/.env
```

Recommended `.env` content for Docker deployment:

```env
SERVER_ID="sfs-game-1"
SALT=
# local, test, prod
APP_STAGE=local
IS_DEBUG=1
GKE=0
IS_TOURNAMENT_GAME_SERVER=false
IS_GAME_SERVER=1
IS_PVP_SERVER=0
IS_BNB_SERVER=1
IS_TON_SERVER=0
IS_SOL_SERVER=0
IS_BAS_SERVER=0
IS_RON_SERVER=0

# DEBUG, INFO, WARN, ERROR
LOG_LEVEL_DEFAULT=INFO
LOG_ALL=true
LOG_PVP=false
LOG_DB=true
LOG_HANDLER=true
LOG_REMOTE_HOST=""
LOG_HTTP_REQUEST=true

PVP_RANK_UPDATE_SECONDS=1500

AP_SIGNATURE_TOKEN=""
AP_LOGIN_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3NzMwNTEwMTgsImV4cCI6MjA4ODYyNzAxOH0.gmILdFzBTRO8ZRFEukUYLP0G4VODvQpyaYAIZhbDFIQ"
SUBSCRIPTION_PACKAGE_NAME=""

AP_PVP_MATCHING="http://ap-pvp-matching"
AP_SIGNATURE="http://ap-signature"
AP_BLOCKCHAIN="http://ap-blockchain"
AP_LOGIN="http://ap-login"
AP_MARKET="http://ap-market"
AP_REFERRAL="http://ap-referral"
AP_MONETIZATION="http://ap-monetization"

REDIS_CONNECTION_STRING="redis://redis:6379"

POSTGRES_CONNECTION_STRING="jdbc:postgresql://postgres:5432/bombcrypto"
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=123456
POSTGRES_MAX_ACTIVE_CONNECTIONS=10

SCHEDULER_THREAD_SIZE=20
USE_STREAM_LISTENER="true"

HASH_ID_KEY="0123456789abcdefghijklmnopqrstuvwxyz_,0,0"
```

> **Note:** Like ap-login and ap-market, [`compose.yaml`](server/deploy/compose.yaml) loads this `.env` file via `env_file` and can override specific values in its `environment` section (e.g., `MAX_CONNECTIONS_PER_IP`, `MAX_CCU`, `SERVER_NAME`). Use Docker hostnames (`postgres`, `redis`, `ap-login`, etc.) instead of `localhost`.

> **Note:** `AP_LOGIN_TOKEN` must be a valid JWT signed with ap-login's `JWT_BEARER_SECRET`.

## 4. Building and Starting Services

### ap-login

```bash
docker compose -f api/login/compose.yaml up -d
```

Builds from [`api/login/Dockerfile`](api/login/Dockerfile) (Node.js 22 Alpine) and starts on port **8120**. See [`api/login/compose.yaml`](api/login/compose.yaml).

### ap-market

```bash
docker compose -f api/market/compose.yaml up -d
```

Builds from [`api/market/Dockerfile`](api/market/Dockerfile) (Node.js 22 Alpine) and starts on port **9120**. See [`api/market/compose.yaml`](api/market/compose.yaml).

### sfs-game-1 (SmartFox Game Server)

**Step 1:** Build the SmartFox base image using [`Dockerfile.arm64`](server/deploy/Dockerfile.arm64):

```bash
docker build -f server/deploy/Dockerfile.arm64 -t smartfox server/deploy/
```

> **Note:** Currently only `Dockerfile.arm64` is available, optimized for ARM64 (Apple Silicon). An amd64 Dockerfile may be added in the future.

**Step 2:** Start the container (see [`server/deploy/compose.yaml`](server/deploy/compose.yaml)):

```bash
docker compose -f server/deploy/compose.yaml up -d
```

The SmartFox server exposes:

| Port | Protocol | Purpose |
|------|----------|---------|
| 8080 | HTTP | WebSocket connections, admin panel |
| 8443 | HTTPS | Secure WebSocket connections |
| 9933 | TCP/UDP | Socket connections (game client) |
| 9898 | TCP | Internal |

## 5. Verification

Check that all containers are running:

```bash
docker ps --filter "network=base_default"
```

Test the API services:

```bash
# ap-login
curl http://localhost:8120

# ap-market
curl http://localhost:9120
```

Access the SmartFox admin panel at [http://localhost:8080](http://localhost:8080) (default credentials: `bombcryptodev` / `123456`).

## 6. Unity Client Configuration (Optional)

This section is for developers setting up the [bombcrypto-client-v2](https://github.com/Senspark/bombcrypto-client-v2) Unity game client.

### AppConfig.json

Create `Assets/Resources/configs/AppConfig.json` in the Unity client project:

```json
{
  "isProduction": false,
  "gamePlatform": "WEBGL",
  "forEthNetwork": "BSC",
  "buildInfo": {
    "testVersion": 26031117,
    "productionVersion": 26031117
  },
  "testUserName": "",
  "testPassword": "",
  "editorAccount": "editor1",
  "testWalletEth": "",
  "testWalletSolana": "",
  "testWalletTonHex": "",
  "rsaDelimiter": "*"
}
```

> **Note:** `rsaDelimiter` must match the server's `RSA_DELIMITER` value.

### WebGL Build Template

If testing with a WebGL build, create `unity-web-template/.env`:

```env
VITE_IS_GCLOUD=false
VITE_IS_PROD=false
VITE_IS_MAIN_TEST=false
VITE_UNITY_FOLDER=./webgl/1773225949

VITE_LOADER_URL_EXTENSION=/webgl.loader.js
VITE_DATA_URL_EXTENSION=/webgl.data
VITE_DATA_URL_MOBILE_EXTENSION=/mobile.data.br
VITE_FRAMEWORK_URL_EXTENSION=/webgl.js
VITE_CODE_URL_EXTENSION=/webgl.wasm

VITE_API_HOST="http://localhost:8120/web"
VITE_API_CHECK_IP_HOST="http://localhost:8121"
VITE_OTHER_NETWORK_URL="http://localhost:5173"

VITE_VERSION=1
VITE_IGNORE_IP_CHECK=true
VITE_IGNORE_CHECK_VERSION=true

VITE_SIGN_SECRET="NtiflzWBc+2Gwvyz6HixfENY8smw9Ip9GWxpdwA2LFo="
VITE_SIGN_PADDING="a3322-"
VITE_LOCAL_SECRET="8fL9sM95YKC8IWeBwTjQy0x/EiNZfDmiITXZe20tPZc="
VITE_LOCAL_IV="3ji8kERpIE4CZaW3zK1IOA=="
VITE_APPEND_BYTES="1"
VITE_RSA_DELIMITER="*"
VITE_WALLET_PROJECT_ID="create_your_reown_id"
```

> **Important:** `VITE_SIGN_SECRET` and `VITE_SIGN_PADDING` must match ap-login's `AES_SECRET` and `GAME_SIGN_PADDING`.

### Default Game Connection Ports

The SmartFox game server defaults to port **8080** (HTTP/WebSocket) and **9933** (TCP/UDP socket). The Unity game client is pre-configured to connect to these ports — no client-side port configuration is needed for default setups.

## Project Structure

```
bombcrypto-server-v2/
├── api/
│   ├── login/                        # ap-login service
│   │   ├── compose.yaml
│   │   ├── Dockerfile
│   │   ├── .env.example
│   │   ├── package.json
│   │   ├── db/
│   │   │   └── schema.sql            # backend database schema
│   │   └── src/                      # TypeScript source
│   └── market/                       # ap-market service
│       ├── compose.yaml
│       ├── Dockerfile
│       ├── .env.example
│       ├── package.json
│       └── src/                      # TypeScript source
├── server/                           # sfs-game-1 (SmartFox game server)
│   ├── .env.example
│   ├── build.gradle.kts
│   ├── run.sh                        # Build & deploy script
│   ├── db/
│   │   ├── schema.sql                # bombcrypto database schema
│   │   ├── init.sql                  # Initial game config data
│   │   ├── pvp_season_1.sql          # PvP season initialization
│   │   └── first_user_add_data.sql   # Test user seed data
│   ├── deploy/
│   │   ├── Dockerfile.arm64
│   │   ├── compose.yaml
│   │   ├── extensions_volume/        # Compiled game extensions (JARs)
│   │   └── SmartFoxServer_2X/        # SmartFox config patches
│   ├── BombChainExtension/           # Game logic (Kotlin)
│   ├── Common/                       # Shared library (Kotlin)
│   ├── ClientModule/                 # Client module
│   └── SmartFoxLibs/                 # SmartFox SDK libraries
└── README.md
```

**Key files:**
[`api/login/README.md`](api/login/README.md) | [`api/login/compose.yaml`](api/login/compose.yaml) | [`api/login/Dockerfile`](api/login/Dockerfile) | [`api/login/.env.example`](api/login/.env.example) | [`api/login/db/schema.sql`](api/login/db/schema.sql)
[`api/market/README.md`](api/market/README.md) | [`api/market/compose.yaml`](api/market/compose.yaml) | [`api/market/Dockerfile`](api/market/Dockerfile) | [`api/market/.env.example`](api/market/.env.example)
[`server/README.md`](server/README.md) | [`server/.env.example`](server/.env.example) | [`server/deploy/compose.yaml`](server/deploy/compose.yaml) | [`server/deploy/Dockerfile.arm64`](server/deploy/Dockerfile.arm64)
[`server/db/schema.sql`](server/db/schema.sql) | [`server/db/init.sql`](server/db/init.sql) | [`server/db/pvp_season_1.sql`](server/db/pvp_season_1.sql) | [`server/db/first_user_add_data.sql`](server/db/first_user_add_data.sql)

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `network base_default not found` | Run `docker network create base_default` |
| Database connection refused | Ensure PostgreSQL container is running and on the `base_default` network |
| JWT authentication failures between services | Ensure `JWT_BEARER_SECRET` matches across ap-login and ap-market, and `AP_LOGIN_TOKEN` in sfs-game-1 is signed with that secret |
| SmartFox fails to start | Check logs with `docker logs sfs-game-1`; verify `.env` is configured with Docker hostnames |
| Game extensions not loading | Ensure `server/deploy/extensions_volume/` contains the compiled JAR files |
| Encryption mismatch with Unity client | Verify `AES_SECRET`, `GAME_SIGN_PADDING`, and `RSA_DELIMITER` match between server and client configs |

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE).
