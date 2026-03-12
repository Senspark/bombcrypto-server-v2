package com.senspark.game.handler.ton

import com.senspark.common.utils.ILogger
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

class TonDepositResponseHandler(
    private val _logger: ILogger,
    private val _userDataAccess: IUserDataAccess,
    private val _usersManager: IUsersManager,
    private val _coroutineScope: ICoroutineScope,
) {

    private val _json = JsonUtility.json
    private val _tag = "[DEPOSIT_TON_RESPONSE]"

    fun handle(result: String) {
        _coroutineScope.scope.launch(Dispatchers.Default) {
            var controller: IUserController? = null
            try {
                val response = parse(result)
                var userName = ""
                var depositType = EnumConstants.DepositType.TON_DEPOSIT

                // First, get the invoice and check user online status
                val invoice = if (response.invoice.startsWith("DEP")) {
                    // Check deposit chéo, token nạp vào phải là TON
                    val isValid = response.tokenName == "TON"
                    if (!isValid) {
                        _logger.log("$_tag User cheat deposit ton: invoice: ${response.invoice} > amount: ${response.amount} > txHash: ${response.txHash}")
                        return@launch
                    }
                    try {
                        response.invoice.replace("DEP", "").toInt()
                    } catch (e: Exception) {
                        _logger.error("$_tag Invalid DEP invoice format: ${response.invoice}, error: ${e.message}")
                        return@launch
                    }
                } else if (response.invoice.startsWith("BCD")) {
                    // Check deposit chéo, token nạp vào phải là BCOIN
                    val isValid = response.tokenName == "BCOIN"
                    if (!isValid) {
                        _logger.log("$_tag User cheat deposit bcoin: invoice: ${response.invoice} > amount: ${response.amount} > txHash: ${response.txHash}")
                        return@launch
                    }
                    try {
                        response.invoice.replace("BCD", "").toInt()
                    } catch (e: Exception) {
                        _logger.error("$_tag Invalid BCD invoice format: ${response.invoice}, error: ${e.message}")
                        return@launch
                    }
                } else {
                    _logger.log("$_tag Invalid invoice: ${response.invoice}")
                    return@launch
                }

                _logger.log("$_tag invoice: $invoice")

                // Get user info early to check online status
                val address = response.sender.lowercase()
                if (address.isNotEmpty() && address.isNotBlank()) {
                    // Check if user is online at the beginning
                    controller = _usersManager.getUserController(address)
                    if (controller == null) {
                        _logger.log("$_tag User is offline, waiting 2 seconds: uid $address")
                        delay(2000) // Wait 2 seconds then proceed normally
                    }
                } else {
                    _logger.log("$_tag Missing sender data in response: ${response.sender}")
                    // Sol và Ton update deposit bằng invoice number nên bước này chỉ để đảm bảo 2 server ko ghi lẫn nhau
                    // Nếu ko có sender thì vẫn cứ delay 2s rồi process như bth
                    delay(2000)
                }

                // Now proceed with deposit validation and database update
                if (response.invoice.startsWith("DEP")) {
                    userName = _userDataAccess.updateTonTransaction(
                        invoice,
                        response.amount,
                        response.txHash,
                        response.tokenName
                    )
                    depositType = EnumConstants.DepositType.TON_DEPOSIT
                } else if (response.invoice.startsWith("BCD")) {
                    userName = _userDataAccess.updateBcoinTonTransaction(
                        invoice,
                        response.amount,
                        response.txHash,
                        response.tokenName
                    )
                    depositType = EnumConstants.DepositType.BCOIN_DEPOSIT
                }

                if (userName == "") {
                    _logger.log("$_tag Error when deposit: User not found")
                    return@launch
                }

                // Lấy lại controller từ userName này sẽ đảm bảo hơn
                controller = _usersManager.getUserController(userName);

                // Process the deposit response
                if (controller != null) {
                    processDepositResponse(controller, response, depositType, userName)
                } else {
                    _logger.log("$_tag Error when deposit: User controller is null")
                }
            } catch (e: Exception) {
                if (controller != null) {
                    val params = SFSObject()
                    params.putInt("code", 100)
                    params.putUtfString("message", e.message)
                    controller.sendDataEncryption(HandlerCommand.DepositTonResponse, params)
                }
                _logger.error("$_tag Error when deposit in TON network: ${e.message} UserName: ${controller?.userInfo?.username}")
            }
        }
    }

    private fun processDepositResponse(
        controller: IUserController,
        response: DepositTonResponse,
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
        controller.sendDataEncryption(HandlerCommand.DepositTonResponse, params)
    }

    @Serializable
    class DepositTonResponse(
        val invoice: String,
        val amount: Double,
        val txHash: String,
        val sender: String,
        val tokenName: String,
    )

    fun parse(data: String): DepositTonResponse {
        val response = _json.decodeFromString<DepositTonResponse>(data)
        return response
    }
}