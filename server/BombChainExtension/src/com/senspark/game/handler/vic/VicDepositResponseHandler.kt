package com.senspark.game.handler.vic

import com.senspark.common.utils.ILogger
import com.senspark.game.declare.UserNameSuffix
import com.senspark.game.controller.IUserController
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.manager.IUsersManager
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.pvp.utility.JsonUtility
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class VicDepositResponseHandler(
    private val _logger: ILogger,
    private val _userDataAccess: IUserDataAccess,
    private val _usersManager: IUsersManager,
    private val _coroutineScope: ICoroutineScope,
) {
    private val _json = JsonUtility.json
    private val _tag = "[DEPOSIT_VIC_RESPONSE]"

    fun handle(result: String) {
        _coroutineScope.scope.launch(Dispatchers.Default) {
            var controller: IUserController? = null
            try {
                val response = parse(result)
                var userName = ""
                var depositType = EnumConstants.DepositType.VIC_DEPOSIT

                // First, get the invoice and check user online status
                val invoice = if (response.invoice.startsWith("DEP")) {
                    // Check deposit chéo, token nạp vào phải là VIC
                    val isValid = response.tokenName == "VIC"
                    if (!isValid) {
                        _logger.log("$_tag User cheat deposit vic: amount: ${'$'}{response.amount} > txHash: ${'$'}{response.txHash}")
                        return@launch
                    }
                    try {
                        response.invoice.replace("DEP", "").toInt()
                    } catch (e: Exception) {
                        -1
                    }
                } else {
                    -1
                }

                // Get user info early to check online status
                val addressRaw = response.sender
                val address = UserNameSuffix.tryAddNameSuffix(addressRaw, EnumConstants.DataType.VIC).lowercase()
                if (address.isNotEmpty() && address.isNotBlank()) {
                    controller = _usersManager.getUserController(address)
                    if (controller == null) {
                        _logger.log("$_tag User is offline, waiting 2 seconds: uid $address")
                        delay(2000)
                    }
                } else {
                    _logger.error("$_tag Missing sender data in response: ${'$'}{response.sender}")
                    return@launch
                }

                userName = _userDataAccess.updateVicTransaction(
                    invoice,
                    response.amount,
                    response.txHash,
                    response.tokenName,
                    address,
                )

                if (userName == "") {
                    _logger.log("$_tag Error when deposit: User not found")
                    return@launch
                }

                val userNameSuffix = UserNameSuffix.tryAddNameSuffix(addressRaw, EnumConstants.DataType.VIC).lowercase()
                controller = _usersManager.getUserController(userNameSuffix)

                if (controller != null) {
                    processDepositResponse(controller, response, depositType, userNameSuffix)
                } else {
                    _logger.log("$_tag Error when deposit: User controller is null")
                }

            } catch (ex: Exception) {
                _logger.log("$_tag Exception: ${'$'}ex")
            }
        }
    }

    private fun processDepositResponse(
        controller: IUserController,
        response: DepositResponse,
        depositType: EnumConstants.DepositType,
        userName: String
    ) {
        val blockRewardManager = controller.masterUserManager.blockRewardManager
        blockRewardManager.loadUserBlockReward()

        val params = SFSObject()
        params.putInt("code", 0)
        params.putSFSArray("rewards", blockRewardManager.toSfsArrays())
        params.putInt("depositType", depositType.value)
        _logger.log("$_tag userName: $userName > amount: ${'$'}{response.amount} > txHash: ${'$'}{response.txHash}")
        controller.sendDataEncryption(HandlerCommand.DepositVicResponse, params)
    }

    private fun parse(json: String): DepositResponse {
        return _json.decodeFromString(DepositResponse.serializer(), json)
    }

    @Serializable
    data class DepositResponse(
        val invoice: String,
        val tokenName: String,
        val amount: Double,
        val txHash: String,
        val sender: String
    )
}
