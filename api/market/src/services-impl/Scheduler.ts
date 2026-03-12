import {ILogger, IScheduler} from "@project/Services";

export default class Scheduler implements IScheduler {
    private timer: NodeJS.Timeout | null = null;
    private initialDelay: number = 0;
    private interval: number = 24 * 60 * 60 * 1000; // Default 24 hours
    private callback: (() => void) | null = null;
    private isRunning: boolean = false;
    private _logger: ILogger

    private constructor(logger: ILogger, name: string) {
        this._logger = logger.clone(`[Scheduler] [${name}]`);
    }

    static Builder(logger: ILogger, name: string = "default"): Scheduler {
        return new Scheduler(logger, name);
    }

    /**
     * Calculate milliseconds until next UTC midnight
     */
    private getMillisecondsUntilNextMidnight(): number {
        const now = new Date();
        const tomorrow = new Date(now);
        tomorrow.setUTCHours(24, 0, 0, 0);
        return tomorrow.getTime() - now.getTime();
    }

    /**
     * Set a custom initial delay in milliseconds
     */
    public setInitialDelay(delay: number): Scheduler {
        this.initialDelay = delay;
        return this;
    }

    /**
     * Set to use midnight UTC as initial delay
     */
    public delayUntilMidnight(): Scheduler {
        this.initialDelay = this.getMillisecondsUntilNextMidnight();
        return this;
    }

    /**
     * Set custom interval in milliseconds
     */
    public setInterval(interval: number): Scheduler {
        this.interval = interval;
        return this;
    }

    /**
     * Set the callback function to be executed
     */
    public setCallback(callback: () => void): Scheduler {
        this.callback = callback;
        return this;
    }

    private executeCallback(): void {
        if (!this.callback || !this.isRunning) return;

        try {
            this.callback();
        } catch (error) {
            this._logger.error(`Error executing scheduler callback: ${error}`);
        }

        if (this.isRunning) {
            this.timer = setTimeout(() => this.executeCallback(), this.interval);
        }
    }

    /**
     * Start the scheduler with configured parameters
     */
    public start(): Scheduler {
        if (!this.callback) {
            this._logger.error('Callback must be set before starting the scheduler');
        }

        if (this.isRunning) {
            this._logger.error('Scheduler is already running');
            return this;
        }

        this.isRunning = true;

        // Schedule first execution
        this.timer = setTimeout(() => {
            this.executeCallback();
        }, this.initialDelay);

        // Convert milliseconds to hours, minutes, seconds
        const totalSeconds = Math.floor(this.initialDelay / 1000);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;

        // Format as hh:mm:ss
        const formattedTime = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

        this._logger.info(`Scheduler started. First execution in ${this.initialDelay}ms (${formattedTime})`);
        // Setup safety net for unexpected errors
        process.on('uncaughtException', (error) => {
            this._logger.error(`Uncaught Exception in scheduler: ${error}`);
            this.restart();
        });

        process.on('unhandledRejection', (error) => {
            this._logger.error(`Unhandled Rejection in scheduler: ${error}`);
            this.restart();
        });

        return this;
    }

    /**
     * Restart the scheduler
     */
    private restart(): void {
        this._logger.info('Attempting to restart scheduler...');
        this.stop();
        this.start();
    }

    /**
     * Stop the scheduler
     */
    public stop(): void {
        this.isRunning = false;
        if (this.timer) {
            clearTimeout(this.timer);
            this.timer = null;
        }
    }
}