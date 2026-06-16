# Hero AI Audit: Treasure Hunt (Farming)

## 1. Executive Summary
This audit addresses reports of inefficient hero behavior in BombCrypto's Treasure Hunt mode, specifically the "pinging" of map corners and failure to prioritize the nearest chests.

Following a deep-dive investigation into the server-side architecture (Kotlin/SmartFoxServer), we have concluded that the current movement and targeting logic resides entirely within the **Unity Client**.

## 2. Technical Findings

### 2.1 Decision Making & Pathfinding
- **Observation:** Heroes occasionally enter a loop where they alternate between map corners without attacking chests.
- **Root Cause (Heuristic Fallback):** Pathfinding is client-side. When a hero without the `BLOCK_PASS` ability (Skill ID 7) targets a far-off chest (e.g., the one with the lowest HP), the A* algorithm may fail to find a valid path due to grid congestion. The client defaults to boundary coordinates (0,0, etc.) as an error fallback.
- **Recommendation:** Implement a re-targeting safety check in the client-side `Pathfinding` module to prevent boundary pinging when a path is not found.

### 2.2 Bomb Placement Logic
- **Observation:** Heroes continue moving toward distant targets even after planting a bomb on a nearby empty cell.
- **Root Cause (Stale Targeting):** The `StartExplodeV5Handler` on the server validates bomb placement but does not issue new targeting instructions. The client's AI loop does not refresh the target immediately after the `OnBombPlaced` event.
- **Recommendation:** Modify the client's `ActionController` to force a target re-evaluation (preferring `Nearest` over `Lowest HP`) immediately after a bomb is successfully planted.

## 3. Server-Side Status
- **Abilities:** All ability constants (e.g., `BLOCK_PASS`, `PIERCE_BLOCK`) are correctly mapped in `GameConstants.kt`.
- **Validation:** Grid positioning validation in `UserBlockMapManagerImpl.kt` is functional and correct.
- **No Configuration Found:** We verified that no "corner checkpoint" logic exists in the server database (`config_th_mode`) or handlers.

## 4. Proposed Client-Side Prompt for Investigation
Para prosseguir com a correção no Unity, utilize o seguinte prompt:
> "Analyze the `HeroAI` and `MovementController` classes for Treasure Hunt mode. Focus on:
> 1. Fallback behavior when `GetPath()` fails (prevent corner pinging).
> 2. Forcing a target refresh (`FindNearest()`) immediately after a bomb is dropped.
> 3. Priority heuristics (Distância vs HP)."

---
**Audit Date:** 2026-03-27  
**Status:** Concluded - Client-Side fix required.
