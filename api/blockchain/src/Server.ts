import http from 'http';
import {AddressInfo} from 'net';
import * as Scheduler from './subscribers/Scheduler';
import {App} from "./App";
import Dependencies from "./services/Dependencies";

const deps = new Dependencies();

if (deps.envConfig.useSubscriberOnly) {
    Scheduler.start(deps).then();
} else {
    const app = new App(deps);

    const server = http.createServer(app.express).listen(deps.envConfig.serverPort, () => {
        const address = server.address() as AddressInfo;
        if (deps.envConfig.isGcp) {
            Scheduler.start(deps).then();
        }
        deps.logger.info(`Server listening at http://${address.address}:${address.port}`);
    });
}