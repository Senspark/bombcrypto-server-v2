package com.senspark.game.data.model.user

import com.senspark.game.utils.deserializeList
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

interface IUserCostumePreset {
    val id: String
    val uid: Int
    var name: String
    var originalName: String
    var bomberId: Int
    var skinIds: List<Int>
    fun toSfsObject(
        isHavingHero: (heroId: Int) -> Boolean,
        isHavingSkin: (skinId: Int) -> Boolean,
    ): ISFSObject

    fun idSameNameWithOther(id: String? = null, other: String): Boolean
}

class UserCostumePreset(
    override val id: String,
    override val uid: Int,
    override var name: String,
    override var originalName: String,
    override var bomberId: Int,
    override var skinIds: List<Int>
) : IUserCostumePreset {

    constructor(
        id: String,
        uid: Int,
        name: String,
        originalName: String,
        bomberId: Int,
        skinIds: String
    ) : this(
        id,
        uid,
        name,
        originalName,
        bomberId,
        deserializeList(skinIds)
    )

    companion object {
        fun fromResultSet(rs: ResultSet): IUserCostumePreset {
            return UserCostumePreset(
                rs.getString("id"),
                rs.getInt("uid"),
                rs.getString("name"),
                rs.getString("original_name"),
                rs.getInt("bomber_id"),
                rs.getString("skin_ids")
            )
        }
    }

    override fun idSameNameWithOther(id: String?, other: String): Boolean {
        return id == null && originalName.equals(other, ignoreCase = true)
            || id != this.id && originalName.equals(other, ignoreCase = true)
    }

    override fun toSfsObject(
        isHavingHero: (heroId: Int) -> Boolean,
        isHavingSkin: (skinId: Int) -> Boolean
    ): ISFSObject {
        return SFSObject().apply {
            putUtfString("name", name)
            if (isHavingHero(bomberId)) {
                putInt("bomber_id", bomberId)
            }
            putIntArray("skin_ids", skinIds.filter(isHavingSkin))
        }
    }
}