package com.senspark.game.api

import com.senspark.game.exception.CustomException

class NullBlockchainDatabaseManager : IBlockchainDatabaseManager {
    override val heroDatabase: IHeroDatabase get() = throw CustomException("Feature not support")
    override val houseDatabase: IHouseDatabase get() = throw CustomException("Feature not support")
    override val depositedDatabase: IDepositedDatabase get() = throw CustomException("Feature not support")

    override fun initialize() {
    }
}