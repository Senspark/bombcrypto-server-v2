import {IUser} from "../../consts/PvpData";

export default class GroupUsersInSameZone {
    createZoneGroup(users: IUser[]): Map<string, IUser[]> {
        const zones = new Map<string, IUser[]>();
        for (const user of users) {
            const zone = user.networkStats.lowestPingZone;
            if (!zones.has(zone)) {
                zones.set(zone, []);
            }
            zones.get(zone)!!.push(user);
        }

        // remove zone with only 1 user
        for (const [zone, usersInZone] of zones) {
            if (usersInZone.length < 2) {
                zones.delete(zone);
            }
        }
        return zones;
    }
}