import fluentLogger from 'fluent-logger';
import {ILogger} from "../../Services";
import {ValidationError} from "../../consts/ServerError";
import {AppStage} from "./AppStage";

let internalRemoteLogger : InternalRemoteLogger; // caching

export interface RemoteLoggerInitData {
    serviceName: string;
    instanceId?: string;
    stage: AppStage;
    remoteHost?: string;
}

export class RemoteLogger implements ILogger {//
    private readonly _logger?: ILogger;
    private readonly _fallback?: ILogger;

    constructor(
        private readonly _initData: RemoteLoggerInitData,
        fallback?: ILogger,
        private readonly _zoneName?: string,
    ) {
        if (_initData.stage === 'local') {
            this._fallback = fallback;
        }
        if (!internalRemoteLogger) {
            if (_initData.remoteHost) {
                const [host, portStr] = _initData.remoteHost.split(':');
                const port = portStr ? parseInt(portStr, 10) : 0;
                if (port > 0) {
                    internalRemoteLogger = this._logger = new InternalRemoteLogger(_initData, _zoneName, host, port);
                }
            }
        } else {
            this._logger = internalRemoteLogger;
        }
    }

    info(message: any): void {
        this._fallback?.info(message);
        this._logger?.info(message);
    }

    infos(...message: any[]): void {
        this._fallback?.infos(message);
        this._logger?.infos(message);
    }

    error(err: any): void {
        this._fallback?.error(err);
        this._logger?.error(err);
    }

    errors(...err: any[]): void {
        this._fallback?.errors(err);
        this._logger?.errors(err);
    }

    assert(condition: any, message: string): void {
        if (!condition) {
            this._fallback?.error(message);
            this._logger?.error(message);
            throw new ValidationError(message);
        }
    }

    clone(prefix: string): ILogger {
        if (this._fallback) {
            return new RemoteLogger(this._initData, this._fallback.clone(prefix), this._zoneName);
        }
        return new RemoteLogger(this._initData, undefined, this._zoneName);
    }
}

let fluent: fluentLogger.FluentSender<unknown>;

function createFluent(tag: string, host: string, port: number) {
    if (!fluent) {
        fluent = fluentLogger.createFluentSender(tag, {host, port});
        console.log(`Use FluentLogger with tag=${tag} host=${host} port=${port}`);
    }
    return fluent;
}

class InternalRemoteLogger implements ILogger {
    private logger: fluentLogger.FluentSender<unknown>;
    private constructorHelper: LogMessageConstructor;

    constructor(
        initData: RemoteLoggerInitData,
        zoneName: string | undefined,
        host: string,
        port: number,
    ) {
        this.constructorHelper = new LogMessageConstructor(initData, zoneName);
        const tag = `service.${this.constructorHelper.tag}`;
        this.logger = createFluent(tag, host, port);
    }

    info(message: any): void {
        const m = this.turnObjIntoString(message);
        this.logger.emit('event', this.constructorHelper.info(m));
    }

    infos(...message: any[]): void {
        const arr: string[] = [];
        message.forEach(e => arr.push(this.turnObjIntoString(e)));
        const m = arr.join(', ');
        this.logger.emit('event', this.constructorHelper.info(m));
    }

    error(message: string) {
        this.logger.emit('event', this.constructorHelper.error(message));
    }

    errors(...err: any[]): void {
        const arr: string[] = [];
        err.forEach(e => arr.push(this.turnObjIntoString(e)));
        const m = arr.join(', ');
        this.logger.emit('event', this.constructorHelper.error(m));
    }

    assert(condition: any, message: string): void {
        throw new Error('Method not implemented.');
    }

    clone(prefix: string): ILogger {
        return this;
    }

    private turnObjIntoString(obj: any): string {
        const t = typeof obj;
        if (t === 'string') {
            return obj;
        }
        if (t === 'number' || t === 'boolean') {
            return obj.toString();
        }
        if (t === 'object') {
            if (obj instanceof Error) {
                const ex = obj;
                const stack = ex.stack?.split('\n').slice(0, 5).join('\n') || '';
                return `${ex.message}\n${stack}`;
            }
            return JSON.stringify(obj);
        }
        throw new Error(`Unknown type of ${t}`);
    }
}

class LogMessageConstructor {
    public tag: string;
    private readonly _stageName: string;
    private readonly _instanceId?: string;

    constructor(
        initData: RemoteLoggerInitData,
        private readonly _zoneName?: string,
    ) {
        this._stageName = initData.stage.toLowerCase();
        this._instanceId = initData.instanceId;
        this.tag = `${initData.serviceName}.${this._stageName}${this._instanceId ? '.' + this._instanceId : ''}`;
    }

    private constructLog(message: string, level: string) {
        const data: Record<string, string> = {
            message,
            level,
            service: this.tag,
            stage: this._stageName,
        };

        if (this._instanceId) data['instance_id'] = this._instanceId;
        if (this._zoneName) data['zone'] = this._zoneName;

        return data;
    }

    info(message: string) {
        return this.constructLog(message, 'info');
    }

    error(message: string) {
        return this.constructLog(message, 'error');
    }
}