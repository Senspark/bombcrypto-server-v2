# MapService

Standalone Kotlin/Ktor HTTP service that plays Bombcrypto's treasure mode for the game server: it
holds each session's map, walks every hero, picks bomb targets, plants, times the fuse and
calculates the blast and rewards. Spec: `bombcrypto-server-v2/server/docs/treasure_server_driven.md`.

It has **no database** and no dependency on the SmartFoxServer extension project. It is called by
the main game server over HTTP, and its only outbound dependency is **Redis** — the same Redis the
main game server uses: every step of a running game is published to the
`AP_MAP_TREASURE_EVENT_CHANNEL` Pub/Sub channel (see *Server-driven treasure mode* below).

Deliberately **excluded** (this stays on the main server, which owns the DB, the live `Hero`
objects, wallet crediting, and the SFS session):

- Hero energy / "dangerous" / kill-bomberman mutation.
- Wallet crediting and DB persistence.
- Cross-user treasure-hunt pool registration.
- Procedural map generation (needs DB-backed block/config data) — the main server generates a new
  map when the current one clears and pushes it here via `POST /sessions/{key}/map`.

## Running

```sh
./gradlew run                      # port 8090 by default
MAP_SERVICE_PORT=9000 ./gradlew run # custom port
```

| Env | Default | |
|---|---|---|
| `MAP_SERVICE_PORT` | `8090` | HTTP port |
| `REDIS_CONNECTION_STRING` | `redis://localhost:6379` | same value (and var name) as `bombcrypto-server-v2`'s |
| `REDIS_RETRY_MS` | `3000` | delay between Redis connect retries at startup; MapService serves requests while Redis is down (events are dropped + logged) |
| `BOMB_FUSE_MS` | `3000` | fuse used when `/auto/start` doesn't send `fuseMs` |
| `AUTO_LEASE_MS` | `100000` | a running game that gets no request (keepalive included) for this long is stopped |

MapService boots without Redis (it connects lazily and retries on the next publish), but events
can't reach the game server until Redis is reachable.

Or build a fat jar and run it directly:

```sh
./gradlew shadowJar
java -jar build/libs/mapservice.jar
```

### Docker

```sh
docker build -t mapservice .
docker run -p 8090:80 mapservice   # container listens on 80 internally, matching this stack's other API services
```

## Session model

