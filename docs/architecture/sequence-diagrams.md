# Sequence Diagrams

This document outlines the core operational flows for the BombCrypto Server V2 system.

## 1. Web3 Wallet Login Flow (ap-login)

The following sequence diagram maps out the REST endpoints involved when a player logs into the system using a Web3 wallet (via `WebRouter.ts`).

```mermaid
sequenceDiagram
    autonumber
    actor Client as Unity Client / Web Client
    participant AP_Login as ap-login (WebHandler)
    participant Redis as Redis
    participant DB as Backend DB

    Note over Client, DB: Step 1: Request Nonce
    Client->>AP_Login: POST /bsc/nonce (walletAddress)
    AP_Login->>Redis: Generate & store nonce for wallet
    Redis-->>AP_Login: Nonce stored
    AP_Login-->>Client: Return { nonce }

    Note over Client, DB: Step 2: Sign Message & Verify
    Client->>Client: User signs nonce via Wallet
    Client->>AP_Login: POST /bsc/check_proof (walletAddress, signature)
    AP_Login->>AP_Login: Verify cryptographic signature

    alt Signature Valid
        AP_Login->>DB: Check if wallet exists
        DB-->>AP_Login: Return user data
        AP_Login->>AP_Login: Generate JWT Auth & Refresh Token
        AP_Login->>AP_Login: Generate RSA Public Key
        AP_Login-->>Client: Return { auth, rf, key, extraData }
    else Signature Invalid
        AP_Login-->>Client: Return Error (Invalid Data)
    end
```

## 2. Server-to-Server Authentication Flow

When the game server (SmartFox) or marketplace needs to verify a user's action, it communicates with the `ap-login` service.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Unity Client
    participant SFS as sfs-game-1 (SmartFox)
    participant AP_Login as ap-login (VerifyLoginData)
    participant DB as Backend DB

    Client->>SFS: Connect & Login (sends auth JWT/Payload)
    SFS->>AP_Login: POST /bsc/verify (walletAddress, loginData + Server Bearer JWT)
    AP_Login->>AP_Login: Verify Server Bearer Token
    AP_Login->>AP_Login: Decode & Verify User's JWT (loginData)

    AP_Login->>DB: Fetch Account details (uid, nickName, etc)
    DB-->>AP_Login: Account Data

    AP_Login-->>SFS: Return Success (userId, userName, extraData)
    SFS->>SFS: Create User Session in Game Engine
    SFS-->>Client: Welcome to BombCrypto!
```
