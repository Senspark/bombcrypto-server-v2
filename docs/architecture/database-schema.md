# Database Schema Diagrams

This document outlines the core entity-relationship (ER) diagrams for the BombCrypto Server V2 databases, `backend` (used by ap-login) and `bombcrypto` (used by the game server and marketplace).

## 1. Backend Database (`backend`)

This database handles user registration and authentication.

```mermaid
erDiagram
    USERS {
        int id PK
        varchar username "Wallet address or email"
        varchar nickname
        varchar email
        varchar type_account
        timestamp create_at
        varchar telegram_id
        timestamp last_login
    }
```

## 2. BombCrypto Database (`bombcrypto`)

This database contains the core game logic, user assets (heroes, houses), and marketplace data. Due to the large number of tables, this diagram highlights the most critical entities and their relationships.

```mermaid
erDiagram
    USER ||--o{ USER_BOMBER : owns
    USER ||--o{ USER_HOUSE : owns
    USER ||--o{ USER_MATERIAL : holds
    USER ||--o{ USER_MARKETPLACE : trades
    USER ||--o{ USER_PVP : plays

    USER {
        int id_user PK
        varchar username
        int bcoin "In-game token"
        int sen "In-game token"
    }

    USER_BOMBER {
        int uid FK
        int id PK "Hero ID"
        int bomber_id
        int rarity
        int power
        int speed
        int stamina
        int bomb_num
        int bomb_range
        timestamp create_at
    }

    USER_HOUSE {
        int uid FK
        int id PK
        int house_id
        int rarity
        timestamp create_at
    }

    USER_MATERIAL {
        int uid FK
        int item_id
        int amount
    }

    USER_MARKETPLACE {
        int id PK
        int uid FK
        int item_id
        int item_type
        decimal price
        int status "Listing status"
        timestamp create_at
    }

    USER_PVP {
        int uid FK
        int rank
        int mmr
        int wins
        int losses
    }
```
