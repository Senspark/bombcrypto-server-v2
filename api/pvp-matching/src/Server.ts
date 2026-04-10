import http from 'http';
import {AddressInfo} from 'net';
import {App} from "./App";
import Dependencies from "./services/Dependencies";

const dependencies = new Dependencies();
const app = new App(dependencies);

const server = http.createServer(app.express).listen(dependencies.envConfig.serverPort, () => {
    const address = server.address() as AddressInfo;
    dependencies.logger.info(`Server listening at http://${address.address}:${address.port}`);
});