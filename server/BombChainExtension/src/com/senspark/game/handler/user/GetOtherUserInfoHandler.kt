package com.senspark.game.handler.user

import com.senspark.game.constant.ItemType
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.pvp.IPvpRankingManager
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.user.SkinChest
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant

class GetOtherUserInfoHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_OTHER_USER_INFO_V2

    private val _userDataAccess = services.get<IUserDataAccess>()
    private val _gameDataAccess = services.get<IGameDataAccess>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            val userId = data.getInt("user_id")
            val userName = data.getUtfString("user_name")

            if (userId <= 0) {
                throw Exception("User not found")
            }

            // Get PVP ranking information of the user
            val pvpRankingManager = controller.svServices.get<IPvpRankingManager>()
            val pvpRank = pvpRankingManager.getRanking(userName, userId, controller.dataType)

            // Get equipped items of the user
            val sfsArray = SFSArray()
            _userDataAccess.getUserInventory(userId)
                .values
                .flatten()
                .map { SkinChest(it) }
                .filter { it.expiryDate == null || it.expiryDate!! >= Instant.now() }
                .filter {
                    it.active &&
                        (it.type == ItemType.BOMB || it.type == ItemType.FIRE || it.type == ItemType.TRAIL || it.type == ItemType.WING)
                }
                .forEach {
                    sfsArray.addSFSObject(SFSObject().apply {
                        putInt("id", it.id)
                        putInt("item_id", it.itemId)
                        putInt("type", it.type.value)
                    })
                }

            // Get hero information the user is using
            val lastHero = _gameDataAccess.queryPvP(userId).lastPlayedHero
            val heroSelected = if (lastHero == -1L) SFSObject() else _userDataAccess.getHeroPvp(userId, lastHero)

            response.putSFSObject("rank", pvpRank.toSFSObject())
            response.putSFSArray("equip_items", sfsArray)
            if (heroSelected.size() > 0) {
                response.putSFSObject("hero", heroSelected)
            }

        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
        sendSuccess(controller, requestId, response)
    }
}