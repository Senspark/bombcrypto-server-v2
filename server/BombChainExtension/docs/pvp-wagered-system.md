# PvP Wagered System - Documentation

## 📄 Overview
This document describes the implementation of the PvP Wagered System (Fases 0-8), focusing on the wager escrow logic, procedural map generation, and security integrity.

## 🏗️ Architecture
- **Wager Escrow**: Atomic debit from player account using `SELECT FOR UPDATE` and `fn_update_user_bcoin_transaction`.
- **Matchmaking**: ELO-based matching with bot-blocking in wagered mode.
- **Map Generation**: Multi-size procedural maps (Small, Medium, Large) with randomized tilesets.
- **Integrity**: HMAC SHA-256 result signing to prevent tampering.

## 💰 Fee Processing
- **Percentage**: 5% on the total pool.
- **Ledger**: All fees are logged in `pvp_fee_ledger` with `PENDING` status.
- **Batch Processor**: `PvpFeeProcessor` runs hourly to aggregate and transfer fees to the treasury wallet.

## 🧪 Test Suites (Phase 1-3)
- **Unit Logic**: `src_test/com/senspark/game/service/PvpWagerServiceTest.kt`
- **Map Generation**: `src_test/com/senspark/game/pvp/manager/PvpMapGeneratorMultiSizeTest.kt`
- **Integrity**: `src_test/com/senspark/game/pvp/security/PvpIntegrityConnectionTest.kt`
- **Stress**: `src_test/com/senspark/game/pvp/stress/PvpStressSimulationTest.kt`

## 🚀 Deployment
- **Scheduler**: Registered in `PvpZoneExtension.kt` via `registerFeeProcessingTask()`.
- **Database**: Requires `pvp_wager_pool`, `pvp_wager_entry`, and `pvp_fee_ledger` tables.

---
*Last Updated: 2026-04-28 by AI (Antigravity)*