State is scoped to a **session key**, a plain string the caller chooses — the main server uses
`"$userId-$dataType-$mode"`, mirroring how it already scopes the DB row for a user's map. Each
session holds its own map, per-hero target/plant/reject state, its auto-play game, and its own lock
(so two different users' requests never block each other; two requests for the *same* session do).

Sessions live only in memory. A restart loses all of them — callers should treat a `404` on any
session-scoped endpoint as "does not exist (yet, or any more)" and recover by re-`POST`ing `/init`
with their own cached copy of the current map, then retrying the original call once. An idle
session (no request for 45 minutes) is swept automatically as a safety net for a missed
`DELETE .../sessions/{key}` on logout.

## API

All request/response bodies are JSON. Errors use `{"error": "<message>"}` with a non-2xx status.

### `POST /config`

Global (non-session) config update — call whenever the main server's own reward-table config
reloads. New sessions default to whichever config was last pushed here unless their own `/init`
body overrides it.

```jsonc
{
  "rewardConfig": {
    "rewardTables": { "TR-3": [{"type": "COIN", "weight": 100, "minValue": 1.0, "maxValue": 2.0}] },
    "minStakeBcoinTHV1": [0, 10, 20],
    "minStakeSenTHV1": [0, 5, 10],
    "minStakeHeroConfig": {"0": 0, "1": 10}
  }
}
```

`rewardTables` is keyed `"$dataType-$blockType"`, matching `BlockRewardDataManager`'s own key
shape exactly (`dataType` here is whatever string the caller uses in `HeroSnapshotDto.dataType`,
e.g. `"TR"`, `"TON"`, `"SOL"`). Each entry's value is rolled uniformly between `minValue` and
`maxValue`, exactly like `BlockReward.getValue` on the main server.

### `POST /sessions/{key}/init`

Creates a session. `409 session_already_exists` if it's already there (use `/map` instead).

```jsonc
{
  "blocks": [{"i": 1, "j": 0, "type": 3, "hp": 40, "maxHp": 40}],
  "tileset": 0,
  "mode": "PVE_V2",
  "rewardConfig": { /* optional, same shape as POST /config's rewardConfig; overrides the global default for this session */ }
}
```

### `POST /sessions/{key}/map`

Replaces the map (a map-clear reload) and resets all per-hero target/plant/reject tracking for the
session — every tracked coordinate is only valid against the map it was picked from. A running
auto-play game respawns its heroes and emits `NEW_MAP`. `404 session_not_found` if the session doesn't
exist yet (caller should use `/init` instead). Body is the same shape as `/init`'s `blocks`/
`tileset`/`mode` (no config fields — config isn't touched by a map replace).

### `DELETE /sessions/{key}`

Teardown. Idempotent — `200` whether or not the session existed. Call this on user logout.

## Server-driven treasure mode (auto play)

The game server's `START_TREASURE_MODE` hands a session's whole game to MapService: it walks every
hero tile by tile with the client's own path-finding (`auto/Pathfinder.kt`, a port of the client's
`BFS.cs`), plants on arrival, explodes each bomb after its fuse and picks the next target, then
streams every step to Redis. Spec: `bombcrypto-server-v2/server/docs/treasure_server_driven.md`.

| Route | Body → answer |
|---|---|
| `POST /sessions/{key}/auto/start` | `{"heroes":[{"hero":HeroSnapshot,"speed":5,"bombCount":2,"blockPass":false}],"fuseMs":3000,"mapResetPauseMs":3000,"paused":false}` → snapshot `{seq, serverTimeMs, fuseMs, heroes:[{heroId,i,j}], bombs:[...], blocks:[...], awaitingNewMap, resumeAtMs, paused}`. Calling it again while running is a resync (positions and live bombs kept) |
| `POST /sessions/{key}/auto/heroes` | `{"upsert":[...],"remove":[heroId],"reason":"no_energy"}` → `{seq}`; `409 auto_mode_not_running` |
| `POST /sessions/{key}/auto/pause` | `{"paused":true}` → `{seq}`; heroes halt until resumed, live bombs still explode; `409 auto_mode_not_running` |
| `POST /sessions/{key}/auto/stop` | heroes leave; live bombs still explode |
| `POST /sessions/{key}/auto/keepalive` | → `{running, seq, paused}` — the game server's lease: keeps the session out of the idle sweep and its game running. A game with no request for `AUTO_LEASE_MS` is stopped (as `/auto/stop`), because nobody is crediting its explosions; `/auto/start` revives it |
| `GET /sessions/{key}/auto` | → current snapshot (debugging) |

`HeroSnapshot` is the hero's combat/economy stats: `{heroId, bombRange, pierceBlock, damageTreasure,
damageJail, totalPower, stakeBcoin, stakeSen, rarity, isHeroS, dataType, isAirdropUser}`. MapService
only adds `totalPower` on top (`damTreasure = damageTreasure + totalPower`).

`POST /sessions/{key}/map` respawns the heroes, pauses them `mapResetPauseMs` and emits `NEW_MAP`.

Events are PUBLISHed on the Redis Pub/Sub channel **`AP_MAP_TREASURE_EVENT_CHANNEL`** as a `TreasureEventBatch`
(`{"sessionKey":"1-TR-PVE_V2","events":[...]}`, nulls omitted), one runner coroutine per session so
a session's `seq` is always in order on the channel:

| `type` | fields |
|---|---|
| `MOVE` | `heroId`, `i`,`j` (from), `path:[{i,j}]`, `stepMs` — empty path = stop on (i,j) |
| `PLANT` | `heroId`, `i`,`j`, `bombNo`, `plantedAtMs`, `explodeAtMs` |
| `EXPLODE` | `heroId`, `i`,`j`, `bombNo`, `plantedAtMs`, `explodeAtMs`, `takeResult`, `blocksHit`, `mapNowEmpty` |
| `HERO_JOIN` / `HERO_LEAVE` | `heroId`, `i`,`j` (+ `reason`) |
| `NEW_MAP` | `heroes:[{heroId,i,j}]`, `resumeAtMs` |

Every event also has `seq` (+1 per event) and `atMs`.

`EXPLODE.takeResult` is `OK`, `ALREADY_TAKEN` or `CANNOT_SET_BOOM`; on the last two `blocksHit` is
empty. Every block in the blast appears in `blocksHit` after damage, destroyed or not: `{i, j, hp,
type, rewards:[{type, value}]}`. `rewards` is per block and only filled on a destroyed block.
`type` is the block's `GameConstants.BlockType` code, so the caller can derive pool eligibility.

Entries nobody owns (user logged out mid-game) expire after ~20s: `MINID ~` trimming on each XADD
plus a 20s `PEXPIRE` on the key.

## Block type codes

Compiled in (`GameConstants.BlockType`), matching `com.senspark.game.declare.GameConstants.BLOCK_TYPE`
on the main server exactly: `ROCK=0, NORMAL=1, JAIL=2, WOODEN=3, SILVER=4, GOLDEN=5, DIAMOND=6, LEGEND=7`.

## Stress testing

`com.senspark.mapservice.stresstest.*` is a standalone load-test client, built into this same
Gradle project, that simulates any number of concurrent players calling MapService directly, the
way `bombcrypto-server-v2`'s `UserBlockMapManagerV2` drives a real user, with no server or Unity
client involved. Each virtual user gets its own session key (`stress-auto-<n>-<dataType>-PVE_V2`)
and hero roster, so a run never touches, and is invisible to, `bombcrypto-server-v2` or any real
player session.

Start MapService (with `REDIS_CONNECTION_STRING` pointed at a Redis you're fine watching traffic on --
the stress test only ever reads that stream, never ACKs/deletes from it, so it's safe to point at the
same Redis a real `bombcrypto-server-v2` is using), then in a second terminal:

```sh
./gradlew stressTest -Pargs="--users 10 --duration 60"
```

Each virtual user plays **server-driven treasure mode** exactly like the game server drives a real
user: `/init`, `/auto/start` with its heroes, then only reacts — reads its
batches off `AP_MAP_TREASURE_EVENT_CHANNEL`, pushes a fresh `/map` when an `EXPLODE` says the map is
empty, edits the roster now and then (`/auto/heroes`), and calls `/auto/keepalive`. Every event is
replayed through `stresstest/TreasureReplay.kt` (the same rules the client follows), so a broken
stream shows up as `ev_invalid` errors.

The number of simulated concurrent users is the one flag you'll change most: `--users 200`, etc.
Other options (see `--help`, or run with no args for the default 10 users / 60s):

```
--base-url <url>            MapService base URL (default http://localhost:8091, i.e.
                              DEFAULT_BASE_URL in StressTestConfig.kt, or $MAP_SERVICE_URL)
