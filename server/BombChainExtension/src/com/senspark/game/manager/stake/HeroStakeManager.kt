package com.senspark.game.manager.stake

import com.senspark.game.api.IRestApi
import com.senspark.game.api.OkHttpRestApi
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.HeroShieldBuilder
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.utils.deserialize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
data class HeroStakeResponse(
    @SerialName("stake_bcoin")
    val stakeBcoin: Double,
    @SerialName("stake_sen")
    val stakeSen: Double
)

class HeroStakeManager(
    private val _envManager: IEnvManager,
    private val _gameDataAccess: IGameDataAccess,
    private val _shopDataAccess: IShopDataAccess,
) : IHeroStakeManager {

    private val _api: IRestApi = OkHttpRestApi()
    private var _minStakeHeroConfig: Map<Int, Int> = mapOf()

    override val minStakeHeroConfig: Map<Int, Int>
        get() = _minStakeHeroConfig

    override fun initialize() {
        _minStakeHeroConfig = _shopDataAccess.loadMinStakeHeroConfig()
    }

    override fun setConfig(minStakeHeroList: Map<Int, Int>) {
        _minStakeHeroConfig = minStakeHeroList
    }

    private fun getHeroStakeAmount(heroId: Int, dataType: EnumConstants.DataType): HeroStakeResponse {
        val url = String.format(_envManager.getBomberStakeUrl, heroId, dataType.name.lowercase())
        val bodyJson: String
        try {
            bodyJson = _api.get(url)
        } catch (ex: Exception) {
            throw CustomException("API fail")
        }

        val message = Json.parseToJsonElement(bodyJson).jsonObject["message"]
            ?: throw CustomException("Invalid response: message not found")

        return deserialize<HeroStakeResponse>(message.toString())
    }

    override fun checkBomberStake(dataType: EnumConstants.DataType, bomber: Hero) {
        val heroStake = getHeroStakeAmount(bomber.heroId, dataType)
        val oldBcoinStake = bomber.stakeBcoin

        // update database
        if (heroStake.stakeBcoin != bomber.stakeBcoin || heroStake.stakeSen != bomber.stakeSen) {
            bomber.stakeBcoin = heroStake.stakeBcoin
            bomber.stakeSen = heroStake.stakeSen
            _gameDataAccess.updateBomberStakeAmount(
                dataType,
                bomber.heroId,
                bomber.type.value,
                heroStake.stakeBcoin,
                heroStake.stakeSen
            )
        }

        if (bomber.isHeroS || heroStake.stakeBcoin == oldBcoinStake) {
            return
        }
        // không phải hero S và có thay đổi bcoin thì kiểm tra để thêm shield
            } else {
                bomber.addShield(shieldInDatabase)
            }
        } else {
            // Stake is insufficient - if they have a shield and are not HeroS, we should remove it
            if (!bomber.isHeroS) {
                // To remove the shield in Senspark pattern, we set it to an empty array in the DB
                // and we can also reset the memory object if needed.
                _gameDataAccess.addShieldToBomber(dataType, bomber.heroId, bomber.type.value, "[]")
                // Note: we might need a way to reset the bomber._shield object to an empty state here
            }
        }
    }

    override fun destroy() {
    }
}