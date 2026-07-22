-- Migration: Cross-chain bridge (server-submitted withdraw) — full feature, single file
-- Date: 2026-07-08
-- Applies to: bombcrypto database (FRESH installs)
--
-- Supersedes the deleted 20260701_110214_add_cross_chain_bridge.sql. That earlier file only
-- ever ran on the local dev DB; nowhere else. This is the complete, self-contained bridge
-- migration = the old schema + the Phase-8 "Option 2" (server-submitted withdraw) additions.
-- See docs/cross-chain-bridge-phase8-server-submit-plan.md §9 and
-- docs/cross-chain-balance-impl-plan.md §D.1/§I/§J.
--
-- To upgrade the local dev DB (which already ran the OLD version) run the throwaway
-- server/db/PATCH_bridge_server_submit_upgrade.sql ONCE instead of this file, then delete it.
--
-- Option 2 (this file): the backend (ap-deposit-bridge) submits withdraw txs itself via a
-- role-gated withdrawTo(). No signature is ever handed to the client. The pending row grows a
-- tx lifecycle (status/tx_hash/nonce/lease) so a restartable backend can reconcile in-flight
-- txs, and a new fn_bridge_refund() safely returns balance when a tx is definitively dead.
--
-- Units (§I): every quantity that touches the blockchain is stored as EXACT integer wei
-- (numeric). Only user_block_reward."values" / history.balance_after are double precision.
-- Convert exactly once at the boundary: values += delta_wei / 1e18.
--
-- Kill-switch (§J): two boolean flags live in the existing game_config table
-- (bridge_deposit_enabled / bridge_withdraw_enabled), loaded at boot by loadGameConfig().

BEGIN;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

-- Per-network watermarks of the on-chain cumulative counters (exact wei).
CREATE TABLE public.cross_chain_bridge_sync (
    uid             integer NOT NULL,
    reward_type     character varying(50) NOT NULL,   -- BCOIN_BRIDGE | SEN_BRIDGE
    chain           character varying(20) NOT NULL,   -- BSC | POLYGON
    synced_deposit  numeric DEFAULT 0 NOT NULL,       -- exact on-chain deposited[user][token] wei
    synced_withdraw numeric DEFAULT 0 NOT NULL,       -- exact on-chain withdrawn[user][token] wei
    PRIMARY KEY (uid, reward_type, chain)
);

-- At most ONE active withdraw per (user, token) — enforced by the PK (no chain).
-- The tx-lifecycle columns (status .. updated_at) let the restartable backend own submission
-- and reconciliation: REQUESTED -> SUBMITTED -> (deleted on confirm) | FAILED -> (refund/retry).
CREATE TABLE public.cross_chain_bridge_pending (
    uid           integer NOT NULL,
    reward_type   character varying(50) NOT NULL,
    chain         character varying(20) NOT NULL,       -- target chain of this withdraw
    gross         numeric NOT NULL,                     -- exact wei
    before_value  numeric NOT NULL,                     -- on-chain withdrawn[user] at request (CAS anchor)
    created_at    timestamptz DEFAULT now() NOT NULL,
    status        character varying(20) NOT NULL DEFAULT 'REQUESTED',  -- REQUESTED | SUBMITTED | FAILED
    tx_hash       character varying(80),                -- null until broadcast
    nonce         bigint,                               -- relayer nonce used (RBF/tracking)
    submitting_at timestamptz,                          -- in-flight lease; null = not in-flight
    attempts      integer NOT NULL DEFAULT 0,
    last_error    text,
    updated_at    timestamptz DEFAULT now() NOT NULL,
    PRIMARY KEY (uid, reward_type)
);

