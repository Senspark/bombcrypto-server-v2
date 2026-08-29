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

    static findLowestPingZone(...stats: INetworkStats[]): string {
        if (stats.length === 0) return DEFAULT_ZONE;
        if (stats.length === 1) return stats[0].lowestPingZone;

        // The average of each user's zone is used to select the zone with the lowest pings
        const combinedPings = new Map<string, number>();
        const allZones = new Set<string>(stats.flatMap(s => s.zones));
        
        allZones.forEach(z => {
            let allHaveZone = true;
            let sum = 0;
            for (const s of stats) {
                if (!s.pings.has(z)) {
                    allHaveZone = false;
                    break;
                }
                sum += s.pings.get(z) ?? PING_THRESHOLD;
            }
            if (allHaveZone) {
                combinedPings.set(z, sum / stats.length);
            }
        });

        if (combinedPings.size === 0) {
            return stats[0].lowestPingZone;
        }

        return NetworkStats.getLowestPingZone(combinedPings);
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