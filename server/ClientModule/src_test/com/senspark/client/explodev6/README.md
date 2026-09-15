# Explode V6 tests

In-process tests for server-assigned bomb targeting: `GET_BOMB_TARGET`, `START_PLANT_BOMB`, the
server-timed fuse that pushes `RESPONSE_EXPLODE`, and the move-time (speed) check. Spec:
`server/docs/explode_v6.md`; overview: `../../../../../../../explode-v6-flow.md`.

```
./gradlew :ClientModule:test                                  # all of it
./gradlew :ClientModule:test --tests "*PlantMoveSpeedTest*"   # one suite
```

No server, database or network — everything runs in the test JVM in a couple of seconds.

## How it is wired

A client in `ClientModule` talks to the **real** server classes:

```
FakeGameClient                       real server code
  getBombTargets() ───encrypt JSON──▶ GetBombTargetHandler   ┐
  startPlantBomb() ─────────────────▶ StartPlantBombHandler  ├▶ UserBlockMapManagerImpl (real)
                   ◀──decrypt JSON──  controller.send(...)   ┘        └▶ MapData (real BFS)

  fireFuse() ──────────────────▶ FakeScheduler.fire(key) ──▶ UserBlockMapManagerImpl.detonateBomb
                ◀── RESPONSE_EXPLODE push, if any ──────────         (real)
```

There is no client-initiated explode request in V6 -- the server times each bomb's own fuse
itself and pushes the result. `FakeScheduler` is the `IScheduler` `UserBlockMapManagerImpl`
resolves via DI (see `ServerTestBed`); it never fires anything on its own, a test decides when
(`ServerTestBed.fireFuse` / `FakeGameClient.fireFuse`) or whether (`FakeScheduler.fireAll`) a
fuse fires. Nothing here sleeps.

Requests go over the same path a live one does — the body is encrypted, wrapped in `{rid, data}`,
handed to `BaseEncryptRequestHandler.handleClientRequest`, decrypted and re-parsed with
`SFSObject.newFromJsonData` before the handler sees it. That round trip is not short-circuited on
purpose: it is what catches a field the client writes as a long and the server reads as an int.
The push side isn't encrypted/round-tripped the same way in the test bed -- `ServerTestBed.Push`
captures the `ISFSObject` `sendDataEncryption` was given directly.

Mocked (all outside V6's scope): the database, the `Hero` records, `IGameConfigManager`, and
SmartFox's session plumbing. The map is a fixture rather than `createRandomMap`, because the
layout is what every BFS assertion is about.

## Files

| | |
|---|---|
| `ServerTestBed.kt` | boots the handlers + a real `UserBlockMapManagerImpl` over a fixed map |
| `FakeScheduler.kt` | the `IScheduler` fake -- records a fuse instead of timing it; a test fires it |
| `FakeGameClient.kt` | the wire format, transcribed from `DefaultPveServerBridge.cs` |
| `ClientBombTargetManager.kt` | transcription of the client's `DefaultBombTargetManager` |
| `SimulatedHero.kt` | `BotManager`'s hunter-mode loop: walk, plant, reject, and pace bomb capacity against the server's own fuse via `fireFuse` |
| `TestMaps.kt` | ASCII map fixtures |

The client pieces are transcriptions, not stubs — `BotManager`'s three load-bearing guards and
`DefaultBombTargetManager`'s two backoffs are reproduced, so a client that lost one of them would
fail these tests rather than pass them.

## Suites

| Suite | Covers |
|---|---|
| `BombTargetPickerTest` | `MapData.findBombTarget` / `findAnyBombTarget` — which cell is picked, and why |
| `GetBombTargetTest` | bootstrap/resync: batching, seed hints, rejects, hero guards, omission |
| `StartPlantBombTest` | the target gate, the next-target handoff, bomb capacity, what a refused plant leaves behind |
| `PlantMoveSpeedTest` | the move-time check: rejection, tolerance, multiplier, detect-only, the speed-0 floor |
| `StartExplodeV6Test` | the fuse timer: when it produces a `RESPONSE_EXPLODE` push, bomb-id reuse, releasing the plant record before the hero guards even on no push |
| `ServerTargetStateTest` | per-hero state lifecycle: cell sharing, anchors, map regeneration (no TTL any more -- a bomb's own fuse is what releases its cell) |
| `ClientBombTargetManagerTest` | the client's cache, coalescing, in-flight expiry and both backoffs |
| `ExplodeV6FlowTest` | the whole loop: heroes farming a map without the two maps drifting apart |
| `ProtocolContractTest` | field names, field types and error codes, against what the Unity client reads |

## Two things to know when a test fails

**A handler that answers nothing has thrown.** `BaseEncryptRequestHandler` catches every exception,
logs it and returns without replying. `dispatch` surfaces `ServerTestBed.loggedErrors` in that case,
so read the assertion message before assuming the handler chose to stay silent.

**Flow tests stop at the first map regeneration** (`Farm.runUntilNewMap`). `createNewMap()`
deliberately discards every in-flight bomb and target, so rejections after it say nothing about the
steady-state loop.
