package com.senspark.game.handler.moderator

import com.senspark.common.utils.IGlobalLogger
import com.senspark.game.extension.GlobalServices
import com.senspark.game.utils.JsonExtensionBuilder
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.extensions.SFSExtension
import kotlinx.serialization.*

class AdminCommandStreamProcessor(
    zone: Zone,
    extension: SFSExtension,
    services: GlobalServices,
) {
    private val _controller: AdminCommandController = AdminCommandController(services)
    private val _logger = services.get<IGlobalLogger>()

    init {
        _controller.init(zone, extension)
    }

    fun process(data: String) {
        try {
            _logger.log("Process admin command: $data")
            val cmdData = JsonExtensionBuilder.json.decodeFromString<CmdData>(data)
            when (cmdData.cmd) {
                ADMIN_COMMANDS.CMD_DUMP_CONFIG_BLOCK_REWARD -> {
                    _logger.log(_controller.dumpConfigBlockReward())
                }

                ADMIN_COMMANDS.CMD_RELOAD_CONFIG_BLOCK_REWARD -> {
                    _controller.hotReloadConfigBlockReward()
                }

                ADMIN_COMMANDS.CMD_RELOAD_CONFIG_MIN_STAKE_HERO -> {
                    _controller.hotReloadConfigMinStakeHero()
                }

                ADMIN_COMMANDS.CMD_RELOAD_CONFIG_PACKAGE_AUTO_MINE -> {
                    _controller.hotReloadConfigPackageAutoMine()
                }

                ADMIN_COMMANDS.CMD_RELOAD_CONFIG_NATIVE_RATE -> {
                    _controller.hotReloadConfigNativeRate()
                }

                ADMIN_COMMANDS.CMD_DUMP_NATIVE_RATE -> {
                    _logger.log(_controller.dumpNativeRate())
                }

                ADMIN_COMMANDS.CMD_RELOAD_CONFIG_IAP_SHOP -> {
                    _controller.hotReloadConfigIapShop()
                }

                ADMIN_COMMANDS.CMD_DUMP_CONFIG_IAP_SHOP -> {
                    _logger.log(_controller.dumpConfigIapShop())
                }

                ADMIN_COMMANDS.CMD_RELOAD_CONFIG_TH_MODE_V2 -> {
                    _controller.hotReloadConfigTHModeV2()
                }

                ADMIN_COMMANDS.CMD_STOP_POOL_TH_MODE_V2 -> {
                    _controller.setStopPoolTHModeV2()
                }

                ADMIN_COMMANDS.CMD_RELOAD_TH_DATA_CONFIG -> {
                    _controller.hotReloadTreasureHuntDataConfig()
                }

                ADMIN_COMMANDS.CMD_RELOAD_BURN_HERO_CONFIG -> {
                    _controller.hotReloadBurnHeroConfig()
                }

                ADMIN_COMMANDS.CMD_RELOAD_PVP_CONFIG -> {
                    _controller.hotReloadPvpConfig()
                }

                ADMIN_COMMANDS.CMD_RELOAD_TON_TASKS -> {
                    _controller.hotReloadTonTasks()
                }

                ADMIN_COMMANDS.CMD_KICK_USER -> {
                    cmdData.data?.let { _controller.kickUser(it) }
                }

                ADMIN_COMMANDS.CMD_RELOAD_REFERRAL_PARAMS -> {
                    _controller.hotReloadReferralParams()
                }

                ADMIN_COMMANDS.CMD_FORCE_CLIENT_SEND_LOG -> {
                    _controller.forceClientSendLog(cmdData.data)
                }

                ADMIN_COMMANDS.CMD_BRIDGE_SET_DEPOSIT_ENABLED -> {
                    cmdData.data?.let { _controller.bridgeSetDepositEnabled(it) }
                }

                ADMIN_COMMANDS.CMD_BRIDGE_SET_WITHDRAW_ENABLED -> {
                    cmdData.data?.let { _controller.bridgeSetWithdrawEnabled(it) }
                }

                else -> {
                    _logger.log("Command not valid: ${cmdData.cmd}")
                }
            }
            _logger.log("Process admin command done")
        } catch (e: Exception) {
            _logger.error(e)
        }
    }

    @Serializable
    data class CmdData(
        val cmd: String,
        val data: String? = null,
    )
}

