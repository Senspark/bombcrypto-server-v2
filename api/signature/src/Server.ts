import http from 'http';
import {AddressInfo} from 'net';
import {App} from './App';
import Dependencies from './services/Dependencies';

const deps = new Dependencies();
const app = new App(deps);

const server = http.createServer(app.express).listen(deps.envConfig.serverPort, () => {
    const address = server.address() as AddressInfo;
    deps.logger.info(`Server listening at http://${address.address}:${address.port}`);
});
