import {createClient, RedisClientType} from 'redis';
import ILogger from '../services/ILogger';

let redisClient: RedisClientType | null = null;

export default function getRedisClient(redisConnectionString: string, logger: ILogger) {
  if (!redisClient) {
    const log = logger.clone('[REDIS]');
    redisClient = createClient({url: redisConnectionString});
    redisClient.on('error', (e) => log.error(e));
    redisClient.on('ready', () => log.info(`connected to ${redact(redisConnectionString)}`));
    redisClient.on('reconnecting', () => log.warn('reconnecting...'));
    redisClient.on('end', () => log.warn('connection closed'));
    redisClient.connect().catch((e) => log.error(e));
  }
  return redisClient;
}

// The url can carry a password; the host is the only part worth logging.
function redact(url: string): string {
  try {
    const parsed = new URL(url);
    return `${parsed.protocol}//${parsed.host}${parsed.pathname}`;
  } catch {
    return '<unparseable url>';
  }
}
