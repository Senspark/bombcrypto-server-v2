-- Migration: Native (BNB / POL) deposit, withdraw and in-game spend
-- Date: 2026-07-24 (revised 2026-07-27 to support in-game spending)
-- Applies to: bombcrypto database (FRESH installs — a database that has NOT run any earlier version
--             of this file). The local dev DB, which ran the original 2026-07-24 version, uses the
--             one-time server/db/PATCH_native_deposit_sinks.sql instead.
--
-- Two layers, different jobs:
--
--   * user_block_reward (BNB_DEPOSITED on BSC / POL_DEPOSITED on POLYGON) holds the SPENDABLE
--     balance as a double, exactly like every other token, so the game spends it through the
--     ordinary fn_sub_user_reward with no native-specific machinery. Because spending is an
--     independent writer, `values` is credited by DELTA on sync and must never be overwritten.
--     total_values / claim_pending / claim_synced have no writer outside this file and are set
--     straight from their wei twins.
--
--   * user_native_deposited mirrors the on-chain counters in exact wei. It does not hold the
--     spendable balance; its job is to cap the withdraw signature, since the contract enforces
--     allowed_cumulative <= deposited[user].
--
-- Withdraw request converts `values` to wei once (numeric cast BEFORE the multiply, truncating down)
-- and clamps the result to deposited_wei - withdrawn_wei. Without the clamp an accumulated float
-- overshoot produces a signature the contract rejects, stranding the user. Every clamp is logged and
-- the excess is removed, not returned: value above deposited - withdrawn has no on-chain backing.
--
-- Nothing may credit BNB_DEPOSITED / POL_DEPOSITED `values` except deposit sync below. A promo,
-- grant or airdrop credit has no on-chain backing and is silently removed by the clamp at withdraw.
--
-- The on-chain RPC read happens BEFORE this transaction opens; the merge uses GREATEST so the
-- after-the-fact merge is safe and the DB lock is never held across an RPC call.

BEGIN;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

CREATE TABLE public.user_native_deposited (
    uid           integer       NOT NULL,
    network       character varying(20) NOT NULL,          -- BSC | POLYGON
    deposited_wei numeric(78,0) NOT NULL DEFAULT 0,        -- mirror of on-chain deposited[user]
    withdrawn_wei numeric(78,0) NOT NULL DEFAULT 0,        -- mirror of on-chain withdrawn[user]  (-> claim_synced)
    pending_wei   numeric(78,0) NOT NULL DEFAULT 0,        -- locked, awaiting withdraw           (-> claim_pending)
    -- When the pending's signature was last issued. Written ONLY by fn_native_request_withdraw (both
    -- the commit and the re-sign branch, since re-signing stamps a fresh deadline) — deliberately not
    -- by fn_native_sync, which touches modify_date on every tick and would keep refreshing its own
    -- reconcile window. This is what bounds the reconciler: past the signature's deadline the contract
    -- rejects the tx, so the pending can no longer land and polling the row discovers nothing until the
    -- user acts again (a fresh request, or login — both sync on their own).
    pending_requested_at timestamptz,
    synced_block  bigint,
    modify_date   timestamptz   NOT NULL DEFAULT now(),
    PRIMARY KEY (uid, network)
);

-- The background reconciler scans only the in-flight set, and only inside the signature window, so the
-- index leads with the column it ranges on.
CREATE INDEX idx_user_native_deposited_pending
    ON public.user_native_deposited (pending_requested_at)
    WHERE pending_wei > 0;

-- Native prices are not configured per item. Every native price is the item's existing BCOIN price
-- converted at a per-network rate, so there is one number to maintain per chain instead of a parallel
-- price list for three sinks. Its own table rather than a game_config key so the lookup is by
-- _network — a value already in scope everywhere the rate is needed — instead of a hardcoded key
-- string, and so the rate is a typed column instead of a varchar needing a cast.
CREATE TABLE public.config_native_rate (
    network          character varying(20) NOT NULL,       -- BSC | POLYGON
    native_per_bcoin double precision NOT NULL,            -- native coin charged per 1 BCOIN of list price
    modify_date      timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (network)
);

