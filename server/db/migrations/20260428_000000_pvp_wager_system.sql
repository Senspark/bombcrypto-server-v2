-- FASE 2: Sistema de Apostas PVP (Escrow & Pool)

-- Tabela para gerenciar a pool de cada partida
CREATE TABLE IF NOT EXISTS public.pvp_wager_pool (
    match_id VARCHAR(64) PRIMARY KEY,
    token_type VARCHAR(20) NOT NULL, -- BCOIN, SEN
    network VARCHAR(20) NOT NULL,    -- BSC, POLYGON
    total_pool DOUBLE PRECISION DEFAULT 0,
    fee_amount DOUBLE PRECISION DEFAULT 0,
    status VARCHAR(20) DEFAULT 'OPEN', -- OPEN, LOCKED, PAID, REFUNDED
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc')
);

-- Tabela para gerenciar as entradas individuais (escrow)
CREATE TABLE IF NOT EXISTS public.pvp_wager_entry (
    match_id VARCHAR(64) NOT NULL,
    user_id INTEGER NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    token_type VARCHAR(20) NOT NULL,
    network VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, COMMITTED, REFUNDED
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc'),
    CONSTRAINT pvp_wager_entry_pk PRIMARY KEY (match_id, user_id),
    CONSTRAINT pvp_wager_entry_match_fk FOREIGN KEY (match_id) REFERENCES pvp_wager_pool(match_id)
);

-- Tabela para registro de taxas coletadas (Treasury Ledger)
CREATE TABLE IF NOT EXISTS public.pvp_fee_ledger (
    id SERIAL PRIMARY KEY,
    match_id VARCHAR(64) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    token_type VARCHAR(20) NOT NULL,
    network VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, TRANSFERRED
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc')
);

-- Index para performance de busca por match
CREATE INDEX IF NOT EXISTS idx_pvp_wager_entry_match ON public.pvp_wager_entry(match_id);
CREATE INDEX IF NOT EXISTS idx_pvp_fee_ledger_status ON public.pvp_fee_ledger(status);
