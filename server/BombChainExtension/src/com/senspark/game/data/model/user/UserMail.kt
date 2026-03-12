package com.senspark.game.data.model.user

import com.senspark.common.utils.toSFSArray
import com.senspark.game.utils.deserializeList
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class UserMail(
    val id: String,
    val title: String,
    val message: String,
    var isRead: Boolean,
    var isClaim: Boolean,
    attachItemsJson: String
) {

    val attachItems: List<UserMailItem> = deserializeList(attachItemsJson)

    companion object {
        fun fromResultSet(rs: ResultSet): UserMail {
            return UserMail(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getBoolean("is_read"),
                rs.getBoolean("is_claim"),
                rs.getString("attach_items")
            )
        }
    }

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putUtfString("id", id)
            putUtfString("title", title)
            putUtfString("message", message)
            putBool("is_read", isRead)
            putBool("is_claim", isClaim)
            putSFSArray("attach_items", attachItems.toSFSArray { it.toSfsObject() })
        }
    }
}