INSERT INTO public.config_native_rate (network, native_per_bcoin)
VALUES ('BSC', 0.000027372),                 -- 1 BCOIN = 0.000027372 BNB
       ('POLYGON', 0.214286)                 -- 1 BCOIN = 0.214286 POL
ON CONFLICT (network) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Functions
-- ---------------------------------------------------------------------------

-- network -> user_block_reward.reward_type. NULL for an unsupported network (callers raise).
CREATE OR REPLACE FUNCTION public.fn_native_reward_type(_network character varying)
    RETURNS character varying
    LANGUAGE sql IMMUTABLE
AS $$
    SELECT CASE _network
               WHEN 'BSC' THEN 'BNB_DEPOSITED'
               WHEN 'POLYGON' THEN 'POL_DEPOSITED'
           END;
$$;

-- BCOIN list price -> native price, for both the charge and the displayed figure. A function rather
-- than an inline multiply so a missing rate raises instead of yielding NULL, which would silently
-- charge nothing.
CREATE OR REPLACE FUNCTION public.fn_native_price(
    _network character varying,
    _bcoin_price double precision
) RETURNS double precision
    LANGUAGE plpgsql STABLE
AS $function$
DECLARE
    _rate double precision;
BEGIN
    IF _bcoin_price IS NULL OR _bcoin_price <= 0 THEN
        RAISE EXCEPTION 'native price: invalid bcoin price %', _bcoin_price;
    END IF;

    SELECT native_per_bcoin INTO _rate FROM config_native_rate WHERE network = _network;
    IF _rate IS NULL OR _rate <= 0 THEN
        RAISE EXCEPTION 'native price: no usable conversion rate for network %', _network;
    END IF;

    RETURN _bcoin_price * _rate;
END;
$function$;

-- Idempotent sync: merge the on-chain counters into the wei row (GREATEST, monotonic), settle any
-- landed pending, credit the deposit delta into `values`, and refresh the derived doubles. Called on
-- login, by the background reconciler, and by the client sync hint. Runs inside the caller's txn.
--
-- Idempotency comes from the counters being monotonic: syncing the same on-chain values twice gives
-- a second delta of 0, so a duplicate deposit event cannot double-credit.
--
-- Because a withdraw is one-at-a-time, the settle delta is either 0 or exactly pending_wei — there is
-- no partial settle.
CREATE OR REPLACE FUNCTION public.fn_native_sync(
    _uid integer,
    _network character varying,
    _onchain_deposited numeric,
    _onchain_withdrawn numeric,
    _synced_block bigint
) RETURNS text
    LANGUAGE plpgsql
AS $function$
DECLARE
    _reward_type   character varying := public.fn_native_reward_type(_network);
    _dep_old       numeric;
    _wd_old        numeric;
    _pend_old      numeric;
    _dep_new       numeric;
    _wd_new        numeric;
    _pend_new      numeric;
    _settle_delta  numeric;
    _credit_wei    numeric;
    _credit_dbl    double precision;
    _val_old       double precision;
    _val_new       double precision;
