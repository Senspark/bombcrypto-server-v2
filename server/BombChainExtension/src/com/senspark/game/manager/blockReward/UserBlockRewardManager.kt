package com.senspark.game.manager.blockReward

import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.data.model.user.UserBlockReward
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.util.*
import kotlin.math.abs

class UserBlockRewardManager(
    private val _mediator: UserControllerMediator,
) : IUserBlockRewardManager {

    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val gameConfigManager = _mediator.services.get<IGameConfigManager>()

    /**
     * WARNING: should not use directly, use getRewardsHaving() instead
     */
    private lateinit var _rewardsHaving: MutableMap<BLOCK_REWARD_TYPE, MutableMap<DataType, UserBlockReward>>
    
    private val _rewardsMining = EnumMap<BLOCK_REWARD_TYPE, RewardDetail>(BLOCK_REWARD_TYPE::class.java)

    /**
     * Chứa các reward đào theo network hiện chỉ có COIN network
     * Được lưu ở đây và đc cộng vào database khi save game xong sẽ clear để tích lại từ đầu
     * Do cách lưu cũ chỉ support đc 1 user 1 loại coin thôi nên ko để sửa quá nhiều nên tạo thêm map này để lưu thêm coin có network
     */
    private val _rewardsFiMining = EnumMap<DataType, MutableMap<BLOCK_REWARD_TYPE, RewardDetail>>(DataType::class.java)

    private fun getRewardsHaving(): MutableMap<BLOCK_REWARD_TYPE, MutableMap<DataType, UserBlockReward>> {
        if (!::_rewardsHaving.isInitialized) {
            _rewardsHaving = dataAccessManager.gameDataAccess.loadUserBlockReward(_mediator.userId)
        }
        return _rewardsHaving
    }

    override fun getRewardsMining(): MutableMap<BLOCK_REWARD_TYPE, RewardDetail> {
        return _rewardsMining
    }

    override fun getRewardMining(blockRewardType: BLOCK_REWARD_TYPE): Float {
        return _rewardsMining[blockRewardType]?.value ?: 0f
    }

    override fun getRewardsMiningFi(): MutableMap<DataType, MutableMap<BLOCK_REWARD_TYPE, RewardDetail>> {
        return _rewardsFiMining
    }

    override fun getRewardMiningFi(
        blockRewardType: BLOCK_REWARD_TYPE,
        dataType: DataType
    ): Float {
        return _rewardsFiMining[dataType]?.get(blockRewardType)?.value ?: 0f
    }

    override fun addRewards(rewards: Map<BLOCK_REWARD_TYPE, RewardDetail>) {
        rewards.forEach {
            addMiningRewards(it.value)
        }
    }

    override fun list(): List<UserBlockReward> {
        return getRewardsHaving().map { it.value.map { it2 -> it2.value } }.flatten()
    }

    override fun get(blockRewardType: BLOCK_REWARD_TYPE): UserBlockReward? {
        val dataType = blockRewardType.getDataType(_mediator.dataType)
        return getRewardsHaving()[blockRewardType]?.get(dataType)
    }

    override fun getByNetwork(blockRewardType: BLOCK_REWARD_TYPE): UserBlockReward? {
        val dataType = _mediator.dataType
        return getRewardsHaving()[blockRewardType]?.get(dataType)
    }

    override fun addReward(rewardDetail: RewardDetail, dtype: DataType) {
        _mediator.saveLater(SAVE.REWARD)
        val type = rewardDetail.blockRewardType

        val dataType = if(dtype != DataType.UNKNOWN) dtype else type.getDataType(_mediator.dataType)
        val rewardHaving = getRewardsHaving()
        
        if (!rewardHaving.containsKey(type)) {
            rewardHaving[type] = EnumMap(DataType::class.java)
        }
        // Chest
        if (!rewardHaving[type]!!.containsKey(dataType)) {
            val uReward = UserBlockReward(type)
            rewardHaving[type]!![dataType] = uReward
        }
        val uReward = get(type)
        uReward!!.addValues(rewardDetail.value)

        // Mining
        if (!_rewardsMining.containsKey(type)) {
            _rewardsMining[type] = RewardDetail(type, rewardDetail.mode, dataType, 0f)
        }
        val curRw = _rewardsMining[type]
        curRw!!.addValue(rewardDetail.value)
        _rewardsMining[type] = curRw
    }


    // Dùng để cộng COIN đào đc trong TH mode vì user airdrop sẽ đc cộng đồng thời cả COIN TR và COIN network
    override fun addMiningRewards(rewardDetail: RewardDetail) {
        // Cộng coin kiểu TR như bình thường
        addReward(rewardDetail)

        if(rewardDetail.blockRewardType == BLOCK_REWARD_TYPE.COIN && _mediator.dataType.isEthereumAirdropUser())
        {
            // Nếu user này là ron, bas, vic thì cộng thêm COIN kiểu network đó nữa
            val type = rewardDetail.blockRewardType

            val rewardHaving = getRewardsHaving()

            if (!rewardHaving.containsKey(type)) {
                rewardHaving[type] = EnumMap(DataType::class.java)
            }
            // Chest
            if (!rewardHaving[type]!!.containsKey(_mediator.dataType)) {
                val uReward = UserBlockReward(type)
                rewardHaving[type]!![_mediator.dataType] = uReward
            }
            val uReward = getByNetwork(type)
            uReward!!.addValues(rewardDetail.value)

            if (!_rewardsFiMining.containsKey(_mediator.dataType)) {
                _rewardsFiMining[_mediator.dataType] = EnumMap(BLOCK_REWARD_TYPE::class.java)
            }
            if (!_rewardsFiMining[_mediator.dataType]!!.containsKey(type)) {
                _rewardsFiMining[_mediator.dataType]!![type] = RewardDetail(type, rewardDetail.mode, _mediator.dataType, 0f).OverrideDataType(_mediator.dataType)
            }
            val curRw = _rewardsFiMining[_mediator.dataType]!![type]
            curRw!!.addValue(rewardDetail.value)
            _rewardsFiMining[_mediator.dataType]!![type] = curRw
        }


    }

    override fun onSaved() {
        _rewardsMining.clear()
        _rewardsFiMining.clear()
    }

    override fun toSfsArrays(): ISFSArray {
        val sfsArr: ISFSArray = SFSArray()
        val listReward = list()
        val nextTimeCanClaimReward = gameConfigManager.nextTimeCanClaimReward
        for (userBlockReward in listReward) {
            var forControlValue = 0f
            if (_rewardsMining.containsKey(userBlockReward.rewardType)) {
                forControlValue = _rewardsMining[userBlockReward.rewardType]!!.forControlValue
            }
            val rw: ISFSObject = SFSObject()
            rw.putUtfString(SFSField.Type, userBlockReward.rewardType.name)
            rw.putUtfString(SFSField.DataType, userBlockReward.dataType.name)
            rw.putFloat(SFSField.Value, roundVerySmallNumber(userBlockReward.values + forControlValue))
            rw.putInt(SFSField.RemainTime, userBlockReward.getRemainTimeCanClaim(nextTimeCanClaimReward))
            rw.putDouble("claimPending", userBlockReward.claimPending)
            sfsArr.addSFSObject(rw)
        }
        return sfsArr
    }

    private fun roundVerySmallNumber(value: Float): Float {
        return if (abs(value) < 0.000001) 0f else value
    }

    override fun getTotalBcoinHaving(): Float {
        val bcoinHaving: Float = get(BLOCK_REWARD_TYPE.BCOIN)?.values ?: 0f
        val bcoinDepositHaving: Float = get(BLOCK_REWARD_TYPE.BCOIN_DEPOSITED)?.values ?: 0f
        return bcoinHaving + bcoinDepositHaving
    }

    override fun getClaimFeeHaving(blockRewardType: BLOCK_REWARD_TYPE): Float {
        return getTotalBcoinHaving()
    }

    override fun loadUserBlockReward() {
        val mapReward = dataAccessManager.gameDataAccess.loadUserBlockReward(_mediator.userId)
        getRewardsHaving().clear()
        getRewardsHaving().putAll(mapReward)
    }

    override fun getRewardValue(rewardType: BLOCK_REWARD_TYPE): Float {
        val dataType = rewardType.getDataType(_mediator.dataType)
        return getRewardValue(rewardType, dataType)
    }

    override fun destroy() {}
    override fun getRewardValue(rewardType: BLOCK_REWARD_TYPE, dataType: DataType): Float {
        return getRewardsHaving()[rewardType]?.get(dataType)?.values ?: 0f
    }

    override fun getTotalGemHaving(): Float {
        val gem = getRewardValue(BLOCK_REWARD_TYPE.GEM, DataType.TR)
        val gemLock = getRewardValue(BLOCK_REWARD_TYPE.GEM_LOCKED, DataType.TR)
        return gem + gemLock
    }

    override fun getTotalGoldHaving(): Float {
        return getRewardValue(BLOCK_REWARD_TYPE.GOLD, DataType.TR)
    }


    // Always COIN TR
    override fun getTotalCoinHaving(): Float {
        return getRewardValue(BLOCK_REWARD_TYPE.COIN, DataType.TR)
    }

    // COIN network
    // Star core của RON và BAS sẽ có network type là RON và BAS để phân biệt với network bsc và polygon do dùng chung ví và client
    override fun getTotalCoinFiHaving(dataType: DataType): Float {
        if(dataType.isEthereumAirdropUser()) {
            return getRewardValue(BLOCK_REWARD_TYPE.COIN, dataType)
        }
        return getTotalCoinHaving()
    }

    override fun getTotalRockHaving(): Float {
        return getRewardValue(BLOCK_REWARD_TYPE.ROCK, DataType.TR)
    }

    override fun getTotalTonDepositHaving(): Float {
        return getRewardValue(BLOCK_REWARD_TYPE.TON_DEPOSITED, DataType.TON)
    }

    override fun getTotalSolDepositHaving(): Float {
        return getRewardValue(BLOCK_REWARD_TYPE.SOL_DEPOSITED, DataType.SOL)
    }

    override fun getTotalRonDepositHaving(): Float {
        return getRewardValue(BLOCK_REWARD_TYPE.RON_DEPOSITED, DataType.RON)
    }

    override fun getTotalBasDepositHaving(): Float {
        return getRewardValue(BLOCK_REWARD_TYPE.BAS_DEPOSITED, DataType.BAS)
    }

    override fun getTotalVicDepositHaving(): Float {
        return getRewardValue(BLOCK_REWARD_TYPE.VIC_DEPOSITED, DataType.VIC)
    }

    override fun checkEnoughReward(value: Float, rewardType: BLOCK_REWARD_TYPE, isFiReward: Boolean) {
        when (rewardType) {
            BLOCK_REWARD_TYPE.GOLD -> {
                if (value > getTotalGoldHaving()) {
                    throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.GOLD.name}")
                }
            }
            BLOCK_REWARD_TYPE.GEM -> {
                if (value > getTotalGemHaving()) {
                    throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.GEM.name}")
                }
            }
            BLOCK_REWARD_TYPE.COIN -> {
                if(isFiReward) {
                    if (value > getTotalCoinFiHaving(_mediator.dataType)) {
                        throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.COIN.name} FI")
                    }
                }
                else {
                    if (value > getTotalCoinHaving()) {
                        throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.COIN.name} TR")
                    }
                }
            }
            BLOCK_REWARD_TYPE.ROCK -> {
                if (value > getTotalRockHaving()) {
                    throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.ROCK.name}")
                }
            }
            BLOCK_REWARD_TYPE.TON_DEPOSITED -> {
                if (value > getTotalTonDepositHaving()) {
                    throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.TON_DEPOSITED.name}")
                }
            }
            BLOCK_REWARD_TYPE.SOL_DEPOSITED -> {
                if (value > getTotalSolDepositHaving()) {
                    throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.SOL_DEPOSITED.name}")
                }
            }
            BLOCK_REWARD_TYPE.RON_DEPOSITED -> {
                if (value > getTotalRonDepositHaving()) {
                    throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.RON_DEPOSITED.name}")
                }
            }
            BLOCK_REWARD_TYPE.BAS_DEPOSITED -> {
                if (value > getTotalBasDepositHaving()) {
                    throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.BAS_DEPOSITED.name}")
                }
            }
            BLOCK_REWARD_TYPE.VIC_DEPOSITED -> {
                if (value > getTotalVicDepositHaving()) {
                    throw CustomException("Not enough $value ${BLOCK_REWARD_TYPE.VIC_DEPOSITED.name}")
                }
            }
            else -> throw CustomException("fun checkEnoughReward ::TODO ${rewardType.name}")
        }
    }
}