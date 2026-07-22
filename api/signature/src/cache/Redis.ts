import {createClient, RedisClientType} from 'redis';

let redisClient: RedisClientType | null = null;
let lastLogMsg: string;

export default function getRedisClient(redisConnectionString: string) {
  if (!redisClient) {
    redisClient = createClient({
      url: redisConnectionString,
    });
    redisClient.on('error', e => {
      log(e?.message ?? String(e));
    });
    redisClient.on('ready', () => {
      log('Redis connected!');
    });
    redisClient.on('reconnecting', () => {
      log('Redis reconnecting...!');
    });
    redisClient.connect().catch(e => log(e.message));
  }
  return redisClient;
}

function log(msg: string) {
  if (msg !== lastLogMsg) {
    console.info(msg);
    lastLogMsg = msg;
  }
}
