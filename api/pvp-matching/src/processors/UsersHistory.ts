import md5 from "md5";

export default class UsersHistory {
    readonly _userHistories = new Set<string>();

    add(users: string[]): void {
        const key = this.getKey(users);
        this._userHistories.add(key);
    }

    has(users: string[]): boolean {
        const key = this.getKey(users);
        return this._userHistories.has(key);
    }

    remove(users: string[]): void {
        const key = this.getKey(users);
        this._userHistories.delete(key);
    }

    private getKey(users: string[]): string {
        const stableSorted = users.sort();
        return md5(stableSorted.join(`_`));
    }
}