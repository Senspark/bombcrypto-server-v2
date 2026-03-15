# API Reference: Market Service (`ap-market`)

The `ap-market` service manages the in-game item marketplace. All marketplace actions must be authenticated using the bearer token created by `ap-login`.

All REST endpoints below are relative to `http://localhost:9120/`.

## 1. Marketplace Core Operations

| Endpoint | Method | Description |
|---|---|---|
| `/order` | POST | Place an order for an item |
| `/cancel_order` | POST | Cancel a pending order |
| `/buy` | POST | Buy a listed item |
| `/sell` | POST | List an item for sale |
| `/edit` | POST | Edit the price of a listed item |
| `/cancel` | POST | Cancel an active listing |

## 2. Marketplace Queries

| Endpoint | Method | Description |
|---|---|---|
| `/get_config` | GET | Retrieve marketplace configurations and fees |
| `/get_my_item` | POST | Retrieve a list of the user's items currently in the market |
| `/get_selling` | GET | Query items currently listed for sale |
| `/get_ordering` | GET | Query items with pending buy orders |
| `/get_expensive` | GET | Retrieve a list of the most expensive items |
| `/get_fixed` | GET | Retrieve items with fixed prices |

## 3. General Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/` | GET | Health check. Returns `OK` with a 200 status code. |
| `/health` | GET | Health check. Returns `OK` with a 200 status code. |
