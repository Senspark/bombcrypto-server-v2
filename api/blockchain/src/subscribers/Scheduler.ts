import {sleep} from "../Utility";
import {HeroStakeApiDatabase} from "../database/HeroStakeApi.Database";
import {HeroStakeApiBlockchain} from "../contracts/HeroStakeApiBlockchain";
import {CHAIN_NAMES} from "../BlockchainConfig";
import {ConfigKeys, DatabaseConfig} from "../database/DatabaseConfig";
import {HeroStakeApiCache} from "../database/HeroStakeApi.Cache";
import IDependencies from "../services/IDependencies";
import {BlockchainCenterApi} from "../providers/BlockchainCenterApi";

const MAX_STEP = 500;
const LATEST_BLOCK_OFFSET = 10; // Lùi lại vài block để tránh lỗi
const BACKUP_BLOCK_OFFSET = 100;

export async function start(dependencies: IDependencies) {
    const envConfig = dependencies.envConfig;
    const database = dependencies.database;

    const isProduction = envConfig.isProduction;
    const databaseConfig = DatabaseConfig.createDefault(isProduction, database);
    await databaseConfig.initialize();

    startBsc(dependencies, databaseConfig).then();
    startPolygon(dependencies, databaseConfig).then();
}

async function startBsc(dependencies: IDependencies, databaseConfig: DatabaseConfig) {
    const network = CHAIN_NAMES.BNB;
    const prefix = '[BSC]';
    const blockchainConfig = dependencies.envConfig.blockchainConfigs.get(network);
    if (!blockchainConfig) {
        throw new Error(`BSC blockchain config doesn't exist`);
    }

    const envConfig = dependencies.envConfig;
    const apiDatabase = new HeroStakeApiCache(dependencies, new HeroStakeApiDatabase(dependencies, network));

    // Create BlockchainCenterApi for BSC
    const blockchainCenterApi = new BlockchainCenterApi(
        envConfig.blockchainCenterApi,
        prefix
    );

    // Pass BlockchainCenterApi to HeroStakeApiBlockchain
    const apiBlockchain = new HeroStakeApiBlockchain(apiDatabase, blockchainConfig, prefix, blockchainCenterApi);

    const [minSleepTime, maxSleepTime] = envConfig.schedulerIntervalSeconds.map(e => e * 1000);

    queryHeroStakeBsc(blockchainCenterApi, minSleepTime, maxSleepTime, apiBlockchain, databaseConfig, prefix, {jumpToLatestBlock: envConfig.jumpToLatestBlock}).then();
}

async function startPolygon(dependencies: IDependencies, databaseConfig: DatabaseConfig) {
    const network = CHAIN_NAMES.POLYGON;
    const prefix = '[POL]';
    const blockchainConfig = dependencies.envConfig.blockchainConfigs.get(network);
    if (!blockchainConfig) {
        throw new Error(`Polygon blockchain config doesn't exist`);
    }

    const envConfig = dependencies.envConfig;
    const apiDatabase = new HeroStakeApiCache(dependencies, new HeroStakeApiDatabase(dependencies, network));

    // Create BlockchainCenterApi (required for Polygon)
    const blockchainCenterApi = new BlockchainCenterApi(
        envConfig.blockchainCenterApi,
        prefix
    );

    // Pass BlockchainCenterApi to HeroStakeApiBlockchain for Polygon
    const apiBlockchain = new HeroStakeApiBlockchain(apiDatabase, blockchainConfig, prefix, blockchainCenterApi);

    const [minSleepTime, maxSleepTime] = envConfig.schedulerIntervalSeconds.map(e => e * 1000);

    queryHeroStakePolygon(blockchainCenterApi, minSleepTime, maxSleepTime, apiBlockchain, databaseConfig, prefix, {jumpToLatestBlock: envConfig.jumpToLatestBlock}).then();
}