-- Append-only audit trail; id is the monotonic cursor the monitor reads (§H).
CREATE TABLE public.cross_chain_bridge_history (
    id              bigserial PRIMARY KEY,
    uid             integer NOT NULL,
    reward_type     character varying(50) NOT NULL,
    chain           character varying(20) NOT NULL,
    action          character varying(30) NOT NULL,    -- DEPOSIT | WITHDRAW_REQUEST | WITHDRAW_CONFIRM | WITHDRAW_REFUND | ANOMALY
    amount          numeric NOT NULL,                  -- delta (deposit) or gross (withdraw), exact wei
    balance_after   double precision,                  -- game balance snapshot (values)
    onchain_counter numeric,                           -- on-chain counter read at this transition
    before_value    numeric,                           -- CAS anchor (withdraw)
    signature       character varying,                 -- vestigial (Option 2 hands out no signatures); kept nullable for compat
    created_at      timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_ccb_history_uid ON public.cross_chain_bridge_history (uid, reward_type);
CREATE INDEX idx_ccb_history_action ON public.cross_chain_bridge_history (action);

-- ---------------------------------------------------------------------------
-- Kill-switch flags (§J) — reuse the existing game_config key/value store.
-- ---------------------------------------------------------------------------
INSERT INTO public.game_config (key, value)
VALUES ('bridge_deposit_enabled', '1'), ('bridge_withdraw_enabled', '1')
ON CONFLICT (key) DO NOTHING;

INSERT INTO config_th_mode (key, value, network) VALUES
  ('bridge_fee_percent', '5', 'BSC'),
  ('bridge_fee_percent', '5', 'POLYGON');

-- ---------------------------------------------------------------------------
-- Functions
-- ---------------------------------------------------------------------------

-- Deposit sync (add-only): credit the delta between the on-chain cumulative
-- deposited counter and our watermark, then advance the watermark to exact.
CREATE OR REPLACE FUNCTION public.fn_bridge_sync_deposit(
    _uid integer,
    _reward_type character varying,
    _balance_type character varying,   -- unified-balance sentinel (server-owned constant, e.g. 'BP')
    _chain character varying,
    _onchain_deposited numeric
) RETURNS text
    LANGUAGE plpgsql
AS
$function$
DECLARE
    _synced   numeric          := 0;
    _delta    numeric          := 0;
    _credited double precision := 0;
    _balance  double precision := 0;
    _hid      bigint;
BEGIN
    SELECT synced_deposit INTO _synced
    FROM cross_chain_bridge_sync
    WHERE uid = _uid AND reward_type = _reward_type AND chain = _chain;
    IF _synced IS NULL THEN _synced := 0; END IF;

    _delta := _onchain_deposited - _synced;

    IF _delta = 0 THEN
        RETURN json_build_object('credited_wei', '0', 'id', NULL, 'balance', COALESCE((
            SELECT "values" FROM user_block_reward
            WHERE uid = _uid AND reward_type = _reward_type AND type = _balance_type), 0))::text;
    END IF;

    IF _delta < 0 THEN
        RAISE EXCEPTION 'bridge deposit watermark went backwards: onchain=% < synced=% (uid=%, %, %)',
            _onchain_deposited, _synced, _uid, _reward_type, _chain;
    END IF;

    _credited := (_delta / 1000000000000000000::numeric)::double precision;

    -- advance the per-chain watermark to the exact on-chain value
    INSERT INTO cross_chain_bridge_sync (uid, reward_type, chain, synced_deposit, synced_withdraw)
    VALUES (_uid, _reward_type, _chain, _onchain_deposited, 0)
    ON CONFLICT (uid, reward_type, chain)
        DO UPDATE SET synced_deposit = EXCLUDED.synced_deposit;

    -- credit the unified spendable balance row (uid, reward_type, 'BP')
    INSERT INTO user_block_reward (uid, reward_type, type, "values", total_values, modify_date, last_time_claim_success)
    VALUES (_uid, _reward_type, _balance_type, _credited, _credited, NOW() AT TIME ZONE 'utc', NOW() AT TIME ZONE 'utc')
    ON CONFLICT (uid, type, reward_type)
        DO UPDATE SET "values"      = user_block_reward."values" + EXCLUDED."values",
                      total_values  = user_block_reward.total_values + EXCLUDED.total_values,
                      modify_date   = NOW() AT TIME ZONE 'utc'
    RETURNING "values" INTO _balance;

    INSERT INTO cross_chain_bridge_history (uid, reward_type, chain, action, amount, balance_after, onchain_counter, before_value, signature, created_at)
    VALUES (_uid, _reward_type, _chain, 'DEPOSIT', _delta, _balance, _onchain_deposited, NULL, NULL, now())
    RETURNING id INTO _hid;

    RETURN json_build_object('credited_wei', _delta::text, 'id', _hid, 'balance', _balance)::text;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$function$;

-- Withdraw request: lock the balance, enforce single-pending, debit, record the
-- CAS anchor (on-chain withdrawn before this withdraw). New pending starts in REQUESTED;
-- the backend advances it to SUBMITTED once it broadcasts the tx. Returns {before, gross, id}.
CREATE OR REPLACE FUNCTION public.fn_bridge_request_withdraw(
    _uid integer,
    _reward_type character varying,
    _balance_type character varying,   -- unified-balance sentinel (server-owned constant, e.g. 'BP')
    _chain character varying,
    _gross numeric,
    _onchain_before numeric
) RETURNS text
    LANGUAGE plpgsql
AS
$function$
DECLARE
    _balance double precision := 0;
    _need    double precision := 0;
    _hid     bigint;
BEGIN
    -- One in-flight withdraw per user across ALL tokens/chains. Advisory lock (auto-released at commit)
    -- serializes concurrent requests for this uid — the per-token FOR UPDATE below can't span tokens.
    PERFORM pg_advisory_xact_lock(778811, _uid);

    SELECT "values" INTO _balance
    FROM user_block_reward
    WHERE uid = _uid AND reward_type = _reward_type AND type = _balance_type
    FOR UPDATE;

    IF _balance IS NULL THEN
        RAISE EXCEPTION 'no bridge balance for uid=%, %', _uid, _reward_type;
    END IF;

    IF EXISTS (SELECT 1 FROM cross_chain_bridge_pending WHERE uid = _uid) THEN
        RAISE EXCEPTION 'a withdraw is already pending for uid=%', _uid;
    END IF;

    _need := (_gross / 1000000000000000000::numeric)::double precision;
    IF _balance < _need THEN
        RAISE EXCEPTION 'insufficient balance: have %, need % (uid=%, %)', _balance, _need, _uid, _reward_type;
    END IF;

    UPDATE user_block_reward
    SET "values"    = "values" - _need,
        modify_date = NOW() AT TIME ZONE 'utc'
    WHERE uid = _uid AND reward_type = _reward_type AND type = _balance_type
    RETURNING "values" INTO _balance;

    INSERT INTO cross_chain_bridge_pending (uid, reward_type, chain, gross, before_value, status, created_at)
    VALUES (_uid, _reward_type, _chain, _gross, _onchain_before, 'REQUESTED', now());

    INSERT INTO cross_chain_bridge_history (uid, reward_type, chain, action, amount, balance_after, onchain_counter, before_value, signature, created_at)
    VALUES (_uid, _reward_type, _chain, 'WITHDRAW_REQUEST', _gross, _balance, NULL, _onchain_before, NULL, now())
    RETURNING id INTO _hid;

    RETURN json_build_object('before', _onchain_before::text, 'gross', _gross::text, 'id', _hid)::text;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$function$;

-- Withdraw sync (conservative-complete, mirrors sp_sync_user_claim_synced): when the
-- on-chain withdrawn counter shows the pending withdraw has landed, clear pending and
-- advance the watermark. Balance is NOT touched (already debited at request).
-- Now called by the backend (ap-deposit-bridge) after it observes its own tx receipt.
CREATE OR REPLACE FUNCTION public.fn_bridge_sync_withdraw(
    _uid integer,
    _reward_type character varying,
    _balance_type character varying,   -- unified-balance sentinel (server-owned constant, e.g. 'BP')
    _chain character varying,
    _onchain_withdrawn numeric
) RETURNS text
    LANGUAGE plpgsql
AS
$function$
DECLARE
    _synced    numeric          := 0;
    _pgross    numeric;
    _pbefore   numeric;
    _balance   double precision := 0;
    _confirmed boolean          := false;
    _hid       bigint;
BEGIN
    SELECT synced_withdraw INTO _synced
    FROM cross_chain_bridge_sync
    WHERE uid = _uid AND reward_type = _reward_type AND chain = _chain;
    IF _synced IS NULL THEN _synced := 0; END IF;

    IF _onchain_withdrawn <= _synced THEN
        RETURN json_build_object('confirmed', false, 'gross', '0', 'id', NULL)::text;   -- nothing new
    END IF;

    SELECT gross, before_value INTO _pgross, _pbefore
    FROM cross_chain_bridge_pending
    WHERE uid = _uid AND reward_type = _reward_type AND chain = _chain;

    SELECT "values" INTO _balance
    FROM user_block_reward
    WHERE uid = _uid AND reward_type = _reward_type AND type = _balance_type;

    IF _pgross IS NOT NULL AND _onchain_withdrawn >= _pbefore + _pgross THEN
        -- the pending withdraw landed on-chain: confirm & clear
        DELETE FROM cross_chain_bridge_pending
        WHERE uid = _uid AND reward_type = _reward_type;
        _confirmed := true;

        INSERT INTO cross_chain_bridge_history (uid, reward_type, chain, action, amount, balance_after, onchain_counter, before_value, signature, created_at)
        VALUES (_uid, _reward_type, _chain, 'WITHDRAW_CONFIRM', _pgross, _balance, _onchain_withdrawn, _pbefore, NULL, now())
        RETURNING id INTO _hid;
    ELSIF _pgross IS NULL THEN
        -- on-chain withdrawn advanced on this chain with NO pending here: record for the monitor (§H.3.4)
        INSERT INTO cross_chain_bridge_history (uid, reward_type, chain, action, amount, balance_after, onchain_counter, before_value, signature, created_at)
        VALUES (_uid, _reward_type, _chain, 'ANOMALY', _onchain_withdrawn - _synced, _balance, _onchain_withdrawn, NULL, NULL, now())
        RETURNING id INTO _hid;
    END IF;
    -- (pending exists but not yet fully landed → just advance the watermark below)

    INSERT INTO cross_chain_bridge_sync (uid, reward_type, chain, synced_deposit, synced_withdraw)
    VALUES (_uid, _reward_type, _chain, 0, _onchain_withdrawn)
    ON CONFLICT (uid, reward_type, chain)
        DO UPDATE SET synced_withdraw = EXCLUDED.synced_withdraw;

    RETURN json_build_object('confirmed', _confirmed, 'gross', COALESCE(_pgross, 0)::text, 'id', _hid)::text;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$function$;

-- Withdraw refund (Option 2): the atomic DB half of a cancel. Restores the debited balance
-- and clears the pending. SAFE ONLY when the caller (ap-deposit-bridge, sole submitter) has
-- already proven the tx is definitively dead: nonce consumed AND on-chain withdrawn == before
-- (§4.5). This function does NOT re-check the chain — it trusts the caller's proof, but it does
-- enforce that the pending is in FAILED state and that _expected_gross matches (defensive).
CREATE OR REPLACE FUNCTION public.fn_bridge_refund(
    _uid integer,
    _reward_type character varying,
    _balance_type character varying,   -- unified-balance sentinel (server-owned constant, e.g. 'BP')
    _chain character varying,
    _expected_gross numeric
) RETURNS text
    LANGUAGE plpgsql
AS
$function$
DECLARE
    _pgross  numeric;
    _pstatus character varying;
    _restore double precision := 0;
    _balance double precision := 0;
    _hid     bigint;
BEGIN
    -- lock the balance row (serializes against deposit-sync / request on the same uid)
    SELECT "values" INTO _balance
    FROM user_block_reward
    WHERE uid = _uid AND reward_type = _reward_type AND type = _balance_type
    FOR UPDATE;

    IF _balance IS NULL THEN
        RAISE EXCEPTION 'no bridge balance for uid=%, %', _uid, _reward_type;
    END IF;

    SELECT gross, status INTO _pgross, _pstatus
    FROM cross_chain_bridge_pending
    WHERE uid = _uid AND reward_type = _reward_type;

    IF _pgross IS NULL THEN
        RAISE EXCEPTION 'no pending to refund for uid=%, %', _uid, _reward_type;
    END IF;

    IF _pstatus <> 'FAILED' THEN
        RAISE EXCEPTION 'refund requires pending in FAILED state, got % (uid=%, %)', _pstatus, _uid, _reward_type;
    END IF;

    IF _pgross <> _expected_gross THEN
        RAISE EXCEPTION 'refund gross mismatch: pending=%, expected=% (uid=%, %)', _pgross, _expected_gross, _uid, _reward_type;
    END IF;

    _restore := (_expected_gross / 1000000000000000000::numeric)::double precision;

    UPDATE user_block_reward
    SET "values"    = "values" + _restore,
        modify_date = NOW() AT TIME ZONE 'utc'
    WHERE uid = _uid AND reward_type = _reward_type AND type = _balance_type
    RETURNING "values" INTO _balance;

    DELETE FROM cross_chain_bridge_pending
    WHERE uid = _uid AND reward_type = _reward_type;

    INSERT INTO cross_chain_bridge_history (uid, reward_type, chain, action, amount, balance_after, onchain_counter, before_value, signature, created_at)
    VALUES (_uid, _reward_type, _chain, 'WITHDRAW_REFUND', _expected_gross, _balance, NULL, NULL, NULL, now())
    RETURNING id INTO _hid;

    RETURN json_build_object('refunded', true, 'balance', _balance, 'id', _hid)::text;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$function$;

-- Status helper: claim-once transition to SUBMITTED. The backend calls this right before (or
-- after) broadcasting; only the caller that wins the lease proceeds to submit. Guards against a
-- second submitter racing (double-broadcast) and against a crashed submitter that never cleared
-- its lease — the lease expires after _lease_seconds, letting the reconciler reclaim it.
-- Touches no balance. Returns {claimed:boolean}.
CREATE OR REPLACE FUNCTION public.fn_bridge_mark_submitted(
    _uid integer,
    _reward_type character varying,
    _chain character varying,
    _tx_hash character varying,
    _nonce bigint,
    _lease_seconds integer DEFAULT 120
) RETURNS text
    LANGUAGE plpgsql
AS
$function$
DECLARE
    _rows integer := 0;
BEGIN
    UPDATE cross_chain_bridge_pending
    SET status        = 'SUBMITTED',
        tx_hash       = _tx_hash,
        nonce         = _nonce,
        submitting_at = now(),
        attempts      = attempts + 1,
        updated_at    = now()
    WHERE uid = _uid AND reward_type = _reward_type AND chain = _chain
      AND (submitting_at IS NULL OR submitting_at < now() - (_lease_seconds * interval '1 second'));

    GET DIAGNOSTICS _rows = ROW_COUNT;
    RETURN json_build_object('claimed', _rows > 0)::text;
END;
$function$;

-- Status helper: mark the pending FAILED and release the lease so the reconciler can retry or
-- refund it. Touches no balance. Returns {failed:boolean}.
CREATE OR REPLACE FUNCTION public.fn_bridge_mark_failed(
    _uid integer,
    _reward_type character varying,
    _chain character varying,
    _error text
) RETURNS text
    LANGUAGE plpgsql
AS
$function$
DECLARE
    _rows integer := 0;
BEGIN
    UPDATE cross_chain_bridge_pending
    SET status        = 'FAILED',
        submitting_at = NULL,
        last_error    = _error,
        updated_at    = now()
    WHERE uid = _uid AND reward_type = _reward_type AND chain = _chain;

    GET DIAGNOSTICS _rows = ROW_COUNT;
    RETURN json_build_object('failed', _rows > 0)::text;
END;
$function$;

COMMIT;
