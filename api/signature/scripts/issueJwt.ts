import dotenv from 'dotenv';
import JwtService from '../src/services/JwtService';
import type IDependencies from '../src/services/IDependencies';
import type {IConfig} from '../src/Config';
import type ILogger from '../src/services/ILogger';

dotenv.config();

const jwtSecret = process.env.JWT_SECRET;
const jwtPayloadKey = process.env.JWT_PAYLOAD_KEY;

if (!jwtSecret || !jwtPayloadKey) {
  console.error('JWT_SECRET and JWT_PAYLOAD_KEY must be set in .env');
  process.exit(1);
}

const noopLogger: ILogger = {
  info: () => {},
  infos: () => {},
  error: () => {},
  errors: () => {},
  assert: () => {},
  clone: () => noopLogger,
};

const dep: IDependencies = {
  logger: noopLogger,
  envConfig: {jwtSecret, jwtPayloadKey} as IConfig,
};

const token = new JwtService(dep).sign();
console.log(token);