BEGIN
    IF _reward_type IS NULL THEN
        RAISE EXCEPTION 'native sync: unsupported network %', _network;
    END IF;

    -- ensure a lockable row exists, then lock it for the read-modify-write
    INSERT INTO user_native_deposited (uid, network)
    VALUES (_uid, _network)
    ON CONFLICT (uid, network) DO NOTHING;

    SELECT deposited_wei, withdrawn_wei, pending_wei
    INTO _dep_old, _wd_old, _pend_old
    FROM user_native_deposited
    WHERE uid = _uid AND network = _network
    FOR UPDATE;

    -- monotonic merge; the on-chain counters only ever grow
    _dep_new      := GREATEST(_dep_old, _onchain_deposited);
    _wd_new       := GREATEST(_wd_old, _onchain_withdrawn);
    _settle_delta := _wd_new - _wd_old;                 -- 0, or exactly the pending that just landed
    _pend_new     := GREATEST(0, _pend_old - _settle_delta);
    _credit_wei   := _dep_new - _dep_old;
    _credit_dbl   := (_credit_wei / 1e18)::double precision;

    UPDATE user_native_deposited
    SET deposited_wei = _dep_new,
        withdrawn_wei = _wd_new,
        pending_wei   = _pend_new,
        synced_block  = COALESCE(_synced_block, synced_block),
        modify_date   = now()
    WHERE uid = _uid AND network = _network;

    SELECT "values" INTO _val_old
    FROM user_block_reward
    WHERE uid = _uid AND type = _network AND reward_type = _reward_type;
    _val_old := COALESCE(_val_old, 0);
    _val_new := _val_old + _credit_dbl;

    -- `values` is credited by delta because in-game spending writes it too; the other three columns
    -- have a single writer and are refreshed from their wei twins.
    INSERT INTO user_block_reward
        (uid, reward_type, type, "values", total_values, claim_pending, claim_synced,
         modify_date, last_time_claim_success)
    VALUES
        (_uid, _reward_type, _network, _credit_dbl,
         (_dep_new / 1e18)::double precision,
         (_pend_new / 1e18)::double precision,
         (_wd_new / 1e18)::double precision,
         NOW() AT TIME ZONE 'utc', NOW() AT TIME ZONE 'utc')
    ON CONFLICT (uid, type, reward_type)
        DO UPDATE SET "values"       = user_block_reward."values" + EXCLUDED."values",
                      total_values   = EXCLUDED.total_values,
                      claim_pending  = EXCLUDED.claim_pending,
                      claim_synced   = EXCLUDED.claim_synced,
                      modify_date    = NOW() AT TIME ZONE 'utc';

    -- Audit rows: this path writes `values` directly instead of through fn_add/fn_sub, so it owns its
    -- own log rows.
    IF _credit_wei > 0 THEN
        INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new, reason)
        VALUES (_uid, _reward_type, _network, _val_old, _credit_dbl, _val_new,
                left('NATIVE_DEPOSIT wei=' || _credit_wei::text, 100));
    END IF;
    IF _settle_delta > 0 THEN
        -- pending -> withdrawn; `values` is unchanged, so this is a 0-change marker carrying the wei.
        INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new, reason)
        VALUES (_uid, _reward_type, _network, _val_new, 0, _val_new,
                left('NATIVE_WITHDRAW_SETTLE wei=' || _settle_delta::text || ' withdrawn=' || _wd_new::text, 100));
    END IF;

    RETURN json_build_object(
        'deposited_wei', _dep_new::text,
        'withdrawn_wei', _wd_new::text,
        'pending_wei', _pend_new::text,
        'spendable_wei', GREATEST(0, trunc(_val_new::numeric * 1e18))::text
    )::text;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$function$;

-- Withdraw request. Merges + settles first, then:
--   * pending_wei > 0  -> RE-SIGN the same allowed_cumulative = withdrawn_wei + pending_wei (no move).
--   * pending_wei == 0 -> commit the whole spendable balance (values -> claim_pending), converted to
--                         wei and clamped to deposited_wei - withdrawn_wei, then sign
--                         allowed_cumulative = withdrawn_wei + pending_wei.
-- Returns { allowed_cumulative_wei, pending_wei, reused }.
CREATE OR REPLACE FUNCTION public.fn_native_request_withdraw(
    _uid integer,
    _network character varying,
    _onchain_deposited numeric,
    _onchain_withdrawn numeric,
    _synced_block bigint
) RETURNS text
    LANGUAGE plpgsql
