import {Response} from "express";
import {sleep} from "./Time";

export async function randomResponse(res: Response) {
    const randDelay = randomDelay(100, 5000);
    const randStatus = randomStatus();
    await sleep(randDelay);
    return res.status(randStatus).send('');
}

function randomPercent() {
    return Math.random() * 100;
}

function randomDelay(min: number, max: number) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomStatus() {
    const statuses = [400, 401, 403, 413, 429, 500, 502, 503, 504, 200];
    return statuses[Math.floor(Math.random() * statuses.length)];
}