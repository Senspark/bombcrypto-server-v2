package com.senspark.game.db.statement

interface IMarketPlace {
    fun statementUpdateDashboard(
        userName: String?,
        id: Int,
        status: Int,
        price: Float?,
        rewardType: Int?
    ): Pair<String, Array<Any?>>

    fun statementUpdateMarketPlace(
        uid: Int,
        id: Int,
        type: Int,
        itemId: Int,
        price: Float?,
        rewardType: Int,
        status: Int
    ): Pair<String, Array<Any?>>

    fun statementUpdateUserReward(uid: Int, name: String, values: Float): Pair<String, Array<Any?>>
    fun statementInsertActivity(
        uid: Int,
        id: Int,
        activity: String,
        type: Int,
        itemId: Int,
        price: Float?,
        rewardType: Int
    ): Pair<String, Array<Any?>>

}