AS $function$
DECLARE
    _reward_type character varying := public.fn_native_reward_type(_network);
    _dep         numeric;
    _wd          numeric;
    _pend        numeric;
    _val_dbl     double precision;
    _a_wei       numeric;
    _a_raw       numeric;
    _cap         numeric;
    _allowed     numeric;
    _reused      boolean := false;
BEGIN
    IF _reward_type IS NULL THEN
        RAISE EXCEPTION 'native withdraw: unsupported network %', _network;
    END IF;

    -- merge on-chain counters + settle any landed pending + credit any new deposit
    PERFORM public.fn_native_sync(_uid, _network, _onchain_deposited, _onchain_withdrawn, _synced_block);

    SELECT deposited_wei, withdrawn_wei, pending_wei
    INTO _dep, _wd, _pend
    FROM user_native_deposited
    WHERE uid = _uid AND network = _network
    FOR UPDATE;

    IF _pend > 0 THEN
        -- re-sign the existing pending; nothing moves, one withdraw at a time
        _reused  := true;
        _allowed := _wd + _pend;
        -- a re-sign issues a fresh deadline, so the reconcile window restarts with it
        UPDATE user_native_deposited
        SET pending_requested_at = now()
        WHERE uid = _uid AND network = _network;
    ELSE
        SELECT "values" INTO _val_dbl
        FROM user_block_reward
        WHERE uid = _uid AND type = _network AND reward_type = _reward_type
        FOR UPDATE;

        IF COALESCE(_val_dbl, 0) <= 0 THEN
            RAISE EXCEPTION 'native withdraw: nothing to withdraw (uid=%, %)', _uid, _network;
        END IF;

        -- cast to numeric BEFORE multiplying: the product is ~1e18, past float64's exact-integer
        -- range. trunc, not round — never round up in the user's favour.
        _a_raw := trunc(_val_dbl::numeric * 1e18);
        _a_wei := _a_raw;
        _cap   := _dep - _wd;

        IF _a_wei > _cap THEN
            -- `values` claims more than exists on-chain. The excess is removed, not carried: it has
            -- no backing, and signing it would make the contract's allowed <= deposited check revert.
            INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new, reason)
            VALUES (_uid, _reward_type, _network, _val_dbl,
                    -((_a_raw - _cap) / 1e18)::double precision,
                    (_cap / 1e18)::double precision,
                    -- reason is varchar(100) and this is the longest string written here: at 1 BNB
                    -- claimed against a 0.2 BNB cap the old four-field form reached 108 characters and
                    -- aborted the whole withdraw. values_old/changed/new already carry these amounts in
                    -- token units, so only the exact wei integers are worth repeating.
                    left('NATIVE_WITHDRAW_CLAMP wei=' || _a_raw::text || ' cap=' || _cap::text, 100));
            _a_wei := _cap;
        END IF;

        IF _a_wei <= 0 THEN
            RAISE EXCEPTION 'native withdraw: nothing to withdraw (uid=%, %)', _uid, _network;
        END IF;

        _pend := _a_wei;
        UPDATE user_native_deposited
        SET pending_wei = _pend, pending_requested_at = now(), modify_date = now()
        WHERE uid = _uid AND network = _network;

        UPDATE user_block_reward
        SET "values"      = 0,
            claim_pending = (_pend / 1e18)::double precision,
            modify_date   = NOW() AT TIME ZONE 'utc'
        WHERE uid = _uid AND type = _network AND reward_type = _reward_type;

        INSERT INTO logs.user_block_reward (uid, reward_type, network, values_old, values_changed, values_new, reason)
        VALUES (_uid, _reward_type, _network,
                (_pend / 1e18)::double precision, -(_pend / 1e18)::double precision, 0,
                left('NATIVE_WITHDRAW_REQUEST wei=' || _pend::text, 100));

        _allowed := _wd + _pend;
    END IF;

    RETURN json_build_object(
        'allowed_cumulative_wei', _allowed::text,
        'pending_wei', _pend::text,
        'reused', _reused
    )::text;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%', SQLSTATE, SQLERRM;
