package com.senspark.game.data.manager.hero

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.ResetShieldBomber
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException

interface IResetShieldBomberManager : IServerService {
    fun getFinalDamage(rare: Int): Int
    fun get(rare: Int): ResetShieldBomber
    fun set(value: Map<Int, ResetShieldBomber>)
}

class ResetShieldBomberManager(
    private val _shopDataAccess: IShopDataAccess,
) : IResetShieldBomberManager {

    private val _value: MutableMap<Int, ResetShieldBomber> = mutableMapOf()

    override fun initialize() {
        _value.putAll(_shopDataAccess.loadResetShieldBomber())
        set(_value)
    }

    private var resetShieldBomber: Map<Int, ResetShieldBomber> = emptyMap()

    override fun getFinalDamage(rare: Int): Int {
        val resetShieldBomber = get(rare)
        return resetShieldBomber.getFinalDamage()
    }

    override fun get(rare: Int): ResetShieldBomber {
        return resetShieldBomber[rare] ?: throw CustomException(
            "shield not found", ErrorCode.SERVER_ERROR
        )
    }

    override fun set(value: Map<Int, ResetShieldBomber>) {
        resetShieldBomber = value
    }
}