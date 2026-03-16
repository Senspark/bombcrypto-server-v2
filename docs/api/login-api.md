# API Reference: Login Service (`ap-login`)

The `ap-login` service exposes various authentication endpoints depending on the connected platform or blockchain network.

All REST endpoints below are relative to `http://localhost:8120/`.

## 1. Web Router (`/web/`)

These endpoints are used by web browsers and Unity WebGL clients.

### General Flow
| Endpoint | Method | Description |
|---|---|---|
| `/verify` | POST | Call from Server: Verifies login token/data |
| `/nonce` | POST | Request a cryptographic nonce |
| `/check_proof` | POST | Submit signed nonce (Web3 wallet) |
| `/check_proof_account` | POST | Submit username/password |
| `/editor_get_jwt` | GET | Unity Editor: Create test JWT |
| `/editor_get_jwt_account` | GET | Unity Editor: Create test JWT (Account) |
| `/refresh/:rf` | GET | Refresh access JWT using refresh token |
| `/ban_list` | GET | Get list of banned wallets |
| `/check_server` | GET | Check if the server is in maintenance mode |
| `/change_nick_name` | POST | Change user nickname |

> **Note:** The Web Router also mounts network-specific endpoints such as `/bsc/nonce`, `/pol/nonce`, `/ron/nonce`, and `/bas/nonce` with identically structured operations.

## 2. DApp Router (`/dapp/`)

These endpoints are meant for decentralized applications.

| Endpoint | Method | Description |
|---|---|---|
| `/verify_account` | POST | Verify account details |
| `/get_nonce` | POST | Request a cryptographic nonce |
| `/verify_signature` | POST | Verify wallet signature |
| `/profile` | GET | Get user profile (Requires JWT) |
| `/create_senspark_account` | POST | Create a Senspark account |
| `/create_account_fi` | POST | Create an FI account |
| `/change_password` | POST | Change account password |
| `/force_change_password` | POST | Admin/Force change password |
| `/forgot_password` | POST | Initiate password reset |
| `/reset_password` | POST | Submit new password using reset token |
| `/assign_wallet_to_account` | POST | Link a Web3 wallet |
| `/set_avatar` | POST | Set user avatar |
| `/get_avatar` | GET | Get user avatar |
| `/change_nick_name` | POST | Change user nickname |

## 3. Solana Router (`/sol/`)

Dedicated endpoints for the Solana ecosystem.

| Endpoint | Method | Description |
|---|---|---|
| `/verify` | POST | Server call: Verify login data |
| `/nonce` | POST | React call: Request nonce |
| `/check_proof` | POST | React call: Submit signed nonce |
| `/editor_get_jwt` | GET | Get Unity Editor test JWT |
| `/refresh` | GET | Refresh JWT token |
| `/ban_list` | GET | Get banned Solana wallets |
| `/check_server` | GET | Check if Solana servers are in maintenance |

## 4. TON Router (`/ton/`)

Dedicated endpoints for the TON blockchain ecosystem.

| Endpoint | Method | Description |
|---|---|---|
| `/verify` | POST | Server call: Verify login token |
| `/generate_payload` | POST | Currently not supported |
| `/nonce` | POST | Request nonce |
| `/check_proof` | POST | Submit signed nonce |
| `/refresh` | POST | Refresh JWT token |
| `/editor_get_jwt` | GET | Get Unity Editor test JWT |
| `/check_server` | GET | Check if TON servers are in maintenance |

## 5. Mobile Router (`/mobile/`)

Endpoints used specifically by the iOS/Android Unity clients.

| Endpoint | Method | Description |
|---|---|---|
| `/verify` | POST | Server call: Verify login data |
| `/refresh` | POST | Refresh JWT token |
| `/check_proof` | POST | Verify username/password |
| `/check_server` | GET | Check server maintenance status |
| `/check_proof_guest` | POST | Authenticate guest accounts |
| `/create_guest_account` | GET | Create a new temporary guest account |
| `/create_senspark_account` | POST | Create a full Senspark account |
| `/change_nick_name` | POST | Change user nickname |
