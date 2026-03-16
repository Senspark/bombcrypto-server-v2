# Scribe Journal - Knowledge Gaps

- **Magic Numbers & Logic:**
  - `IS_TOURNAMENT_GAME_SERVER=false` vs `IS_GAME_SERVER=1` in `server/.env.example`: The boolean and integer representations for feature toggles are inconsistent across the config file.
  - Port `9898` TCP Internal in sfs-game-1 is mentioned in README.md but its exact internal usage (e.g., admin vs cluster communication) is not fully documented in the config or handlers.

- **Discrepancies:**
  - `api/login/src/routers/WebRouter.ts` mounts endpoints for `bsc`, `pol`, `ron`, and `bas`, but the README mentions "BSC, Polygon, RON, BAS, TON, Solana". Solana and TON have their own routers (`SolRouter.ts`, `TonRouter.ts`), which is a slight structural discrepancy compared to how EVM chains are handled.
  - The `app_stage` in `server/.env.example` allows `local`, `test`, `prod`, but `IS_PROD` in ap-login and ap-market is a boolean, showing inconsistent environment stage definitions across microservices.
