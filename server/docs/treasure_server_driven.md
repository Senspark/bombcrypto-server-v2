# Server-driven treasure mode

The server plays treasure mode; the client only re-enacts. This replaced the client-driven loop
(`GET_BOMB_TARGET` / `START_PLANT_BOMB` / `RESPONSE_EXPLODE`), which never shipped and has been
removed from the server, the client and map-service.

Client hand-off: `bombcrypto-client-v2/docs/treasure-server-driven-client-guide.md`.

## Who does what

| | Before (removed) | Now |
|---|---|---|
| pick target | map-service (asked by client) | map-service, by itself |
| path-find + walk | client (`BotManager`/`BotMove`) | map-service (`auto/Pathfinder` = port of the client's `BFS.cs`), simulated tile by tile at `1000/speed` ms |
| plant | client asks, server validates | map-service, on arrival |
| fuse + blast + rewards roll | map-service | map-service |
| energy, crediting, pools, dangerous | game server | game server (`applyExplodeToHero`) |
| new map generation | game server | game server |
| client | plays | re-enacts `TREASURE_EVENTS` |

## Flow

```
client              game server (SmartFox)                    map-service                      Redis
  |-- START_TREASURE_MODE -->|                                    |                               |
  |                          | dangerous check, joinRoom          |                               |
  |                          |-- POST /sessions/{k}/auto/start -->| spawn heroes, start runner    |
  |<-- map, heroes, seq -----|<-- snapshot (blocks, heroes, seq) -|                               |
  |                          |                                    | loop: walk / plant / explode  |
  |                          |                                    |-- PUBLISH AP_MAP_TREASURE_EVENT_CHANNEL -->|
  |                          |<-- FastStreamRedis (SUBSCRIBE) ------------------------------------|
  |                          | MapTreasureEventRouter -> UserBlockMapManagerV2.onTreasureEvents   |
  |                          |   MOVE/PLANT/JOIN/LEAVE: forward                                   |
  |                          |   EXPLODE: mirror map, energy, rewards, pools, dangerous           |
  |<-- TREASURE_EVENTS ------|   (one push per batch, in seq order)                               |
  |                          |   map empty -> createNewMap -> POST /map -> NEW_MAP event ...      |
```

Everything a session publishes goes through one runner coroutine, so its batches are in seq order
on the stream; the router's striped executor keeps them in order per session on the server; the
client checks `seq` and resyncs on a gap.

## Commands

| Command | Kind | Handler |
|---|---|---|
| `START_TREASURE_MODE` | request | `StartTreasureModeHandler` — `START_PVE_V2`'s work (hash check, `getBombermanDangerous`, `joinRoom`) + `startTreasureMode()`. Calling again = resync |
| `STOP_TREASURE_MODE` | request | `StopTreasureModeHandler` — heroes stop, live bombs still explode and are credited |
| `PAUSE_TREASURE_MODE` / `RESUME_TREASURE_MODE` | request | `Pause/ResumeTreasureModeHandler` → `setTreasurePaused` — client paused: heroes halt at their next tile until resumed, live bombs still explode. The flag rides on every `auto/start` (START's optional `paused`, resyncs), keepalive re-sends it if MapService disagrees, STOP clears it |
| `TREASURE_EVENTS` | push | `{events: [...]}`, see the client guide for every field |

## The roster

The server decides which heroes play: `activeHeroes` of the session's data type, stage `WORK`,
energy > 0, sorted by id, max 15 (`desiredTreasureHeroes`). It is re-checked:

- after every stage/active change: `GoWorkV2`, `GoSleepV2`, `GoHomeV2`, `ChangeBomberManStageV2/V3`,
  `ActiveBomberV2`, `ActiveBombers` call `userBlockMapManagerV2.syncTreasureHeroes()`;
- after every batch containing an `EXPLODE` (energy may have hit 0) — only if the id set changed;
- on every keepalive tick.

Only the difference is sent (`POST /auto/heroes`, upsert/remove + reason); map-service answers with
`HERO_JOIN` / `HERO_LEAVE` events. Start, stop, roster edits and keepalive are serialized by
`_treasureSync` so they reach map-service in order (lock order: `_treasureSync` → `locker`).

## Explode handling

`treasureExplode` mirrors `blocksHit` into the server map, then runs `applyExplodeToHero`
(subEnergy, rewards → `addRewards`, pool registration, dangerous/kill, stamina shield). The blast is
always pushed, so the client map can never drift:

- a hero that is no longer valid (asleep, home, inactive, gone) still gets its blast pushed, with
  `hp` only (no rewards, nothing credited);
- a hero at 0 energy gets the blast's `hp` values too.

`map_now_empty` → `createNewMap(pushLegacyNewMap = false)` → `POST /map` → map-service respawns the
heroes, pauses them `TREASURE_MAP_RESET_PAUSE_MS` (3 s) and emits `NEW_MAP`; the server attaches the
new map to it. No `PVE_NEW_MAP` is pushed while treasure mode runs.

## Keepalive and recovery

map-service sessions only get HTTP traffic from the server, so a running game would hit its 45 min
idle sweep. Every 30 s (`IScheduler`, key `treasure-keepalive-<sessionKey>`) the server calls
`POST /auto/keepalive`:

| Answer | Action |
|---|---|
| `running: true` | reconcile the roster |
| `running: false` or `404` (map-service restarted / session swept) | re-init the session from the server's map, `auto/start` again, push a `RESYNC` event (full snapshot) |
| network error | log, try again next tick |

The keepalive is also the game's lease: map-service stops a game that got no request for 100 s
(`AUTO_LEASE_MS`), so a game server that died does not leave heroes clearing the map uncredited.
After a game server restart the client's `START_TREASURE_MODE` starts the game again.

A failed `POST /map` after a map clear also sets `_treasureNeedsResync`; the next roster sync or
keepalive resyncs.

## map-service API (auto play)

| Route | Body → answer |
|---|---|
| `POST /sessions/{k}/auto/start` | `{heroes: [{hero: HeroSnapshot, speed, bombCount, blockPass}], fuseMs?, mapResetPauseMs?, paused?}` → snapshot `{seq, serverTimeMs, fuseMs, heroes, bombs, blocks, awaitingNewMap, resumeAtMs, paused}` |
| `POST /sessions/{k}/auto/heroes` | `{upsert: [...], remove: [ids], reason}` → `{seq}`; `409` if not running |
| `POST /sessions/{k}/auto/pause` | `{paused}` → `{seq}`; `409` if not running |
| `POST /sessions/{k}/auto/stop` | → `200` |
| `POST /sessions/{k}/auto/keepalive` | → `{running, seq, paused}`; `404` if the session is gone |
| `GET /sessions/{k}/auto` | → snapshot (debugging) |

Pub/Sub channel `AP_MAP_TREASURE_EVENT_CHANNEL`, message = `TreasureEventBatch` JSON; nothing is stored,
every game server receives every batch and ignores sessions it does not own. DTOs: map-service `model/AutoDtos.kt` ↔ server
`mapservice/MapServiceDtos.kt` (keep in sync).

## Simulation rules (`map-service/auto/AutoPlay.kt`)

At every tile centre, in the client's `BotManager.OnUpdate` order:

1. No target, the target lost its last brick, or another hero now stands on it as its own target →
   pick one from the current tile (`Session.pickAutoTarget` → `findTargetWithFallbacks`, never
   sharing a cell another hero already stands on).
2. On the target → a live bomb on it: wait for that bomb; bomb capacity full: wait for the hero's
   earliest bomb; else plant (fuse = `time_bomb_explode`), then choose where to go next
   (`targetAfterPlant`): a free cell — no bomb, not another hero's target
   (`Session.pickFreeAutoTarget`). If there is none, or only one brick is left
   (`BotManager.SpawnBomb`'s `NumberOfBlock == 1`), the hero **stays** and re-plants on the same
   tile the instant its bomb explodes. So on the last bricks the heroes next to them stay put and
   everyone else stands still with no target.
3. Else walk: keep the current route while every remaining tile is still passable, otherwise BFS
   again (bombs and walls block; bricks block unless block-pass). A re-plan or stop emits `MOVE`.
4. No route → wait 250 ms and retry; unreachable for 3 s (`UnreachableRejectDelay`) → reject the
   target and pick another.

Actions are processed at their scheduled time in time order (a detonation wins a tie), however late
the runner wakes, so the stream is exact and deterministic.

## Tests

| Where | What |
|---|---|
| `map-service/src/test/.../auto/` | `AutoPlayTest` (timing, capacity, block-pass, unreachable, map reset, roster, resync), `AutoPlayFuzzTest` (random maps/heroes/churn replayed through `TreasureReplay`, liveness), `PathfinderTest`; `routes/AutoRoutesTest` (HTTP + runner) |
| `map-service` stress test | `./gradlew stressTest -Pargs="--users 300"` (default `--mode auto`; every event replayed, `ev_invalid` must be 0) |
| `server/ClientModule/src_test/.../treasure/` | `TreasureModeTest` (handler → manager → pushes, crediting, roster, map clear, keepalive resync, stop); `TreasureModeRedisE2ETest` (opt-in: `MAP_SERVICE_E2E_URL=http://localhost:8090`) |
| `server/DevTool/login` | `test/treasureReplay.test.ts` (offline); `npm run v6 --uid=<uid>` / `--all` (live, report shows protocol violations) |

## Known limits

- One map-service instance holds a session's game in memory. A restart loses it; the keepalive
  resyncs within 30 s (heroes teleport to fresh spawns, live bombs are lost, not credited).
- Heroes don't block each other (same as the client).
