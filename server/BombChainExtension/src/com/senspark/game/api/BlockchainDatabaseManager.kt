package com.senspark.game.api

import com.senspark.common.utils.ILogger
import com.senspark.game.manager.IEnvManager

class BlockchainDatabaseManager(
    api: IRestApi,
    envManager: IEnvManager,
    logger: ILogger,
) : IBlockchainDatabaseManager {
    override val heroDatabase: IHeroDatabase = BlockchainHeroDatabase(api, envManager.syncHeroUrl, envManager.syncHeroUrlV4, envManager.refreshHeroStakeUrl, logger)
    override val houseDatabase: IHouseDatabase = BlockchainHouseDatabase(api, envManager.syncHouseUrl, envManager.syncHouseUrlV4)
    override val depositedDatabase: IDepositedDatabase = BlockchainDepositedDatabase(api, envManager.syncDepositedUrl, envManager.syncDepositedUrlV4)
    override fun initialize() {
    }
}