-- Migration: config_hero_upgrade_stamina — ganho de energia dos niveis 6-10
--
-- Os niveis 6-10 alternam bonus: 6 e 8 dao energia, 7 e 9 dao poder, e o 10 da os dois. O poder ja
-- tem tabela propria (config_hero_upgrade_power); a energia nao tinha nenhuma, entao os niveis
-- pares nao entregavam nada.
--
-- Leitura: `datas` e indexado por nivel e guarda o TOTAL acumulado naquele nivel, nao o delta —
-- mesma convencao de config_hero_upgrade_power. Nivel 1 le datas[0].
--
--   nivel  1 2 3 4 5 6 7 8 9 10
--   power  0 1 2 3 5 5 6 6 7  8   <- config_hero_upgrade_power
--   stamina0 0 0 0 0 1 1 2 2  3   <- esta tabela
--
-- Um ponto de stamina vale 50 de energia (HeroHelper.ENERGY_PER_STAMINA), entao um Super Mystic
-- sai de 30 pontos / 1500 de energia no nivel 5 para 33 pontos / 1650 no nivel 10.
--
-- Os valores sao iguais para todas as raridades, igual ao que ja acontece na tabela de power.

BEGIN;

CREATE TABLE IF NOT EXISTS config_hero_upgrade_stamina (
    rare integer NOT NULL,
    datas text
);

ALTER TABLE config_hero_upgrade_stamina
    DROP CONSTRAINT IF EXISTS config_hero_upgrade_stamina_pkey;
ALTER TABLE config_hero_upgrade_stamina
    ADD CONSTRAINT config_hero_upgrade_stamina_pkey PRIMARY KEY (rare);

INSERT INTO config_hero_upgrade_stamina (rare, datas) VALUES
  (0, '[0,0,0,0,0,1,1,2,2,3]'),  -- Common
  (1, '[0,0,0,0,0,1,1,2,2,3]'),  -- Rare
  (2, '[0,0,0,0,0,1,1,2,2,3]'),  -- Super Rare
  (3, '[0,0,0,0,0,1,1,2,2,3]'),  -- Epic
  (4, '[0,0,0,0,0,1,1,2,2,3]'),  -- Legend
  (5, '[0,0,0,0,0,1,1,2,2,3]'),  -- Super Legend
  (6, '[0,0,0,0,0,1,1,2,2,3]'),  -- Mega
  (7, '[0,0,0,0,0,1,1,2,2,3]'),  -- Super Mega
  (8, '[0,0,0,0,0,1,1,2,2,3]'),  -- Mystic
  (9, '[0,0,0,0,0,1,1,2,2,3]')   -- Super Mystic
ON CONFLICT (rare) DO UPDATE SET datas = EXCLUDED.datas;

COMMIT;

-- Verificar — 10 linhas, 0..9:
--   SELECT rare, datas FROM config_hero_upgrade_stamina ORDER BY rare;