--redis-url <url>            Redis MapService publishes treasure events to -- must be the same
                              Redis MapService itself was started with (default
                              redis://localhost:6379 or $REDIS_CONNECTION_STRING)
--users <n>                  Concurrent virtual users (default 10)
--heroes-per-user <n>        Heroes simulated per user (default 15)
--hero-rarity <n>            Fixed rarity for every generated hero (default 5)
--bombs-per-hero <n>         Max concurrent planted bombs per hero (default 6)
--duration <seconds>         Test duration (default 60)
--ramp-up <seconds>          Spread user start times over this window (default 5)
--think-time-ms <ms>         Delay between ticks per user (default 200)
--fuse-ms <ms>               Bomb fuse length sent on auto/start (default 3000)
--map-density <0..1>         Block density for generated maps (default 0.35)
--map-reset-pause-ms <ms>    Heroes' pause after a new map (default 3000)
--churn-per-min <n>          Roster edits per user per minute (default 2)
--block-pass-ratio <0..1>    Fraction of block-pass heroes (default 0.1)
--airdrop-ratio <0..1>       Fraction of heroes flagged as airdrop users (default 0.2)
--print-interval <seconds>   Periodic report interval (default 5)
--seed <long>                RNG seed, for reproducible runs (default: current time)
--report-dir <dir>           Save full console output here as a timestamped .log file
                              (default "report"; pass "" to disable)
```

The report has one `ev_<TYPE>` line per event type — its latency is how long after
the event's own time it was read off Redis (runner + publish + stream lag) — plus `ev_invalid`
(must stay 0) and `maps cleared`. Reference run on a laptop: 300 users × 15 heroes, ~5,500 events/s,
event lag p99 4 ms, 0 invalid, 0 errors.

It also prints a live per-endpoint report (`init`/`auto_start`/`auto_heroes`/`keepalive`/`map`/
`delete`/`config`) every `--print-interval` seconds with request counts, error counts, throughput
and latency percentiles (p50/p95/p99). A final report prints when the run ends.

The same output (startup line, every periodic report, and the final report) is also saved to
`report/stress-test-<yyyyMMdd-HHmmss>.log` as it's printed, so a run's results survive after the
terminal scrolls away. Pass `--report-dir ""` to disable saving, or `--report-dir <other-dir>` to
save elsewhere.

## Testing it by hand

```sh
BASE=http://localhost:8090
KEY=1-TR-PVE_V2

curl -X POST $BASE/sessions/$KEY/init -H 'Content-Type: application/json' -d '{
  "blocks": [{"i":1,"j":0,"type":3,"hp":1,"maxHp":1}],
  "rewardConfig": {"rewardTables": {"TR-3": [{"type":"COIN","weight":100,"minValue":1.0,"maxValue":2.0}]}}
}'

curl -X POST $BASE/sessions/$KEY/auto/start -H 'Content-Type: application/json' -d '{
  "heroes": [{"speed":5,"bombCount":1,
    "hero":{"heroId":1,"bombRange":2,"pierceBlock":false,"damageTreasure":10,"damageJail":10,
            "totalPower":0,"stakeBcoin":0,"stakeSen":0,"rarity":0,"isHeroS":false,
            "dataType":"TR","isAirdropUser":false}}]
}'

curl $BASE/sessions/$KEY/auto                       # snapshot
curl -X POST $BASE/sessions/$KEY/auto/keepalive     # {running, seq, paused}
```

how to rebuild:
cd ~/open-source/bombcrypto-server-v2/server/deploy
docker compose -f compose-dev.yaml up -d --build map-service