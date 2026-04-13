import IDependencies from "../services/IDependencies";
import {Request, Response} from "express";
import fs from "fs";
import {IServerDetail} from "../processors/NetworkAddress";

export default class PvpConfigHandlers {
    readonly _zones: IZoneInfo[];
    readonly _servers: IServerInfo[];
    readonly _servers2: IServerInfo[];

    constructor(private readonly _deps: IDependencies) {
        this._servers = [];
        this._servers2 = [];
        const fileName = `./data/PvpServerAddress.${_deps.envConfig.runTimeEnv}.json`;
        const fileName2 = `./data/PvpServerAddress.${_deps.envConfig.runTimeEnv}.2.json`;
        this._servers = JSON.parse(fs.readFileSync(fileName, `utf8`));
        this._servers2 = JSON.parse(fs.readFileSync(fileName2, `utf8`));
        this._zones = JSON.parse(fs.readFileSync(`./data/PingServerAddress.json`, `utf8`));
    }

    /**
     * for legacy code
     * @param req
     * @param res
     */
    getConfig(req: Request, res: Response) {
        if (req.query.newServer === "true") {
            res.send({
                code: 0,
                message: {
                    zones: this._zones,
                    servers: this._servers2,
                },
            });
        } else {
            res.send({
                code: 0,
                message: {
                    zones: this._zones,
                    servers: this._servers,
                },
            });
        }


    }

    //legacy code support old client
    getServerId() {
        const configMap = new Map<string, string>();

        for (let i = 0; i < this._servers.length; i++) {
            configMap.set(this._servers[i].zone, this._servers[i].id);
        }

        return configMap;
    }

    getServerConfig() {
        const configMap = new Map<string, IServerDetail>();

        for (let i = 0; i < this._servers.length; i++) {
            const server = this._servers[i];
            const serverDetail: IServerDetail = {
                id: server.id,
                useSSl: server.use_ssl,
                detail: {
                    web: {
                        address: server.host,
                        port: server.port,
                    },
                    editor: {
                        address: server.udp_host,
                        port: server.udp_port,
                    }
                }
            };
            configMap.set(server.zone + "1", serverDetail);
        }
        for (let i = 0; i < this._servers2.length; i++) {
            const server = this._servers2[i];
            const serverDetail: IServerDetail = {
                id: server.id,
                useSSl: server.use_ssl,
                detail: {
                    web: {
                        address: server.host,
                        port: server.port,
                    },
                    editor: {
                        address: server.udp_host,
                        port: server.udp_port,
                    }
                }
            };
            configMap.set(server.zone, serverDetail);
        }
        return configMap;
    }
}

interface IZoneInfo {
    id: string,
    host: string,
}

interface IServerInfo {
    id: string,
    zone: string,
    host: string,
    port: number,
    use_ssl: boolean,
    udp_host: string,
    udp_port: number,

    useSsl: boolean, // Backward compatibility.
}