export function randNumber(): number {
    // generate random number from 1_000_000 to 9_999_999
    const min = 1_000_000;
    const max = 9_999_999;
    return Math.floor(Math.random() * max) + min;
}