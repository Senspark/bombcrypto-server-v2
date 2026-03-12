package com.senspark.game.data.model.user

import com.senspark.common.constant.ItemId
import com.senspark.common.pvp.IRankManager
import com.senspark.common.utils.toSFSArray
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.user.UserPoint
import com.senspark.game.utils.deserializeList
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet


interface IUserPvpProfile {
    fun addHero(rs: ResultSet, heroTraditionalConfig: IConfigHeroTraditionalManager)
    fun toSfsObject(): ISFSObject
}

class UserPvpProfile(
    val heroes: MutableList<Hero>,
    val rank: PvPRank,
    val itemIds: List<ItemId>,
    val activeItemIds: List<ItemId>
) : IUserPvpProfile {
    companion object {
        fun fromResultSet(
            rs: ResultSet,
            heroTraditionalConfig: IConfigHeroTraditionalManager,
            rankManager: IRankManager,
            userDataAccess: IUserDataAccess
        ): IUserPvpProfile {
            val heroes = mutableListOf(heroTraditionalConfig.createHero(SFSObject.newFromResultSet(rs)))
            val rank = PvPRank(
                rs.getString("user_name"),
                rs.getInt("uid"),
                rs.getInt("rank"),
                UserPoint(rs.getInt("point")),
                rs.getInt("total_match"),
                rs.getInt("win_match"),
                -1,
                rankManager
            )
            //Thêm avatar vào thông tin pvp
            rank.avatar = userDataAccess.queryUserAvatarActive(rank.uid)
            return UserPvpProfile(
                heroes,
                rank,
                if (rs.getString("item_ids").isNullOrEmpty()) emptyList()
                else deserializeList(rs.getString("item_ids")),
                if (rs.getString("active_item_ids").isNullOrEmpty()) emptyList()
                else deserializeList(rs.getString("active_item_ids")),
            )
        }
    }

    override fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putSFSArray("heroes", heroes.toSFSArray { it.toSFSObject() })
            putSFSObject("rank", rank.toSFSObject())
            putIntArray("item_ids", itemIds)
            putIntArray("active_item_ids", activeItemIds)
        }
    }

    override fun addHero(rs: ResultSet, heroTraditionalConfig: IConfigHeroTraditionalManager) {
        heroes.add(heroTraditionalConfig.createHero(SFSObject.newFromResultSet(rs)))
    }
}