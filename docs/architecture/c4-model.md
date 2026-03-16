# System Context & Container Diagrams

This document uses the [C4 Model](https://c4model.com/) to illustrate the architecture of the BombCrypto Server V2 system.

## Level 1: System Context Diagram

This diagram shows the high-level interactions between the users, the BombCrypto system, and external services.

```mermaid
C4Context
    title System Context diagram for BombCrypto V2

    Person(player, "Player", "A user of the game, playing via Unity WebGL, PC, or Mobile.")

    System(bombcrypto, "BombCrypto Server V2", "Provides game modes (Treasure Hunt & Adventure), authentication, and marketplace functionality.")

    System_Ext(blockchain, "Blockchain Networks", "Handles web3 transactions (BSC, Polygon, RON, BAS, TON, Solana).")

    Rel(player, bombcrypto, "Plays game, logs in, trades items using")
    Rel(player, blockchain, "Signs transactions using web3 wallet")
    Rel(bombcrypto, blockchain, "Verifies proofs and processes on-chain actions")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

## Level 2: Container Diagram

This diagram dives deeper into the `BombCrypto Server V2` system, showing its microservices and data stores.

```mermaid
C4Container
    title Container diagram for BombCrypto Server V2

    Person(player, "Player", "A user playing the game.")

    System_Boundary(c1, "BombCrypto Server V2") {
        Container(ap_login, "API Login", "Node.js, Express", "Handles user authentication (Email/Password, TON, Solana, Web3).")
        Container(ap_market, "API Market", "Node.js, Express", "Handles the in-game item marketplace.")
        Container(sfs_game, "SmartFox Server (sfs-game-1)", "Java/Kotlin, SFS2X", "The core game server running Treasure Hunt and Adventure modes.")

        ContainerDb(db_backend, "Backend DB", "PostgreSQL", "Stores user accounts and authentication data.")
        ContainerDb(db_bombcrypto, "BombCrypto DB", "PostgreSQL", "Stores game data, heroes, houses, and market listings.")
        ContainerDb(redis, "Redis", "Redis", "Caching and real-time state synchronization.")
    }

    Rel(player, ap_login, "Authenticates via", "HTTP :8120")
    Rel(player, ap_market, "Trades items via", "HTTP :9120")
    Rel(player, sfs_game, "Plays game via", "TCP/UDP :9933, HTTP :8080")

    Rel(ap_login, db_backend, "Reads/Writes user data", "JDBC")
    Rel(ap_login, redis, "Caches session data", "Redis Protocol")

    Rel(ap_market, db_bombcrypto, "Reads/Writes market data", "JDBC")
    Rel(ap_market, redis, "Caches market state", "Redis Protocol")

    Rel(sfs_game, db_bombcrypto, "Reads/Writes game data", "JDBC")
    Rel(sfs_game, redis, "Caches game state", "Redis Protocol")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```
