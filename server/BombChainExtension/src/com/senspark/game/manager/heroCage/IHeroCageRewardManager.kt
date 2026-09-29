package com.senspark.game.manager.heroCage

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants.DataType

/**
 * Bonus BHero Cage for a match winner (PvP, Adventure). The server rolls, the player then picks the
 * network within 5 minutes; a later offer replaces an unanswered one.
 */
interface IHeroCageRewardManager : IGlobalService {
    /** @return true if the player won an offer that must now be claimed with [claim]. */
    fun roll(userInfo: IUserInfo): Boolean

    /** Consumes the pending offer and credits 1 BOMBERMAN on [network]. Throws if there is none. */
    fun claim(uid: Int, network: DataType)
}
