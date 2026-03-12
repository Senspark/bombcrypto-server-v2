export default interface IScheduler {
    setInitialDelay(delay: number): IScheduler;
    delayUntilMidnight(): IScheduler;
    setInterval(interval: number): IScheduler;
    setCallback(callback: () => void): IScheduler;
    start(): IScheduler;
    stop(): void;
}