END;
$function$;

-- ---------------------------------------------------------------------------
-- Sinks — auto mine, rock pack, gem shop
-- ---------------------------------------------------------------------------
--
-- A sink charges the native balance through the ordinary fn_sub_user_reward, like any other token.
-- Two things are native-specific and both are mandatory:
--
--   * the price is the item's existing BCOIN price run through fn_native_price, and
--   * the charge uses the SINGLE-TYPE overload of fn_sub_user_reward.
--
-- The (reward, deposited) overload sums the two reward types before the sufficiency check. Native has
-- only one balance, so both types name the same row and the check reads 2 * values — a user holding
-- 1 BNB could spend 2 and drive the balance negative.

-- Replaces user_auto_mine_buy_logs. Both sp_user_buy_auto_mine overloads write here instead, so the old
-- table has no writer left; it is kept, not dropped, and nothing in the repo reads either one. What it
-- fixes: no column said which currency was charged (`type` is the
-- network, so a native price and a BCOIN price on 'BSC' were indistinguishable), no package_name to
-- join back to config, no record of the period actually granted (user_auto_mine is overwritten on
-- renewal), and a UNIQUE (uid, "time") over NOW() — transaction start time — that can only reject a
-- legitimate concurrent purchase. Modelled on logs.logs_user_buy_rock_pack.
CREATE TABLE logs.logs_user_buy_auto_mine (
    id           bigserial PRIMARY KEY,
    uid          integer NOT NULL,
    time_stamp   timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    package_name character varying(30) NOT NULL,
    num_day      integer NOT NULL,
    price        double precision NOT NULL,          -- in whatever token_name names
    token_name   character varying(30) NOT NULL,     -- the reward type actually charged
    network      character varying(20) NOT NULL,
    start_time   timestamp with time zone NOT NULL,
    end_time     timestamp with time zone NOT NULL
);

CREATE INDEX idx_logs_user_buy_auto_mine_uid_time
    ON logs.logs_user_buy_auto_mine (uid, time_stamp DESC);

-- Auto mine, 4-argument form — the airdrop chains (TON/SOL/RON/BAS/VIC), which pay in their own
-- deposited coin at the flat min_price. No native branch: nothing native routes here. It is rewritten
-- only so auto-mine history lives in one table instead of two.
CREATE OR REPLACE PROCEDURE public.sp_user_buy_auto_mine(IN _uid integer, IN _reward_type character varying,
                                                         IN _data_type character varying, IN _package_json json)
    LANGUAGE plpgsql
AS
$procedure$
DECLARE
    _time_stamp      timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _last_start_time timestamp DEFAULT NULL;
    _last_end_time   timestamp DEFAULT NULL;
    _start_time      timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _end_time        timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _package_name    varchar;
    _price           float;
    _num_days        int;
BEGIN

    SELECT start_time,
           end_time
    INTO _last_start_time,
        _last_end_time
    FROM user_auto_mine
    WHERE uid = _uid
      AND type = _data_type;

    IF _last_end_time IS NOT NULL
    THEN
        IF EXTRACT(DAY FROM
                   (_time_stamp::timestamp - _last_end_time::timestamp)) < -2
        THEN
            RAISE EXCEPTION 'You can only renew your package on the last 2 days';
        END IF;
    END IF;

    SELECT _package_json ->> 'package',
           _package_json ->> 'min_price',
           _package_json ->> 'num_days'
    INTO _package_name,
        _price,
        _num_days;

--     tính thời gian auto mine
    IF _last_end_time IS NOT NULL AND _start_time < _last_end_time THEN
        _start_time = _last_start_time;
        _end_time = _last_end_time + (_num_days || ' DAY')::INTERVAL;
    ELSE
        _end_time = _end_time + (_num_days || ' DAY')::INTERVAL;
    END IF;

