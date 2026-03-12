package com.senspark.game.handler.bas

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

class BasDepositResponseHandler(
    private val _logger: ILogger,
    private val _userDataAccess: IUserDataAccess,
    private val _usersManager: IUsersManager,
    private val _coroutineScope: ICoroutineScope,
) {

    private val _json = JsonUtility.json
    private val _tag = "[DEPOSIT_BAS_RESPONSE]"

    @Serializable
    class BasDepositResponse(
        val invoice: String,
        val txHash: String,
        val amount: Double,
        val tokenName: String,
        val sender: String,
    )

    fun parse(data: String): BasDepositResponse {
        return _json.decodeFromString(BasDepositResponse.serializer(), data)
    }

    fun handle(result: String) {
        _coroutineScope.scope.launch(Dispatchers.Default) {
            var controller: IUserController? = null
            try {
                val response = parse(result)
                var userName = ""
                var depositType = EnumConstants.DepositType.BAS_DEPOSIT

                // First, get the invoice and check user online status
                val invoice = if (response.invoice.startsWith("DEP")) {
                    // Check deposit chéo, token nạp vào phải là BAS
                    val isValid = response.tokenName == "BAS"
                    if (!isValid) {
                        _logger.log("$_tag User cheat deposit bas: invoice: ${response.invoice} > amount: ${response.amount} > txHash: ${response.txHash}")
                        return@launch
                    }
                    try {
                        response.invoice.replace("DEP", "").toInt()
                    } catch (e: Exception) {
                        // Deposit Eth ko có invoice number nên cứ ignore, dùng wallet address và txHash để update
                        -1
                    }
                } else {
                    _logger.log("$_tag Invalid invoice: ${response.invoice}")
                    return@launch
                }

                _logger.log("$_tag invoice: $invoice")

                // Get user info early to check online status
                val addressRaw = response.sender
                val address = UserNameSuffix.tryAddNameSuffix(addressRaw, EnumConstants.DataType.BAS).lowercase()
                
                if (address.isNotEmpty() && address.isNotBlank()) {
                    // Check if user is online at the beginning
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
                userName = _userDataAccess.updateBasTransaction(
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

                val userNameSuffix = UserNameSuffix.tryAddNameSuffix(addressRaw, EnumConstants.DataType.BAS).lowercase()
                // Get controller from username for reliability
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
                    controller.sendDataEncryption(HandlerCommand.DepositBasResponse, params)
                }
                _logger.error("$_tag Error when deposit in BAS network: ${e.message} UserName: ${controller?.userInfo?.username}")
            }
        }
    }

    private fun processDepositResponse(
        controller: IUserController,
        response: BasDepositResponse,
        depositType: EnumConstants.DepositType,
        userName: String
    ) {
        try {
            val blockRewardManager = controller.masterUserManager.blockRewardManager
            blockRewardManager.loadUserBlockReward()

            val params = SFSObject()
            params.putInt("code", 0)
            params.putSFSArray("rewards", blockRewardManager.toSfsArrays())
            params.putInt("depositType", depositType.value)

            _logger.log("$_tag User deposit success: $userName, invoice: ${response.invoice} > amount: ${response.amount} > txHash: ${response.txHash}")
            controller.sendDataEncryption(HandlerCommand.DepositBasResponse, params)
        } catch (e: Exception) {
            _logger.error("$_tag Error sending deposit response: ${e.message}")
        }
    }
}
