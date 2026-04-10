import {INetworkStats} from "../consts/PvpData";

const PING_THRESHOLD = 1;
const DEFAULT_ZONE = 'sg';
type PingData = Map<string, number>;

export default class NetworkStats implements INetworkStats {
    /**
     * Will normalize the number of pings by max ping
     */
    readonly pings: PingData;
    readonly lowestPingZone: string;

    constructor(pings: PingData) {
        if (!pings || pings.size === 0) {
            this.pings = new Map<string, number>([[DEFAULT_ZONE, PING_THRESHOLD]]);
            this.lowestPingZone = DEFAULT_ZONE;
        } else {
            const sumPings = Array.from(pings.values()).reduce((a, b) => a + b, 0);
            this.pings = new Map(Array.from(pings.entries()).map(([zone, ping]) => [zone, ping / sumPings]));
            this.lowestPingZone = NetworkStats.getLowestPingZone(this.pings);
        }
    }

    public get zones(): string[] {
        return Array.from(this.pings.keys());
    }

    static findLowestPingZone(a: INetworkStats, b: INetworkStats): string {

        if (a.lowestPingZone === b.lowestPingZone) {
            return a.lowestPingZone;
        }

        // The average of each user's zone is used to select the zone with the lowest pings
        const bothPings = new Map<string, number>();
        const bothZones = new Set<string>([...a.zones, ...b.zones]);
        bothZones.forEach(z => {
            if (a.pings.has(z) && b.pings.has(z)) {
                const avg = ((a.pings.get(z) ?? PING_THRESHOLD) + (b.pings.get(z) ?? PING_THRESHOLD)) / 2;
                bothPings.set(z, avg);
            }
        });

        return NetworkStats.getLowestPingZone(bothPings);
    }

    private static getLowestPingZone(pings: PingData): string {
        if (!pings || pings.size === 0) {
            throw new Error('No ping data');
        }

        let lowestPing = PING_THRESHOLD;
        let lowestZone = '';
        for (const zone of pings.keys()) {
            const ping = pings.get(zone) ?? PING_THRESHOLD;
            if (ping <= lowestPing) {
                lowestPing = ping;
                lowestZone = zone;
            }
        }

        return lowestZone;

    }

    public getPing(zone: string): number {
        return this.pings.get(zone) ?? PING_THRESHOLD;
    }
}