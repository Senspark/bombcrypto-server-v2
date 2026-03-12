package com.senspark.game.manager.blockChain

import com.senspark.common.service.IServerService

interface IBlockchainResponseManager: IServerService {
    fun listenSyncHero(message: String): Boolean
    fun listenSyncHouse(message: String): Boolean
    fun listenSyncDeposit(message: String): Boolean
}