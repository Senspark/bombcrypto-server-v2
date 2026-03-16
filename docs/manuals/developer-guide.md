# Developer Manual

Welcome to the developer guide for the BombCrypto Server V2 project. This document provides essential instructions for configuring your local environment and running the services from source.

## Project Architecture Overview

The system runs via Docker Compose and consists of three main applications along with infrastructure dependencies:
1. **ap-login**: Authentication Node.js microservice (`api/login/src`).
2. **ap-market**: Marketplace Node.js microservice (`api/market/src`).
3. **sfs-game-1**: Game logic built on Java/Kotlin and the SmartFox Server (`server/`).
4. **PostgreSQL** & **Redis**: State and caching stores.

## 1. Local Environment Setup

Ensure you have the following installed:
- **Docker** and **Docker Compose** v2+
- **Node.js 22** (if developing the API services without Docker)
- **JDK 17** and **Gradle** (if developing the Kotlin game extensions)
- **PostgreSQL Client** (`psql`)

### Step 1: Initializing the Database

Before starting the services, you must create a shared docker network and start the databases:

```bash
docker network create base_default

# Start PostgreSQL and Redis
docker run -d --name postgres --network base_default -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=123456 -p 5432:5432 postgres:17
docker run -d --name redis --network base_default -p 6379:6379 redis:8
```

Run the schema files using the PostgreSQL client:

```bash
# ap-login DB
createdb -h localhost -U postgres backend
psql -h localhost -U postgres -d backend -f api/login/db/schema.sql

# ap-market & sfs-game-1 DB
createdb -h localhost -U postgres bombcrypto
psql -h localhost -U postgres -d bombcrypto -f server/db/schema.sql -f server/db/init.sql -f server/db/pvp_season_1.sql -f server/db/first_user_add_data.sql
```

### Step 2: Environment Variables

Every service requires a `.env` file to start up. Refer to the [Environment Reference](env-reference.md) document to learn how to configure these variables, or copy the `.env.example` templates in each respective service folder:

```bash
cp api/login/.env.example api/login/.env
cp api/market/.env.example api/market/.env
cp server/.env.example server/.env
```

*Note: Ensure `JWT_BEARER_SECRET` matches across ap-login and ap-market, and is used to sign `AP_LOGIN_TOKEN` in sfs-game-1.*

## 2. Building and Running Locally

### Starting Node.js Microservices (ap-login & ap-market)

You can run them natively via npm or via Docker. To run via Docker:

```bash
docker compose -f api/login/compose.yaml up -d
docker compose -f api/market/compose.yaml up -d
```

To run natively for development:
```bash
cd api/login
npm install
npm run start

cd ../market
npm install
npm run start
```

### Starting SmartFox Server (sfs-game-1)

The server codebase uses a `Dockerfile.arm64` specifically.

```bash
# Build the base image
docker build -f server/deploy/Dockerfile.arm64 -t smartfox server/deploy/

# Run the container
docker compose -f server/deploy/compose.yaml up -d
```

Access the admin panel at `http://localhost:8080`.

## 3. Contributing Rules

- **Code Formatting:** The project uses `.editorconfig` for strict formatting guidelines. Ensure your IDE is configured to use it.
- **Microservice Boundries:** Game logic should go into `server/BombChainExtension`, authentication into `api/login`, and trading into `api/market`.
- **Database Migrations:** Never modify `schema.sql` files without testing against the current live schema. Make sure to back up before pushing DDL statements.

## 4. Documentation

Documentation is maintained by **Scribe**. If you modify endpoints or database schema, you must update the corresponding files under `docs/`.
