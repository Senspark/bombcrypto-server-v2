-- =============================================================================
-- House Rental P2P (players renting houses to each other, via the market site)
-- =============================================================================
--
-- What it does:
--   Creates the P2P house rental infrastructure: the owner lists a house with a
--   price per day (BCOIN or SEN), another player rents it for N days, and the
--   charge is DAILY and ALWAYS UPFRONT, debited from the balance deposited in
--   the game (user_block_reward) and credited to the owner in the same
--   transaction.
--
-- Why:
--   The existing "rent house" (config_package_rent_house_v2 +
--   user_house.end_time_rent) is a SYSTEM rental for airdrop users: prepaid and
--   with no owner receiving anything. This migration leaves it untouched and
--   creates its own tables for the P2P flow.
--
-- Objects created:
--   - public.house_rental_listing  : the owner's offer (1 live per house)
--   - public.house_rental          : rental contract (active + history)
--   - public.house_rental_payment  : statement of the daily payments
--   - public.sp_rent_house_p2p     : closes the rental and charges day 1
--   - public.sp_charge_house_rent_day : charges the next day when the cycle turns
--
-- Idempotent: uses IF NOT EXISTS / CREATE OR REPLACE.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Tables
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.house_rental_listing (
    id            bigserial PRIMARY KEY,
    house_id      integer          NOT NULL,
    type          character varying(255) NOT NULL,          -- network: BSC / POLYGON (same as user_house.type)
    owner_uid     integer          NOT NULL,
    price_per_day double precision NOT NULL CHECK (price_per_day > 0),
    pay_token     character varying(20) NOT NULL CHECK (pay_token IN ('BCOIN', 'SEN')),
    status        character varying(16) NOT NULL CHECK (status IN ('AVAILABLE', 'RENTED', 'CANCELLED')),
    created_at    timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

-- At most 1 live listing (available or rented) per house/network
CREATE UNIQUE INDEX IF NOT EXISTS ux_house_rental_listing_live
    ON public.house_rental_listing (house_id, type)
    WHERE status IN ('AVAILABLE', 'RENTED');

-- Public browsing: filter by network/token + sort by price
CREATE INDEX IF NOT EXISTS ix_house_rental_listing_browse
    ON public.house_rental_listing (type, status, pay_token, price_per_day)
    WHERE status = 'AVAILABLE';

CREATE INDEX IF NOT EXISTS ix_house_rental_listing_owner
    ON public.house_rental_listing (owner_uid, type);


CREATE TABLE IF NOT EXISTS public.house_rental (
    id                  bigserial PRIMARY KEY,
    listing_id          bigint           NOT NULL REFERENCES public.house_rental_listing (id),
    house_id            integer          NOT NULL,
    type                character varying(255) NOT NULL,
    owner_uid           integer          NOT NULL,          -- owner at contract time
    renter_uid          integer          NOT NULL,
    price_per_day       double precision NOT NULL,          -- frozen in the contract
    pay_token           character varying(20) NOT NULL,
    total_days          integer          NOT NULL CHECK (total_days > 0),
    days_paid           integer          NOT NULL DEFAULT 0,
    started_at          timestamp with time zone NOT NULL,
    current_period_end  timestamp with time zone NOT NULL,  -- end of the day already paid = next turn
    interrupted_by_sale boolean          NOT NULL DEFAULT false,
    status              character varying(20) NOT NULL CHECK (status IN (
                            'ACTIVE', 'ENDED_COMPLETED', 'ENDED_NO_FUNDS', 'ENDED_SOLD', 'ENDED_BY_RENTER')),
    ended_at            timestamp with time zone,
    -- Has the renter activated the rented house in the game? It lives here (and
    -- not in user_house.active) because the user_house row belongs to the OWNER.
    renter_active       boolean          NOT NULL DEFAULT false
);

-- For databases that already ran an earlier version of this migration
ALTER TABLE public.house_rental
    ADD COLUMN IF NOT EXISTS renter_active boolean NOT NULL DEFAULT false;

-- 1 active contract per house and 1 per renter (per network)
CREATE UNIQUE INDEX IF NOT EXISTS ux_house_rental_active_house
    ON public.house_rental (house_id, type)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS ux_house_rental_active_renter
    ON public.house_rental (renter_uid, type)
    WHERE status = 'ACTIVE';

-- Scan for the charge job
CREATE INDEX IF NOT EXISTS ix_house_rental_charge
    ON public.house_rental (current_period_end)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS ix_house_rental_owner ON public.house_rental (owner_uid, type);
CREATE INDEX IF NOT EXISTS ix_house_rental_renter ON public.house_rental (renter_uid, type);


CREATE TABLE IF NOT EXISTS public.house_rental_payment (
    id         bigserial PRIMARY KEY,
    rental_id  bigint           NOT NULL REFERENCES public.house_rental (id),
    house_id   integer          NOT NULL,
    type       character varying(255) NOT NULL,
    day_number integer          NOT NULL,                   -- 1..total_days
    amount     double precision NOT NULL,                   -- amount charged from the renter
    fee        double precision NOT NULL DEFAULT 0,          -- fee withheld by the platform
    owner_amount double precision NOT NULL,                 -- credited to the owner (amount - fee)
    pay_token  character varying(20) NOT NULL,
    owner_uid  integer          NOT NULL,
    renter_uid integer          NOT NULL,
    charged_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- DAILY = the day's charge; CANCEL_PENALTY = early cancellation penalty
    kind       character varying(20) NOT NULL DEFAULT 'DAILY'
);

-- For databases that already ran an earlier version of this migration
ALTER TABLE public.house_rental_payment
    ADD COLUMN IF NOT EXISTS kind character varying(20) NOT NULL DEFAULT 'DAILY';

-- One payment per day; the penalty is left out (it has no day)
CREATE UNIQUE INDEX IF NOT EXISTS ux_house_rental_payment_day
    ON public.house_rental_payment (rental_id, day_number)
    WHERE kind = 'DAILY';

CREATE INDEX IF NOT EXISTS ix_house_rental_payment_owner
    ON public.house_rental_payment (owner_uid, pay_token, type);


-- -----------------------------------------------------------------------------
-- 2. Helper: maps the rental token to the reward_type of the in-game balance
-- -----------------------------------------------------------------------------
-- BCOIN -> ('BCOIN', 'BCOIN_DEPOSITED') ; SEN -> ('SENSPARK', 'SENSPARK_DEPOSITED')
-- The debit uses the 6-arg variant of fn_sub_user_reward, which spends the
-- DEPOSITED balance first and the remainder from the mined one.

CREATE OR REPLACE FUNCTION public.fn_house_rental_reward_types(
    _pay_token character varying,
    OUT reward_type character varying,
    OUT deposit_reward_type character varying
)
    LANGUAGE plpgsql
    IMMUTABLE
AS
$$
BEGIN
    IF _pay_token = 'BCOIN' THEN
        reward_type := 'BCOIN';
        deposit_reward_type := 'BCOIN_DEPOSITED';
    ELSIF _pay_token = 'SEN' THEN
        reward_type := 'SENSPARK';
        deposit_reward_type := 'SENSPARK_DEPOSITED';
    ELSE
        RAISE EXCEPTION '1021,Invalid pay token %', _pay_token;
    END IF;
END;
$$;


-- -----------------------------------------------------------------------------
-- 3. sp_rent_house_p2p: closes the rental and charges day 1 (upfront)
-- -----------------------------------------------------------------------------
-- Validates that the listing is available, that the owner is still correct in
-- the game and that renter != owner, debits day 1 from the renter, credits the
-- owner (minus the fee) and creates the contract.
-- Errors follow the project convention: '<code>,<message>'.
--   1019 -> not enough balance (raised by fn_sub_user_reward)
--   1022 -> listing not available
--   1023 -> owner changed (house sold) - the listing is cancelled
--   1024 -> renter is the owner himself
--   1025 -> number of days outside the allowed range

CREATE OR REPLACE PROCEDURE public.sp_rent_house_p2p(
    IN _listing_id bigint,
    IN _renter_uid integer,
    IN _num_days integer,
    IN _fee double precision,
    IN _max_days integer,
    INOUT _rental_id bigint DEFAULT NULL
)
    LANGUAGE plpgsql
AS
$$
DECLARE
    l_house_id      integer;
    l_type          character varying(255);
    l_owner_uid     integer;
    l_price_per_day double precision;
    l_pay_token     character varying(20);
    l_status        character varying(16);
    l_current_owner integer;
    l_reward_type   character varying;
    l_deposit_type  character varying;
    l_fee_amount    double precision;
    l_owner_amount  double precision;
    l_fee_uid       integer;
    l_period_end    timestamp with time zone;
BEGIN
    IF _num_days < 1 OR _num_days > _max_days THEN
        RAISE EXCEPTION '1025,Invalid rental days %', _num_days;
    END IF;

    IF _fee < 0 OR _fee >= 1 THEN
        RAISE EXCEPTION '1026,Invalid rental fee %', _fee;
    END IF;

    -- Lock the listing: stops two renters from closing the same house
    SELECT house_id, type, owner_uid, price_per_day, pay_token, status
    INTO l_house_id, l_type, l_owner_uid, l_price_per_day, l_pay_token, l_status
    FROM public.house_rental_listing
    WHERE id = _listing_id
    FOR UPDATE;

    IF NOT FOUND OR l_status <> 'AVAILABLE' THEN
        RAISE EXCEPTION '1022,Listing not available';
    END IF;

    IF l_owner_uid = _renter_uid THEN
        RAISE EXCEPTION '1024,Cannot rent your own house';
    END IF;

    -- Is the owner recorded in the game still the owner of the listing?
    -- (the on-chain check happens in the API before calling this procedure)
    SELECT uid INTO l_current_owner
    FROM public.user_house
    WHERE house_id = l_house_id AND type = l_type
    FOR UPDATE;

    IF NOT FOUND OR l_current_owner IS DISTINCT FROM l_owner_uid THEN
        UPDATE public.house_rental_listing
        SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
        WHERE id = _listing_id;

        RAISE EXCEPTION '1023,House owner changed';
    END IF;

    SELECT reward_type, deposit_reward_type
    INTO l_reward_type, l_deposit_type
    FROM public.fn_house_rental_reward_types(l_pay_token);

    -- Day 1 upfront: debit the renter (DEPOSITED first) and credit the owner
    PERFORM fn_sub_user_reward(_renter_uid, l_type, l_price_per_day, l_reward_type, l_deposit_type,
                               'House rental payment');

    l_fee_amount := l_price_per_day * _fee;
    l_owner_amount := l_price_per_day - l_fee_amount;

    PERFORM fn_add_user_reward(l_owner_uid, l_type, l_owner_amount, l_deposit_type, 'House rental income');

    -- Game fee: when house_rental_fee_uid points to an account the amount is
    -- credited to it; otherwise it leaves circulation (token sink). Either way
    -- the amount is recorded in house_rental_payment.fee.
    IF l_fee_amount > 0 THEN
        SELECT NULLIF(value, '')::int INTO l_fee_uid
        FROM public.game_config WHERE key = 'house_rental_fee_uid';

        IF l_fee_uid IS NOT NULL AND l_fee_uid > 0 THEN
            PERFORM fn_add_user_reward(l_fee_uid, l_type, l_fee_amount, l_deposit_type, 'House rental fee');
        END IF;
    END IF;

    l_period_end := CURRENT_TIMESTAMP + INTERVAL '1 day';

    INSERT INTO public.house_rental (listing_id, house_id, type, owner_uid, renter_uid, price_per_day,
                                     pay_token, total_days, days_paid, started_at, current_period_end, status)
    VALUES (_listing_id, l_house_id, l_type, l_owner_uid, _renter_uid, l_price_per_day,
            l_pay_token, _num_days, 1, CURRENT_TIMESTAMP, l_period_end, 'ACTIVE')
    RETURNING id INTO _rental_id;

    INSERT INTO public.house_rental_payment (rental_id, house_id, type, day_number, amount, fee, owner_amount,
                                             pay_token, owner_uid, renter_uid)
    VALUES (_rental_id, l_house_id, l_type, 1, l_price_per_day, l_fee_amount, l_owner_amount,
            l_pay_token, l_owner_uid, _renter_uid);

    UPDATE public.house_rental_listing
    SET status = 'RENTED', updated_at = CURRENT_TIMESTAMP
    WHERE id = _listing_id;

    -- The owner loses the use of the house while it is rented. Done here so it
    -- also applies when he is offline; when he is online the stream listener in
    -- sfs-game additionally puts his heroes to sleep.
    UPDATE public.user_house
    SET active = 0
    WHERE house_id = l_house_id AND type = l_type;
END;
$$;


-- -----------------------------------------------------------------------------
-- 4. sp_charge_house_rent_day: charges the next day when the cycle turns
-- -----------------------------------------------------------------------------
-- Called by the job when current_period_end <= now. If the renter is out of
-- balance, fn_sub_user_reward raises '1019,Not enough ...' and the whole
-- transaction is rolled back - the caller then ends the contract as
-- ENDED_NO_FUNDS.
-- current_period_end advances from its previous value (not from now()), so the
-- renter does not lose time already paid for if the job runs late.

CREATE OR REPLACE PROCEDURE public.sp_charge_house_rent_day(
    IN _rental_id bigint,
    IN _fee double precision,
    INOUT _day_number integer DEFAULT NULL
)
    LANGUAGE plpgsql
AS
$$
DECLARE
    r_house_id      integer;
    r_type          character varying(255);
    r_owner_uid     integer;
    r_renter_uid    integer;
    r_price_per_day double precision;
    r_pay_token     character varying(20);
    r_total_days    integer;
    r_days_paid     integer;
    r_period_end    timestamp with time zone;
    r_status        character varying(20);
    l_reward_type   character varying;
    l_deposit_type  character varying;
    l_fee_amount    double precision;
    l_owner_amount  double precision;
    l_fee_uid       integer;
BEGIN
    SELECT house_id, type, owner_uid, renter_uid, price_per_day, pay_token,
           total_days, days_paid, current_period_end, status
    INTO r_house_id, r_type, r_owner_uid, r_renter_uid, r_price_per_day, r_pay_token,
        r_total_days, r_days_paid, r_period_end, r_status
    FROM public.house_rental
    WHERE id = _rental_id
    FOR UPDATE;

    IF NOT FOUND OR r_status <> 'ACTIVE' THEN
        RAISE EXCEPTION '1027,Rental not active';
    END IF;

    IF r_days_paid >= r_total_days THEN
        RAISE EXCEPTION '1028,Rental already fully paid';
    END IF;

    -- Only charge if the cycle is really due. Without this check two instances
    -- of the job (or a retry) could charge days ahead of time: the first one
    -- advances current_period_end and the second would charge the next day
    -- right away.
    IF r_period_end > CURRENT_TIMESTAMP THEN
        RAISE EXCEPTION '1029,Rental period not due yet';
    END IF;

    SELECT reward_type, deposit_reward_type
    INTO l_reward_type, l_deposit_type
    FROM public.fn_house_rental_reward_types(r_pay_token);

    PERFORM fn_sub_user_reward(r_renter_uid, r_type, r_price_per_day, l_reward_type, l_deposit_type,
                               'House rental payment');

    l_fee_amount := r_price_per_day * _fee;
    l_owner_amount := r_price_per_day - l_fee_amount;

    PERFORM fn_add_user_reward(r_owner_uid, r_type, l_owner_amount, l_deposit_type, 'House rental income');

    -- Game fee (see sp_rent_house_p2p)
    IF l_fee_amount > 0 THEN
        SELECT NULLIF(value, '')::int INTO l_fee_uid
        FROM public.game_config WHERE key = 'house_rental_fee_uid';

        IF l_fee_uid IS NOT NULL AND l_fee_uid > 0 THEN
            PERFORM fn_add_user_reward(l_fee_uid, r_type, l_fee_amount, l_deposit_type, 'House rental fee');
        END IF;
    END IF;

    _day_number := r_days_paid + 1;

    UPDATE public.house_rental
    SET days_paid = _day_number,
        current_period_end = r_period_end + INTERVAL '1 day'
    WHERE id = _rental_id;

    INSERT INTO public.house_rental_payment (rental_id, house_id, type, day_number, amount, fee, owner_amount,
                                             pay_token, owner_uid, renter_uid)
    VALUES (_rental_id, r_house_id, r_type, _day_number, r_price_per_day, l_fee_amount, l_owner_amount,
            r_pay_token, r_owner_uid, r_renter_uid);
END;
$$;


-- -----------------------------------------------------------------------------
-- 4b. sp_cancel_house_rental: the renter gives up before the end
-- -----------------------------------------------------------------------------
-- Ends the contract RIGHT AWAY (the current day, already paid, is not refunded)
-- and charges a penalty over the amount still owed:
--   _fee_owner (10%) -> credited to the owner
--   _fee_game  (5%)  -> game fee (house_rental_fee_uid, or leaves circulation)
-- Without balance for the penalty, fn_sub_user_reward raises
-- '1019,Not enough ...' and nothing changes - the renter keeps the rental.
--   1027 -> contract is not active
--   1043 -> the caller is not the renter

CREATE OR REPLACE PROCEDURE public.sp_cancel_house_rental(
    IN _rental_id bigint,
    IN _renter_uid integer,
    IN _fee_owner double precision,
    IN _fee_game double precision,
    INOUT _penalty double precision DEFAULT NULL
)
    LANGUAGE plpgsql
AS
$$
DECLARE
    r_house_id       integer;
    r_type           character varying(255);
    r_owner_uid      integer;
    r_renter_uid     integer;
    r_price_per_day  double precision;
    r_pay_token      character varying(20);
    r_total_days     integer;
    r_days_paid      integer;
    r_listing_id     bigint;
    r_status         character varying(20);
    l_reward_type    character varying;
    l_deposit_type   character varying;
    l_remaining_days integer;
    l_remaining_val  double precision;
    l_owner_amount   double precision;
    l_game_amount    double precision;
    l_fee_uid        integer;
BEGIN
    IF _fee_owner < 0 OR _fee_game < 0 OR (_fee_owner + _fee_game) >= 1 THEN
        RAISE EXCEPTION '1026,Invalid cancel fee';
    END IF;

    SELECT house_id, type, owner_uid, renter_uid, price_per_day, pay_token,
           total_days, days_paid, listing_id, status
    INTO r_house_id, r_type, r_owner_uid, r_renter_uid, r_price_per_day, r_pay_token,
        r_total_days, r_days_paid, r_listing_id, r_status
    FROM public.house_rental
    WHERE id = _rental_id
    FOR UPDATE;

    IF NOT FOUND OR r_status <> 'ACTIVE' THEN
        RAISE EXCEPTION '1027,Rental not active';
    END IF;

    IF r_renter_uid <> _renter_uid THEN
        RAISE EXCEPTION '1043,Not the renter of this rental';
    END IF;

    -- Penalty over what was still owed (days not charged yet)
    l_remaining_days := GREATEST(r_total_days - r_days_paid, 0);
    l_remaining_val := l_remaining_days * r_price_per_day;
    l_owner_amount := l_remaining_val * _fee_owner;
    l_game_amount := l_remaining_val * _fee_game;
    _penalty := l_owner_amount + l_game_amount;

    IF _penalty > 0 THEN
        SELECT reward_type, deposit_reward_type
        INTO l_reward_type, l_deposit_type
        FROM public.fn_house_rental_reward_types(r_pay_token);

        PERFORM fn_sub_user_reward(_renter_uid, r_type, _penalty, l_reward_type, l_deposit_type,
                                   'House rental cancel penalty');

        IF l_owner_amount > 0 THEN
            PERFORM fn_add_user_reward(r_owner_uid, r_type, l_owner_amount, l_deposit_type,
                                       'House rental cancel compensation');
        END IF;

        IF l_game_amount > 0 THEN
            SELECT NULLIF(value, '')::int INTO l_fee_uid
            FROM public.game_config WHERE key = 'house_rental_fee_uid';

            IF l_fee_uid IS NOT NULL AND l_fee_uid > 0 THEN
                PERFORM fn_add_user_reward(l_fee_uid, r_type, l_game_amount, l_deposit_type,
                                           'House rental cancel fee');
            END IF;
        END IF;

        INSERT INTO public.house_rental_payment (rental_id, house_id, type, day_number, amount, fee,
                                                 owner_amount, pay_token, owner_uid, renter_uid, kind)
        VALUES (_rental_id, r_house_id, r_type, 0, _penalty, l_game_amount,
                l_owner_amount, r_pay_token, r_owner_uid, _renter_uid, 'CANCEL_PENALTY');
    END IF;

    UPDATE public.house_rental
    SET status = 'ENDED_BY_RENTER', ended_at = CURRENT_TIMESTAMP
    WHERE id = _rental_id;

    -- The house goes back to the list, unless the owner already changed (on-chain sale)
    UPDATE public.house_rental_listing
    SET status = CASE WHEN EXISTS (SELECT 1 FROM public.user_house uh
                                   WHERE uh.house_id = r_house_id AND uh.type = r_type
                                     AND uh.uid = r_owner_uid)
                      THEN 'AVAILABLE' ELSE 'CANCELLED' END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = r_listing_id;
END;
$$;


-- -----------------------------------------------------------------------------
-- 5. Feature defaults (fee and limits), following the game_config convention
-- -----------------------------------------------------------------------------

INSERT INTO public.game_config (key, value)
VALUES ('house_rental_fee', '0.05'),           -- 5%, same as the on-chain marketplace
       ('house_rental_max_days', '30'),
       ('house_rental_min_price', '1'),
       -- uid receiving the 5% fee; empty = the fee leaves circulation
       ('house_rental_fee_uid', ''),
       -- Early cancellation by the renter: penalty over the amount still owed.
       -- 10% goes to the owner, 5% to the game (15% total).
       ('house_rental_cancel_fee_owner', '0.10'),
       ('house_rental_cancel_fee_game', '0.05')
ON CONFLICT (key) DO NOTHING;
