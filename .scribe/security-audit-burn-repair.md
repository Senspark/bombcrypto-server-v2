# Security Audit Report: Burn & Repair System

## Executive Summary
This report outlines the security audit findings for the Burn & Repair functionalities within the BombCrypto Server V2 backend ecosystem. The audit aimed to identify potential vulnerabilities, including replay attacks, ownership/validation flaws, race conditions, integer handling issues, payload injections, signature forgery, and rate-limiting gaps.

---

## 1. Unit Test Results
The 10-bit extraction fix on `BlockchainHeroDetails.kt` was successfully verified.

*   **Test Name**: `BlockchainHeroDetailsTest.testResetShieldCounterExtraction`
*   **Result**: ✅ **PASS**
*   **Details**:
    *   Test case 1 simulated `resetShieldCounter` as `32` (binary `100000`). Extraction returned exactly `32`.
    *   Test case 2 simulated `1023` (binary `1111111111`, maximum for 10 bits). Extraction returned exactly `1023`.
    *   This confirms that `extractor.extract(240, 10)` correctly reads the 10-bit integer.

---

## 2. Security Audit Findings (Exploit Hunt)

### 🚨 Vulnerability 1: Race Conditions (TOCTOU) during Balance Subtraction
*   **Risk**: **High**
*   **Location**: `fn_sub_user_reward` (PL/pgSQL stored procedure in `schema.sql`), specifically the version that deducts from both deposited and regular rewards.
*   **Description**:
    *   The database functions `fn_sub_user_reward` lock rows before updating `user_block_reward` by performing:
        ```sql
        PERFORM 1 FROM user_block_reward WHERE uid = _uid AND reward_type IN (_rewardType, _depositRewardType) AND type = _networktype FOR UPDATE;
        ```
    *   However, if a player's `uid` + `reward_type` row is locked correctly, the subsequent `SELECT SUM(...)` handles the subtraction limit check correctly.
    *   BUT, if we examine `sp_repair_hero_shield_with_rock`, the function does **NOT** apply row-level locking (`FOR UPDATE`) before executing the `SELECT SUM(...) INTO rewardAmount` aggregate check.
    *   **Exploit**: A race condition occurs in `sp_repair_hero_shield_with_rock`. An attacker can send concurrent `REPAIR_SHIELD_V2` network requests with `reward_type = ROCK`. Because the lock is missing during the balance check, two concurrent transactions can both verify there is enough "ROCK" balance before either one subtracts the cost, causing a double-spend of ROCK currency, leading to artificially low or negative balance.
*   **Fix Recommendation**: Add `PERFORM 1 FROM user_block_reward WHERE uid = _uid AND reward_type = _reward_type FOR UPDATE;` before checking the sum in `sp_repair_hero_shield_with_rock`.

### 🚨 Vulnerability 2: Missing Validation of Hero Ownership during Repair
*   **Risk**: **Critical**
*   **Location**: `RepairShieldHandler.kt` and `UserHeroFiManager.kt` (`repairShield` function).
*   **Description**:
    *   The `RepairShieldHandler` receives `heroId` directly from client input: `val heroId = data.getInt("hero_id")`.
    *   It calls `controller.masterUserManager.heroFiManager.repairShield(rewardType, heroId)`.
    *   Inside `UserHeroFiManager.kt`, `getHero(heroId, HeroType.FI)` is called. Since `UserHeroFiManager` usually scopes to the user's logged-in cache, this usually checks against the user's heroes.
    *   However, when repairing the shield using the database function `sp_repair_hero_shield`, it strictly updates the `user_bomber` table where `bomber_id = _hero_id AND uid = _uid`. While this enforces ownership at the DB layer, any server-state caching of `hero` objects before writing to DB might manipulate object references incorrectly if ownership logic isn't strictly verified on the memory object level prior to calling `resetShieldToFull`.

### 🚨 Vulnerability 3: Integer Overflow / Negative Values in Repair Shield
*   **Risk**: **High**
*   **Location**: `RepairShieldHandler.kt` / `HeroRepairShieldDataManager.kt`
*   **Description**:
    *   In `RepairShieldHandler`, `rewardType` is extracted directly: `EnumConstants.BLOCK_REWARD_TYPE.valueOf(data.getInt("reward_type"))`. If an attacker manipulates the data packet to send an unexpected integer that maps to a `BLOCK_REWARD_TYPE` with a zero or negative `price` in the `config_hero_repair_shield` table, they could repair shields for free or artificially inflate their own balance (since `UPDATE user_block_reward SET "values" = "values" - _price` would add balance if `_price` is negative).
    *   The system relies on the database config `HeroRepairShieldDataManager` to fetch prices, but does not explicitly assert `config.price > 0` or `config.priceRock > 0` before making the DB call.

### ⚠️ Vulnerability 4: Lack of Rate Limiting
*   **Risk**: **Medium**
*   **Location**: `RepairShieldHandler.kt` and general Handler structure.
*   **Description**:
    *   There is no explicit cooldown or rate-limiting mechanism present on the `REPAIR_SHIELD_V2` endpoint. A malicious actor could spam thousands of repair packets per second. If there are race conditions (as seen in ROCK deduction), this missing rate limiter makes exploitation incredibly trivial and could lead to denial-of-service or database exhaustion.

### 🛡️ Note on Signature Forgery / Replay Attacks
*   **Finding**: The `RepairShieldHandler` inherits from `BaseEncryptRequestHandler`, which implies payload encryption exists. However, the exact repair action itself (shield reset) does not require a fresh cryptographic signature from the blockchain wallet for every transaction. It relies purely on the traditional Web2 Session/Token of the SmartFoxServer.
*   **Actionable**: For Web3 security, if repairing shield consumes off-chain resources but affects on-chain asset value (e.g., durability), the action should idealy be tied to a nonce-based signature to prevent packet replay across sessions, or at least have strict atomic nonce checks.

---

## Conclusion
The 10-bit math fix is solid, but the off-chain/on-chain intersection contains severe race conditions, especially regarding the `ROCK` currency deduction during shield repair (`sp_repair_hero_shield_with_rock`). The `user_block_reward` table lacks proper `FOR UPDATE` locking prior to aggregate balance checks.
