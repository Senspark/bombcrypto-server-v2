package com.senspark.game.api

import com.senspark.common.utils.ILogger
import com.senspark.game.manager.IEnvManager

class BlockchainDatabaseManager(
    api: IRestApi,
    envManager: IEnvManager,
    logger: ILogger,
) : IBlockchainDatabaseManager {
    override val heroDatabase: IHeroDatabase = BlockchainHeroDatabase(envManager.syncHeroUrl, logger)
    override val houseDatabase: IHouseDatabase = BlockchainHouseDatabase(api, envManager.syncHouseUrl)
    override val depositedDatabase: IDepositedDatabase = BlockchainDepositedDatabase(api, envManager.syncDepositedUrl)
    override fun initialize() {
    }
}