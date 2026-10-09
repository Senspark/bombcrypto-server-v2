-- Migration: estende config_hero_upgrade_power do nivel 5 para o nivel 10
--
-- A tabela guardava 5 entradas por raridade ([0,1,2,3,5]), suficientes so ate o nivel 5. Com os
-- niveis 6-10 o HeroUpgradePowerManager.getPowerIncrease indexa datas[level-1] e, faltando entrada,
-- devolve 0 — o heroi subia de nivel, pagava, e nao ganhava poder nenhum.
--
-- Leitura: `datas` e indexado por nivel e guarda o TOTAL acumulado naquele nivel, nao o delta.
--
--   nivel   1  2  3  4  5   6  7  8  9  10
--   power   0  1  2  3  5   5  6  6  7   8   <- esta tabela
--   stamina 0  0  0  0  0   1  1  2  2   3   <- config_hero_upgrade_stamina
--
-- Alternancia pedida: 6 da energia, 7 da poder, 8 da energia, 9 da poder, 10 da os dois. Por isso
-- power repete no 6 e no 8 (5,5 e 6,6) — quem sobe e a stamina nesses niveis.
--
-- O WHERE restringe ao formato antigo de proposito: reexecutar nao sobrescreve nenhum ajuste
-- manual de balanceamento que ja tenha sido feito.

BEGIN;

UPDATE config_hero_upgrade_power
SET datas = '[0,1,2,3,5,5,6,6,7,8]'
WHERE datas = '[0,1,2,3,5]';

COMMIT;

-- Verificar — as 10 raridades devem ter 10 entradas:
--   SELECT rare, datas FROM config_hero_upgrade_power ORDER BY rare;
