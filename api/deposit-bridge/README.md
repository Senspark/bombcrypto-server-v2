# ap-deposit-bridge

Cross-chain bridge **executor**. Owns the server-submitted withdraw state machine end-to-end:
the whole `fn_bridge_*` mutation set (deduct / clear / refund / credit), on-chain submission of
`withdrawTo` (relayer key, sequential per-chain nonce + RBF), receipt tracking, and a
reconciler that drains the durable `cross_chain_bridge_pending` queue on restart.

It serves **nothing to clients** — no websocket, no client-facing HTTP (only `/health`). It talks
to SmartFox over Redis streams. See `docs/cross-chain-bridge-phase8-server-submit-plan.md`.

## Flow

```
SmartFox ──SV_DEPBRIDGE_REQUEST_STR──▶ Executor
  (withdraw | deposit-confirm, correlationId)     │ read on-chain before → fn_bridge_request_withdraw
                                                  │ submit withdrawTo → fn_bridge_mark_submitted
                                                  │ wait receipt (budget) → fn_bridge_sync_withdraw
  ◀──AP_DEPBRIDGE_RESULT_STR──────────────────────┘ {code, txHash, netWei, status}
  ◀──AP_DEPBRIDGE_REFUND_STR── (async: refund/confirm that landed after the request timed out)
```

- **Durability**: the DB pending table is the work queue. A missed request stream message just
  times out the caller (no balance moved); anything already committed to a pending row is finished
  by the reconciler. Refund is only ever applied after the tx is proven dead (§4.5).

## Run

```sh
npm install
npm start            # tsc --noEmit + vite-node (dev)
npm run start:release # tsc + tsc-alias + node dist/Server.js
```

Config via env (`.env`, see `.env.example`). The relayer key must hold `RELAYER_ROLE` on each
chain's `DepositBridge` proxy and be funded with native gas.
