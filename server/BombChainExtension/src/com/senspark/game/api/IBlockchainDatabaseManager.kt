package com.senspark.game.api

import com.senspark.common.service.IServerService

interface IBlockchainDatabaseManager : IServerService {
    val heroDatabase: IHeroDatabase
    val houseDatabase: IHouseDatabase
    val depositedDatabase: IDepositedDatabase
}