async function queryHeroStakeBsc(
    blockchainCenterApi: BlockchainCenterApi,
    minSleepTime: number,
    maxSleepTime: number,
    apiBlockchain: HeroStakeApiBlockchain,
    databaseConfig: DatabaseConfig,
    logPrefix: string,
    option?: IOption
) {
    let startBlock = databaseConfig.getConfig().latestBlockBsc;
    const updateDatabase = async (latestBlock: string) => {
        await databaseConfig.setConfig(ConfigKeys.LatestBlockBsc, latestBlock);
    };
    queryHeroStake(
        startBlock,
        minSleepTime,
        maxSleepTime,
        apiBlockchain,
        logPrefix,
        updateDatabase,
        () => blockchainCenterApi.getLatestBlockNumber('bsc'),
        option
    ).catch(console.error);
}

async function queryHeroStakePolygon(
    blockchainCenterApi: BlockchainCenterApi,
    minSleepTime: number,
    maxSleepTime: number,
    apiBlockchain: HeroStakeApiBlockchain,
    databaseConfig: DatabaseConfig,
    logPrefix: string,
    option?: IOption
) {
    let startBlock = databaseConfig.getConfig().latestBlockPolygon;
    const updateDatabase = async (latestBlock: string) => {
        await databaseConfig.setConfig(ConfigKeys.LatestBlockPolygon, latestBlock);
    };
    queryHeroStake(
        startBlock,
        minSleepTime,
        maxSleepTime,
        apiBlockchain,
        logPrefix,
        updateDatabase,
        () => blockchainCenterApi.getLatestBlockNumber('polygon'),
        option
    ).catch(console.error);
}

async function queryHeroStake(
    startBlock: number,
    minSleepTime: number,
    maxSleepTime: number,
    apiBlockchain: HeroStakeApiBlockchain,
    logPrefix: string,
    setConfig: (value: string) => Promise<void>,
    getLatestBlock: () => Promise<number | null>,
    option?: IOption
) {
    if (option?.jumpToLatestBlock) {
        const result = await getLatestBlock();
        if (result) {
            startBlock = result;
        }
    }

    let sleepTime = minSleepTime;
    const loop = true;

    while (loop) {
        try {
            await sleep(sleepTime);
            const latestBlockOnChain = await getLatestBlock();
            if (!latestBlockOnChain) {
                continue;
            }
            let toBlock = latestBlockOnChain - LATEST_BLOCK_OFFSET;
            startBlock -= BACKUP_BLOCK_OFFSET;
            const blockLeft = toBlock - startBlock;

            if (blockLeft > MAX_STEP) {
                toBlock = startBlock + MAX_STEP;
                sleepTime = minSleepTime;
            } else {
                sleepTime = maxSleepTime;
                if (toBlock <= startBlock) {
                    continue;
                }
            }

            console.info(`${logPrefix} queryHeroStake ${latestBlockOnChain} [${startBlock}, ${toBlock}] (${blockLeft} left)`)
            await apiBlockchain.queryEvents(startBlock, toBlock);
            startBlock = toBlock + 1;
            await setConfig(startBlock.toString());
        } catch (e) {
            console.error(e);
            sleepTime = minSleepTime;
        }
    }
}

// async function queryHouseTransfer(
//   apiQuery: ApiQuery,
//   minSleepTime: number,
//   maxSleepTime: number,
//   apiBlockchain: HouseApiBlockchain,
//   databaseConfig: DatabaseConfig
// ) {
//
//   let startBlockBsc = databaseConfig.getConfig().latestBlockHouseBsc;
//   let sleepTime = minSleepTime;
//   const loop = true;
//
//   while (loop) {
//     try {
//       await sleep(sleepTime);
//       let toBlock = await apiQuery.getLatestBlock();
//       console.info(`queryHouseTransfer - latest block is ${toBlock}, (${toBlock - startBlockBsc} left)`)
//       if (toBlock - startBlockBsc > MAX_STEP) {
//         toBlock = startBlockBsc + MAX_STEP;
//         sleepTime = minSleepTime;
//       } else {
//         sleepTime = maxSleepTime;
//       }
//       await apiBlockchain.queryTransferEvents(startBlockBsc, toBlock);
//       startBlockBsc = toBlock + 1;
//       await databaseConfig.setConfig(ConfigKeys.LatestBlockHouseBsc, startBlockBsc.toString());
//     } catch (e) {
//       console.error(e);
//       sleepTime = minSleepTime;
//     }
//   }
// }

interface IOption {
    jumpToLatestBlock?: boolean;
}