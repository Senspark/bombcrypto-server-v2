-- Ranking semanal
CREATE TABLE pvp_weekly_ranking (
    user_id INT, 
    week_number INT, 
    year INT,
    points INT DEFAULT 0, 
    wins INT DEFAULT 0, 
    losses INT DEFAULT 0,
    matches_played INT DEFAULT 0, 
    game_mode VARCHAR(20),
    tier VARCHAR(20) DEFAULT 'BRONZE',
    UNIQUE(user_id, week_number, year, game_mode)
);

-- Ranking mensal
CREATE TABLE pvp_monthly_ranking (
    user_id INT, 
    month INT, 
    year INT,
    points INT DEFAULT 0, 
    wins INT DEFAULT 0, 
    losses INT DEFAULT 0,
    total_wagered DECIMAL(18,8) DEFAULT 0, 
    total_won DECIMAL(18,8) DEFAULT 0,
    game_mode VARCHAR(20), 
    tier VARCHAR(20) DEFAULT 'BRONZE',
    UNIQUE(user_id, month, year, game_mode)
);

-- Configurações de Tier
CREATE TABLE pvp_tier_config (
    tier VARCHAR(20) PRIMARY KEY,
    min_points INT,
    reset_floor INT
);

INSERT INTO pvp_tier_config (tier, min_points, reset_floor) VALUES
('BRONZE', 0, 0),
('SILVER', 500, 400),
('GOLD', 1200, 1000),
('PLATINUM', 2000, 1700),
('DIAMOND', 3000, 2500),
('MASTER', 5000, 4000);
