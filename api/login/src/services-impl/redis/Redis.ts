import {createClient, RedisClientType} from "redis";
import {ILogger} from "../../Services";

let redisClient: RedisClientType | null = null;
let lastLogMsg: string;

export default function getRedisClient(
    redisConnectionString: string,
    logger: ILogger,
) {
    if (!redisClient) {
        redisClient = createClient({
            url: redisConnectionString,
        });
        redisClient.on('error', e => {
            log(logger, e['message']);
        });
        redisClient.on('ready', e => {
            log(logger, "Redis connected!");
        });
        redisClient.on('reconnecting', e => {
            log(logger, "Redis reconnecting...!");
        });
        redisClient.connect().catch(e => log(logger, e.message));
    }
    return redisClient;
}

function log(logger: ILogger, msg: string) {
    if (msg !== lastLogMsg) {
        logger.info(msg);
        lastLogMsg = msg;
    }
}