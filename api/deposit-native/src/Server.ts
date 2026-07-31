import http from 'http';
import {AddressInfo} from 'net';
import {App} from './App';
import {EnvConfig} from './Config';
import ConsoleLogger from './ConsoleLogger';

const logger = new ConsoleLogger('[ap-deposit-native]');
const config = new EnvConfig();
const app = new App(config, logger);

// What this instance is actually configured with. Without it, a request failing on one network while
// another works reads as a bug rather than as a blank address slot.
for (const [network, cfg] of config.networks) {
  const address = cfg.depositNativeAddress || '<not deployed>';
  logger.info(
    `${network}: contract=${address} chainId=${cfg.chainId} confirmations=${cfg.confirmations} signWindow=${cfg.signWindowSeconds}s`,
  );
}
logger.info(`prod=${config.isProduction} center=${config.blockchainCenterApi}`);

app.startStreamListener();

const server = http.createServer(app.express).listen(config.serverPort, () => {
  const address = server.address() as AddressInfo;
  logger.info(`listening at http://${address.address}:${address.port}`);
});

// Otherwise these land on stdout with no tag and no timestamp, or kill the process silently.
process.on('unhandledRejection', (reason) => logger.errors('unhandledRejection', reason));
process.on('uncaughtException', (e) => logger.errors('uncaughtException', e));