--     trừ reward
    PERFORM fn_sub_user_reward(_uid,
                               _data_type,
                               _price,
                               _reward_type,
                               'Buy auto mine');

--     update lại thời gian hết hạn auto mine
    INSERT INTO user_auto_mine(uid, start_time, end_time, type)
    VALUES (_uid, _start_time, _end_time, _data_type)
    ON CONFLICT (uid,type) DO UPDATE SET start_time  = excluded.start_time,
                                         end_time    = excluded.end_time,
                                         modify_time = _time_stamp;

--     ghi log
    INSERT INTO logs.logs_user_buy_auto_mine (uid, package_name, num_day, price, token_name, network,
                                              start_time, end_time)
    VALUES (_uid, _package_name, _num_days, _price, _reward_type, _data_type,
            _start_time AT TIME ZONE 'utc', _end_time AT TIME ZONE 'utc');

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;

END;
$procedure$;

-- Auto mine, 5-argument form. Native must route here rather than to the 4-argument form, which prices
-- at the flat min_price: fn_calculate_package_auto_price charges a heavy miner more than min_price, and
-- paying in BNB must not be a discount on the same chain.
CREATE OR REPLACE PROCEDURE public.sp_user_buy_auto_mine(IN _uid integer,
                                                         IN _first_reward_type character varying,
                                                         IN _second_reward_type character varying,
                                                         IN _data_type character varying,
                                                         IN _package_json json)
    LANGUAGE plpgsql
AS
$procedure$
DECLARE
    _time_stamp      timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _last_start_time timestamp DEFAULT NULL;
    _last_end_time   timestamp DEFAULT NULL;
    _start_time      timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _end_time        timestamp DEFAULT NOW() AT TIME ZONE 'utc';
    _package_name    varchar;
    _price           float;
    _charged_price   float;
    _num_days        int;
BEGIN

    SELECT start_time,
           end_time
    INTO _last_start_time,
        _last_end_time
    FROM user_auto_mine
    WHERE uid = _uid
      AND type = _data_type;

    IF _last_end_time IS NOT NULL
    THEN
        IF EXTRACT(DAY FROM
                   (_time_stamp::timestamp - _last_end_time::timestamp)) < -2
        THEN
            RAISE EXCEPTION 'You can only renew your package on the last 2 days';
        END IF;
    ELSE
        IF EXTRACT(DAY FROM
                   (_time_stamp::timestamp - (SELECT datecreate FROM "user" WHERE id_user = _uid)::timestamp)) < -2
        THEN
            RAISE EXCEPTION 'You can only renew your package on the last 2 days';
        END IF;
    END IF;

    SELECT package,
           price,
           num_days
    INTO _package_name,
        _price,
        _num_days
    FROM fn_calculate_package_auto_price(_uid, _package_json);

    IF (SELECT COUNT(*) FROM user_auto_mine WHERE uid = _uid AND type = _data_type) != 0
    THEN
        SELECT CASE WHEN _start_time < end_time THEN start_time ELSE _start_time END,
               CASE
                   WHEN _end_time < end_time
                       THEN end_time + (_num_days || ' DAY')::INTERVAL
                   ELSE _end_time + (_num_days || ' DAY')::INTERVAL
                   END
        INTO _start_time,
            _end_time
        FROM user_auto_mine
        WHERE uid = _uid
          AND type = _data_type;

    ELSE
        _end_time = _end_time + (_num_days || ' DAY')::INTERVAL;
    END IF;

--     trừ reward
    -- fn_native_reward_type is NULL on a non-native network, so the comparison is NULL and falls
    -- through to the pair overload, byte-identical to what every caller got before.
    IF _first_reward_type = public.fn_native_reward_type(_data_type) THEN
        _charged_price := public.fn_native_price(_data_type, _price);
        PERFORM fn_sub_user_reward(_uid,
                                   _data_type,
                                   _charged_price,
                                   _first_reward_type,
                                   'Buy auto mine');
    ELSE
        _charged_price := _price;
        PERFORM fn_sub_user_reward(_uid,
                                   _data_type,
                                   _price,
                                   _first_reward_type,
                                   _second_reward_type,
                                   'Buy auto mine');
    END IF;

