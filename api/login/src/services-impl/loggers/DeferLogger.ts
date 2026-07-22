import {ILogger} from "../../Services";

/**
 * Decorator that runs the wrapped logger's calls off the critical path:
 * each log is pushed to the next tick (setImmediate) and guarded so a logging
 * failure can never break or delay the caller.
 *
 * `assert` is control-flow validation (throws synchronously), so it is NOT
 * deferred and delegates straight to the inner logger.
 */
export class DeferLogger implements ILogger {
    constructor(private readonly _inner: ILogger) {
    }

    private defer(fn: () => void): void {
        setImmediate(() => {
            try {
                fn();
            } catch {
                // Logging must never affect the caller.
            }
        });
    }

    info(message: any): void {
        this.defer(() => this._inner.info(message));
    }

    infos(...message: any[]): void {
        this.defer(() => this._inner.infos(...message));
    }

    error(err: any): void {
        this.defer(() => this._inner.error(err));
    }

    errors(...err: any[]): void {
        this.defer(() => this._inner.errors(...err));
    }

    assert(condition: any, message: string): void {
        this._inner.assert(condition, message);
    }

    clone(prefix: string): ILogger {
        return new DeferLogger(this._inner.clone(prefix));
    }
}
