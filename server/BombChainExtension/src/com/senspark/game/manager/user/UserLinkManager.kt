package com.senspark.game.manager.user

import com.senspark.game.schema.TableUser
import com.senspark.game.schema.TableUserLink
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class UserLinkManager : IUserLinkManager {

    override fun initialize() {
    }
    
    override fun link(userId: Int, linkedUserId: Int) {
        require(userId != linkedUserId) { "Cannot link to the same user ID" }
        transaction {
            val userExisted = TableUser
                .select { TableUser.userId eq linkedUserId }
                .count() > 0
            require(!userExisted) { "Account $linkedUserId already existed" }
            val existedUserIds = TableUserLink
                .select { TableUserLink.userId eq userId }
                .map { it[TableUserLink.linkedUserId] }
            require(existedUserIds.isEmpty()) { "Account $userId already linked to ${existedUserIds[0]}" }
            val existedLinkedUserIds = TableUserLink
                .select { TableUserLink.linkedUserId eq userId }
                .map { it[TableUserLink.userId] }
            require(existedLinkedUserIds.isEmpty()) { "Account $userId already linked to ${existedLinkedUserIds[0]}" }
            TableUserLink.insert {
                it[TableUserLink.userId] = userId
                it[TableUserLink.linkedUserId] = linkedUserId
            }
        }
    }

    override fun getLinkedUserId(userId: Int): List<Int> {
        val items = mutableListOf<Int>()
        transaction {
            val result = TableUserLink.select {
                TableUserLink.userId eq userId
            }
            items.addAll(result.map { it[TableUserLink.linkedUserId] })
        }
        return items
    }

    override fun getLinkedToUserId(userId: Int): Int {
        val items = mutableListOf<Int>()
        transaction {
            val result = TableUserLink.select {
                TableUserLink.linkedUserId eq userId
            }.forUpdate()
            items.addAll(result.map { it[TableUserLink.userId] })
        }
        if (items.isEmpty()) {
            // Link to own user ID.
            return userId
        }
        require(items.size == 1) { "Account $userId linked to multiple user IDs: $items" }
        return items[0]
    }
}