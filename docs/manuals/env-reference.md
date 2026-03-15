# Environment Reference

This document maps all the environment variables needed across the different services to run BombCrypto Server V2 successfully.

## ap-login (`api/login/.env.example`)

| Variable | Description |
|---|---|
| `IS_GCLOUD` | Whether the application runs inside Google Cloud. Default: `false`. |
| `IS_PROD` | Production toggle. Default: `false`. |
| `PORT` | API port to listen to. Default: `8120`. |
| `LOG_NAME` / `LOG_REMOTE_HOST` | Remote logging configurations. |
| `REDIS_CONNECTION_STRING` | Redis connection URI. |
| `POSTGRES_CONNECTION_STRING_BACKEND` | URI for the backend PostgreSQL database. |
| `POSTGRES_CONNECTION_STRING_BOMBCRYPTO` | URI for the bombcrypto PostgreSQL database. |
| `ALLOWED_DOMAINS` | CORS allowed origins. |
| `TELEGRAM_BOT_TOKEN` | Token for Telegram integrations. |
| `JWT_LOGIN_SECRET` | Secret key used to sign and verify client authentication JWTs. |
| `JWT_BEARER_SECRET` | Secret key used for Server-to-Server authentication. |
| `SYNC_BANNED_LIST` | Syncs banned player lists from Redis. |
| `VERSION_WEB` / `VERSION_SOL` | Version tags for different client platforms. |
| `TON_SERVER_MAINTENANCE` / `SOL_SERVER_MAINTENANCE` / `WEB_SERVER_MAINTENANCE` | Maintenance toggles per platform. |
| `MAIL_SENDER` / `MAIL_PASSWORD` | SMTP configuration for emails. |
| `RESET_PASSWORD_LINK` | Frontend URL for password resets. |
| `RESET_TOKEN_EXPIRE` | TTL for reset tokens (in seconds). |
| `ENABLE_REQUEST_LOGGING` | Enable API request logging middleware. |
| `AES_SECRET` / `GAME_SIGN_PADDING` / `DAPP_SIGN_PADDING` / `OBFUSCATE_BYTES_APPEND` / `RSA_DELIMITER` | Cryptographic configurations used to communicate securely with Unity Clients. |

## ap-market (`api/market/.env.example`)

| Variable | Description |
|---|---|
| `IS_GCLOUD` | Whether the application runs inside Google Cloud. Default: `false`. |
| `IS_PROD` | Production toggle. Default: `false`. |
| `PORT` | API port to listen to. Default: `9120`. |
| `ALLOWED_DOMAINS` | CORS allowed origins. |
| `IS_MARKET_OPEN` | Master switch for the marketplace functionality. Default: `true`. |
| `TRANSACTION_TIMEOUT` | Timeout in seconds. Default: `60`. |
| `CHECK_INTERVAL` | Polling interval. Default: `1`. |
| `REDIS_CONNECTION_STRING` | Redis connection URI. |
| `POSTGRES_CONNECTION_STRING` | URI for the bombcrypto PostgreSQL database. |
| `JWT_BEARER_SECRET` | Secret key used for Server-to-Server authentication. **Must match ap-login.** |
| `LOG_NAME` / `LOG_REMOTE_HOST` | Remote logging configurations. |
| `ENABLE_REQUEST_LOGGING` | Enable API request logging middleware. |

## sfs-game-1 (`server/.env.example`)

| Variable | Description |
|---|---|
| `SERVER_ID` | Identity string for this node. Default: `sv-game-1`. |
| `SALT` | Salt for hashing. |
| `APP_STAGE` | Environment (`local`, `test`, `prod`). |
| `GKE` | Running inside Google Kubernetes Engine. |
| `IS_TOURNAMENT_GAME_SERVER` / `IS_GAME_SERVER` / `IS_PVP_SERVER` | Server mode toggles. |
| `IS_TON_SERVER` / `IS_SOL_SERVER` / `IS_RON_SERVER` / `IS_BAS_SERVER` / `IS_BNB_SERVER` / `IS_VIC_SERVER` | Supported web3 networks. |
| `LOG_LEVEL_DEFAULT` / `LOG_ALL` / `LOG_PVP` / `LOG_DB` / `LOG_HANDLER` | Granular log toggles. |
| `PVP_RANK_UPDATE_SECONDS` | Interval for updating PVP ranking. |
| `AP_SIGNATURE_TOKEN` | Signature verification token. |
| `AP_LOGIN_TOKEN` | Server Bearer JWT. **Must be signed with ap-login's `JWT_BEARER_SECRET`.** |
| `SUBSCRIPTION_PACKAGE_NAME` | Configuration package string. |
| `AP_PVP_MATCHING` / `AP_SIGNATURE` / `AP_BLOCKCHAIN` / `AP_LOGIN` / `AP_MARKET` / `AP_REFERRAL` / `AP_MONETIZATION` | URLs linking back to specific API microservices. |
| `REDIS_CONNECTION_STRING` | Redis connection URI. |
| `POSTGRES_CONNECTION_STRING` / `POSTGRES_USERNAME` / `POSTGRES_PASSWORD` | JDBC connection details for the bombcrypto PostgreSQL database. |
| `POSTGRES_MAX_ACTIVE_CONNECTIONS` | Connection pool size. |
| `SCHEDULER_THREAD_SIZE` | Thread pool size for scheduled game loops. |
| `USE_STREAM_LISTENER` | Stream listener toggle. |
| `SAVE_CLIENT_LOG_PATH` | Path to save unity client error logs. |
| `HASH_ID_KEY` | Key for hashing IDs. |
