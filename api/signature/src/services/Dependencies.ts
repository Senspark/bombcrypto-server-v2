import ILogger from './ILogger';
import {EnvConfig, IConfig} from '../Config';
import ConsoleLogger from './ConsoleLogger';
import {RemoteLogger} from './loggers/RemoteLogger';
import {AppStage} from './loggers/AppStage';
import IDependencies from './IDependencies';

export default class Dependencies implements IDependencies {
  constructor(options?: IDependenciesData) {
    this.envConfig = options?.envConfig ?? new EnvConfig();

    const appStage: AppStage = this.envConfig.isProduction ? 'prod' : this.envConfig.isGcp ? 'test' : 'local';
    this.logger = new RemoteLogger({
      serviceName: 'ap-signature',
      instanceId: this.envConfig.logName,
      stage: appStage,
      remoteHost: this.envConfig.logRemoteHost,
    }, new ConsoleLogger('[DEFAULT]'));
  }

  envConfig: IConfig;
  logger: ILogger;
}

interface IDependenciesData {
  envConfig?: EnvConfig;
}
