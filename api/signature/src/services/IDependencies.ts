import ILogger from './ILogger';
import {IConfig} from '../Config';

export default interface IDependencies {
  logger: ILogger;
  envConfig: IConfig;
}
