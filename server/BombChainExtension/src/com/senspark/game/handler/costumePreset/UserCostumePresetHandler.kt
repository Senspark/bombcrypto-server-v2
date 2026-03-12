package com.senspark.game.handler.costumePreset

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class CreateOrUpdateCostumePresetHandler : UserCostumePresetHandler(SFSCommand.CU_COSTUME_PRESET_V2)
class BuyCostumePresetSlotHandler : UserCostumePresetHandler(SFSCommand.BUY_COSTUME_PRESET_SLOT_V2)
class GetCostumePresetHandler : UserCostumePresetHandler(SFSCommand.GET_COSTUME_PRESET_V2)

open class UserCostumePresetHandler(
    override val serverCommand: String
) : BaseEncryptRequestHandler() {
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            when (serverCommand) {
                SFSCommand.BUY_COSTUME_PRESET_SLOT_V2 -> {
                    val rewardType = BLOCK_REWARD_TYPE.valueOf(data.getInt("reward_type"))
                    controller.masterUserManager.userConfigManager.buyCostumePresetSlot(rewardType)
                    sendSuccess(controller, requestId, SFSObject())
                }

                SFSCommand.GET_COSTUME_PRESET_V2 -> {
                    val preset = controller.masterUserManager.userCostumePresetManager.toSfsObject()
                    sendSuccess(controller, requestId, SFSObject().apply {
                        putSFSObject("data", preset)
                    })
                }

                SFSCommand.CU_COSTUME_PRESET_V2 -> {
                    val id: String? = if (data.containsKey("id")) data.getUtfString("id") else null
                    val name: String = data.getUtfString("name")
                    val bomberId = data.getInt("bomber_id")
                    val skinIds = data.getIntArray("skin_ids").toList()
                    controller.masterUserManager.userCostumePresetManager.createOrUpdate(
                        id,
                        name,
                        bomberId,
                        skinIds
                    )
                    sendSuccess(controller, requestId, SFSObject())
                }

                else -> throw CustomException("[UserCostumePresetHandler] Command invalid")
            }
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}