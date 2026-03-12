export function removeNameSuffix(userNameL: string): string {
    if (userNameL.length > 42) {
        userNameL = userNameL.slice(0, 42);
    }
    return userNameL;
}