--     update lại thời gian hết hạn auto mine
    INSERT INTO user_auto_mine(uid, start_time, end_time, type)
    VALUES (_uid, _start_time, _end_time, _data_type)
    ON CONFLICT (uid,type) DO UPDATE SET start_time  = excluded.start_time,
                                         end_time    = excluded.end_time,
                                         modify_time = _time_stamp;

--     ghi log
    INSERT INTO logs.logs_user_buy_auto_mine (uid, package_name, num_day, price, token_name, network,
                                              start_time, end_time)
    VALUES (_uid, _package_name, _num_days, _charged_price, _first_reward_type, _data_type,
            _start_time AT TIME ZONE 'utc', _end_time AT TIME ZONE 'utc');

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;

END;
$procedure$;

-- Rock pack. Same native branch, same reason. The 4-argument divert that shields the auto mine SP does
-- not exist here, so without this branch the double-count is reachable as soon as the manager accepts a
-- native reward type.
CREATE OR REPLACE PROCEDURE public.sp_user_buy_rock_pack(IN _uid integer, IN _pack_name character varying,
                                                         IN _network character varying,
                                                         IN _reward_type character varying,
                                                         IN _second_reward_type character varying)
    LANGUAGE plpgsql
AS
$procedure$
DECLARE
    _price       NUMERIC;
    _charged     NUMERIC;
    _rock_amount INT;
BEGIN
    -- Load giá
    IF _second_reward_type = 'SENSPARK' THEN
        SELECT sen_price, rock_amount INTO _price, _rock_amount FROM config_rock_pack WHERE pack_name = _pack_name;
    ELSE
        SELECT bcoin_price, rock_amount INTO _price, _rock_amount FROM config_rock_pack WHERE pack_name = _pack_name;
    END IF;

    -- Trừ tiền
    IF _reward_type = public.fn_native_reward_type(_network) THEN
        _charged := public.fn_native_price(_network, _price::double precision)::numeric;
        PERFORM fn_sub_user_reward(_uid, _network, _charged, _reward_type, 'Buy rock pack');
    ELSE
        _charged := _price;
        PERFORM fn_sub_user_reward(_uid, _network, _price, _reward_type, _second_reward_type, 'Buy rock pack');
    END IF;

    -- Cộng đá
    PERFORM fn_add_user_reward(_uid, 'TR', _rock_amount, 'ROCK', 'Buy rock pack');

    -- Lưu lại lịch sử. price là số đã trừ thật, token_name cho biết đơn vị.
    INSERT INTO logs.logs_user_buy_rock_pack (uid, time_stamp, package_name, rock_amount, price, token_name,
                                              network)
    VALUES (_uid, CURRENT_TIMESTAMP, _pack_name, _rock_amount, _charged, _reward_type, _network);

END;
$procedure$;

-- Gem packs had no price column at all: the price lives on the app store against product_id. A native
-- purchase does not pay the platform's cut, so converting the store's USD price would overcharge —
-- each pack is priced at 1 BCOIN per base gem instead, which lands uniformly 25% under the store.
-- Nullable: PACK rows stay store-only.
ALTER TABLE public.config_iap_shop
    ADD COLUMN bcoin_price double precision;

UPDATE public.config_iap_shop c
SET bcoin_price = v.price
FROM (VALUES ('tiny_pack', 100.0),
             ('regular_pack', 250.0),
             ('pro_pack', 500.0),
             ('deluxe_pack', 1250.0),
             ('super_deluxe_pack', 2000.0),
             ('huge_pack', 4000.0),
             ('giant_pack', 8000.0)) AS v(product_id, price)
WHERE c.product_id = v.product_id
  AND c.type = 'GEM';

COMMIT;
