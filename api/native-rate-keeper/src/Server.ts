import cors from 'cors';
import express from 'express';
import http from 'http';
import {AddressInfo} from 'net';
import getRedisClient from './cache/Redis';
import MessengerService from './cache/MessengerService';
import {EnvConfig} from './Config';
import ConsoleLogger from './ConsoleLogger';
import {INativeRateMessage, StreamKeys} from './consts/Messages';
import {NetworkKeeper} from './RateKeeper';
import {StreamHandlers} from './StreamHandlers';

const logger = new ConsoleLogger('[ap-native-rate-keeper]');
const config = new EnvConfig();

const keepers = new Map<string, NetworkKeeper>();
for (const [network, cfg] of config.networks) {
  keepers.set(
    network,
    new NetworkKeeper(cfg, config.privateKey, {
      minChangePercent: config.minChangePercent,
      maxChangePercent: config.maxChangePercent,
      dryRun: config.dryRun,
    }, logger),
  );
}
logger.info(
  `prod=${config.isProduction} dryRun=${config.dryRun} minChange=${config.minChangePercent}% ` +
    `maxChange=${config.maxChangePercent}% maxAge=${config.maxMessageAgeMinutes}min`,
);

const handlers = new StreamHandlers(
  logger,
  keepers,
  new MessengerService(logger, config.redisConnectionString),
  config.maxMessageAgeMinutes * 60_000,
);

// Resolve HeroDesign per network, retrying while the RPC is unreachable. Each network retries on its
// own, so one that never comes up does not block the other; rates received meanwhile are kept by the
// keeper and applied once its init succeeds.
async function initKeeper(network: string, keeper: NetworkKeeper): Promise<void> {
  for (;;) {
    try {
      await keeper.init();
      return;
    } catch (e) {
      logger.errors(`${network}: init failed, retrying in 60s`, e);
      await new Promise((resolve) => setTimeout(resolve, 60_000));
    }
  }
}

// The stream listener only sees messages published after it starts, so the rate the game server sent
// while we were down would wait a whole tick. Replay the newest message per network once at startup.
async function replayLatest(): Promise<void> {
  try {
    const redis = getRedisClient(config.redisConnectionString, logger);
    const entries = await redis.xRevRange(StreamKeys.SV_NATIVE_RATE_STR, '+', '-', {COUNT: 20});
    const seen = new Set<string>();
    for (const entry of entries) {
      const msg = JSON.parse(entry.message.data) as INativeRateMessage;
      if (!msg?.network || seen.has(msg.network)) {
        continue;
      }
      seen.add(msg.network);
      handlers.handle(msg);
    }
  } catch (e) {
    logger.errors('startup replay failed; waiting for the next tick instead', e);
  }
}

for (const [network, keeper] of keepers) {
  initKeeper(network, keeper).catch((e) => logger.errors(`${network}: init loop failed`, e));
}
handlers.register();
replayLatest().catch((e) => logger.errors('startup replay failed', e));

// Operator view: what each chain holds, what we last tried to write, and the last failure.
const app = express();
app.use(cors());
app.get('/', (_req, res) => res.send('ap-native-rate-keeper'));
app.get('/health', (_req, res) => res.json({ok: true}));
app.get('/status', (_req, res) => res.json([...keepers.values()].map((k) => k.status)));

const server = http.createServer(app).listen(config.serverPort, () => {
  const address = server.address() as AddressInfo;
  logger.info(`listening at http://${address.address}:${address.port}`);
});

// Otherwise these land on stdout with no tag and no timestamp, or kill the process silently.
process.on('unhandledRejection', (reason) => logger.errors('unhandledRejection', reason));
process.on('uncaughtException', (e) => logger.errors('uncaughtException', e));
