package com.senspark.game.handler.ron

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

class RonDepositResponseHandler(
    private val _logger: ILogger,
    private val _userDataAccess: IUserDataAccess,
    private val _usersManager: IUsersManager,
    private val _coroutineScope: ICoroutineScope,
) {

    private val _json = JsonUtility.json
    private val _tag = "[DEPOSIT_RON_RESPONSE]"

    fun handle(result: String) {
        _coroutineScope.scope.launch(Dispatchers.Default) {
            var controller: IUserController? = null
            try {
                val response = parse(result)
                var userName = ""
                var depositType = EnumConstants.DepositType.RON_DEPOSIT

                // First, get the invoice and check user online status
                val invoice = if (response.invoice.startsWith("DEP")) {
                    // Check deposit chéo, token nạp vào phải là RON
                    val isValid = response.tokenName == "RON"
                    if (!isValid) {
                        _logger.log("$_tag User cheat deposit ron: amount: ${response.amount} > txHash: ${response.txHash}")
                        return@launch
                    }
                    try {
                        response.invoice.replace("DEP", "").toInt()
                    } catch (e: Exception) {
                        // Deposit Ron ko có invoice number nên cứ ignore, dùng wallet address và txHash để update
                        -1
                    }
                } else {
                    _logger.log("$_tag Invalid invoice: ${response.invoice}")
                    return@launch
                }

                // Get user info early to check online status
                val addressRaw = response.sender
                val address = UserNameSuffix.tryAddNameSuffix(addressRaw, EnumConstants.DataType.RON).lowercase()
                if (address.isNotEmpty() && address.isNotBlank()) {                    // Check if user is online at the beginning
                    controller = _usersManager.getUserController(address)
                    if (controller == null) {
                        _logger.log("$_tag User is offline, waiting 2 seconds: uid $address")
                        delay(2000) // Wait 2 seconds then proceed normally
                    }
                } else {
                    _logger.error("$_tag Missing sender data in response: ${response.sender}")
                    // Ron và Base update deposit bằng sender address nên nếu miss ở đây là lỗi rồi
                    // ko thể update đc nên sẽ dừng ở đây và cũng ko biết user nào để thông báo
                    return@launch
                }

                // Now proceed with deposit validation and database update
                userName = _userDataAccess.updateRonTransaction(
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

                // Lấy lại controller từ userName này sẽ đảm bảo hơn
                val userNameSuffix = UserNameSuffix.tryAddNameSuffix(addressRaw, EnumConstants.DataType.RON).lowercase()
                controller = _usersManager.getUserController(userNameSuffix);

                // Process the deposit response
                if (controller != null) {
                    processDepositResponse(controller, response, depositType, userNameSuffix)
                } else {
                    _logger.log("$_tag Error when deposit: User controller is null")
                }
            } catch (e: Exception) {
                if (controller != null) {
                    val params = SFSObject()
                    params.putInt("code", 100)
                    params.putUtfString("message", e.message)
                    controller.sendDataEncryption(HandlerCommand.DepositRonResponse, params)
                }
                _logger.error("$_tag Error when deposit in RON network: ${e.message} UserName: ${controller?.userInfo?.username}")
            }
        }
    }

    private fun processDepositResponse(
        controller: IUserController,
        response: DepositRonResponse,
        depositType: EnumConstants.DepositType,
        userName: String
    ) {
        val blockRewardManager = controller.masterUserManager.blockRewardManager
        blockRewardManager.loadUserBlockReward()

        val params = SFSObject()
        params.putInt("code", 0)
        params.putSFSArray("rewards", blockRewardManager.toSfsArrays())
        params.putInt("depositType", depositType.value)
        _logger.log("$_tag userName: $userName > amount: ${response.amount} > txHash: ${response.txHash}")
        controller.sendDataEncryption(HandlerCommand.DepositRonResponse, params)
    }

    @Serializable
    class DepositRonResponse(
        val invoice: String,
        val amount: Double,
        val txHash: String,
        val sender: String,
        val tokenName: String,
    )

    fun parse(data: String): DepositRonResponse {
        val response = _json.decodeFromString<DepositRonResponse>(data)
        return response
    }
}
