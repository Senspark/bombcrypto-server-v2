export default class NetworkAddress {
    _serverAddress: Map<string, IServerDetail> = new Map();
    DEFAULT_ZONE: string = 'sg';

    constructor(serverAddress: Map<string, IServerDetail>) {
        this._serverAddress = serverAddress;
        serverAddress.forEach(e => {
            this._serverAddress.set(e.id, e);
        });
    }

    //legacy code support old client
    convertZoneToServerId(zone: string): string {
        return this._serverAddress.get(zone)?.id ?? this._serverAddress.get(this.DEFAULT_ZONE)?.id!!;
    }

    convertZoneToServerDetail(zone: string): IServerDetail {
        return this._serverAddress.get(zone) ?? this._serverAddress.get(this.DEFAULT_ZONE)!!;
    }

}

export interface IServerDetail {
    id: string;
    useSSl: boolean;
    detail: {
        web: {
            address: string;
            port: number;
        };
        editor: {
            address: string;
            port: number;
        };
    };
}