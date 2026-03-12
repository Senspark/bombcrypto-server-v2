package com.senspark.game.manager.autoMine

import com.senspark.game.annotation.FunctionTest
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.autoMine.IAutoMineManager
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.data.model.autoMine.IUserAutoMine
import com.senspark.game.data.model.config.AutoMinePackage
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.extension.MainGameExtension
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant
import kotlin.math.max

class UserAutoMineManager(
    private val _mediator: UserControllerMediator,
) : IUserAutoMineManager {

    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val autoMineManager = _mediator.services.get<IAutoMineManager>()
    private val userDataAccess: IUserDataAccess = dataAccessManager.userDataAccess
    private val coinRankingManager = _mediator.svServices.get<ICoinRankingManager>()

    private var userAutoMine: IUserAutoMine? = null

    fun toSfsObject(): SFSObject {
        val result = SFSObject()
        result.putLong("end_time", endAutoMineTime)
        return result
    }

    private fun getUserAutoMine(): IUserAutoMine {
        if (userAutoMine == null) {
            userAutoMine = userDataAccess.loadUserAutoMinePackage(_mediator.userId, _mediator.dataType)
        }
        return userAutoMine!!
    }

    private val endAutoMineTime: Long
        get() = getUserAutoMine().endTime

    private val canStartAutoMating: Boolean
        get() {
            val now = System.currentTimeMillis()
            val m = getUserAutoMine()
            if (m.isNull) {
                return false
            }
            return m.startTime <= now && m.endTime >= now
        }

    override fun startAutoMine(): ISFSObject {
        if (!canStartAutoMating) {
            throw CustomException("Please buy auto mine package first", ErrorCode.INVALID_PARAMETER)
        }
        return toSfsObject()
    }

    @FunctionTest
    override fun buyPackage(autoMinePackage: AutoMinePackage, blockRewardType: BLOCK_REWARD_TYPE): ISFSObject {
        val firstRewardType: String
        val secondRewardType: String
        when (blockRewardType) {
            BLOCK_REWARD_TYPE.BCOIN -> {
                firstRewardType = BLOCK_REWARD_TYPE.BCOIN.name
                secondRewardType = BLOCK_REWARD_TYPE.BCOIN_DEPOSITED.name
            }

            BLOCK_REWARD_TYPE.TON_DEPOSITED -> {
                firstRewardType = BLOCK_REWARD_TYPE.TON_DEPOSITED.name
                secondRewardType = BLOCK_REWARD_TYPE.TON_DEPOSITED.name
            }

            BLOCK_REWARD_TYPE.SOL_DEPOSITED -> {
                firstRewardType = BLOCK_REWARD_TYPE.SOL_DEPOSITED.name
                secondRewardType = BLOCK_REWARD_TYPE.SOL_DEPOSITED.name
            }

            BLOCK_REWARD_TYPE.RON_DEPOSITED -> {
                firstRewardType = BLOCK_REWARD_TYPE.RON_DEPOSITED.name
                secondRewardType = BLOCK_REWARD_TYPE.RON_DEPOSITED.name
            }

            BLOCK_REWARD_TYPE.BAS_DEPOSITED -> {
                firstRewardType = BLOCK_REWARD_TYPE.BAS_DEPOSITED.name
                secondRewardType = BLOCK_REWARD_TYPE.BAS_DEPOSITED.name
            }

            BLOCK_REWARD_TYPE.VIC_DEPOSITED -> {
                firstRewardType = BLOCK_REWARD_TYPE.VIC_DEPOSITED.name
                secondRewardType = BLOCK_REWARD_TYPE.VIC_DEPOSITED.name
            }

//            BLOCK_REWARD_TYPE.SENSPARK -> {
//                firstRewardType = BLOCK_REWARD_TYPE.SENSPARK.name
//                secondRewardType = BLOCK_REWARD_TYPE.SENSPARK_DEPOSITED.name
//            }

            else -> throw CustomException("Reward type invalid", ErrorCode.INVALID_PARAMETER)
        }
        userDataAccess.buyAutoMinePackage(
            _mediator.userId, autoMinePackage, _mediator.dataType, firstRewardType, secondRewardType
        )
        userAutoMine = userDataAccess.loadUserAutoMinePackage(_mediator.userId, _mediator.dataType)
        return toSfsObject()
    }

    override fun packagePrice(): ISFSObject {
        val packages =
            userDataAccess.loadAutoMinePackagePrice(_mediator.userId, autoMineManager.toJsonArray(_mediator.dataType))
        val sfsObject = SFSObject()
        sfsObject.putSFSArray("packages", packages)
        sfsObject.putLong("last_package_end_time", endAutoMineTime)
        return sfsObject
    }

    override fun packagePriceUserAirdrop(dataType: DataType): ISFSObject {
        val packages = autoMineManager.listConfigPackages[dataType]
        val sfsArrayPackage = SFSArray()
        packages!!.forEach {
            val obj = it.toSFSObject()
            obj.putDouble("price", it.minPrice)
            sfsArrayPackage.addSFSObject(obj)
        }
        val sfsObject = SFSObject()
        sfsObject.putSFSArray("packages", sfsArrayPackage)
        sfsObject.putLong("last_package_end_time", endAutoMineTime)
        return sfsObject
    }

    override fun getOfflineReward(heroes: List<Hero>, house: House?): ISFSObject {
        val rewardType = BLOCK_REWARD_TYPE.COIN
        val lastTimeLogOut = _mediator.lastLogOut() ?: return SFSObject()
        val lastLogout = max(lastTimeLogOut.epochSecond, MainGameExtension.TimeInitServer.epochSecond)
        val timeNow = Instant.now().epochSecond
        val maxOfflineTimeWithNoAutoMine = autoMineManager.getMaxOfflineTimeWithNoAutoMine(_mediator.dataType)

        var offlineMinutes = (timeNow - lastLogout).toDouble() / 60
        val enabledAutoMine = (timeNow * 1000) < endAutoMineTime
        if (!enabledAutoMine && (offlineMinutes > maxOfflineTimeWithNoAutoMine)) {
            offlineMinutes = maxOfflineTimeWithNoAutoMine
        }

        val reward =
            autoMineManager.calculateOfflineReward(offlineMinutes, heroes, house, _mediator.dataType, enabledAutoMine)

        if (reward <= 0 || reward.isNaN()) {
            return SFSObject()
        } else if (reward > 0) {
            dataAccessManager.rewardDataAccess.addUserBlockReward(
                _mediator.userId,
                rewardType,
                DataType.TR,
                reward.toFloat(),
                0f,
                ChangeRewardReason.OFFLINE_REWARD_TH_MODE
            )
            coinRankingManager.saveRankingCoin(_mediator.userId, reward.toFloat(), _mediator.dataType)
            dataAccessManager.thModeDataAccess.logOfflineReward(
                _mediator.userId, _mediator.lastLogOut()!!, offlineMinutes, reward, _mediator.dataType
            )
        }

        val result = SFSObject().apply {
            putDouble("time_offline", offlineMinutes)
            putInt("reward_type", rewardType.value)
            putDouble("quantity", reward)
        }
        return result
    }
}