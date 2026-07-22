package com.senspark.game.handler.nft

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.HeroType
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.pvp.HandlerCommand
import com.smartfoxserver.v2.entities.data.ISFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * V3 (fire-and-forget + async push) — replacement của V2 (sync response):
 *
 * - V2: server check on-chain rồi trả `HeroDetails` qua response sync. Client phải
 *   block UI chờ.
 * - V3: client gửi và không chờ response. Server check on-chain rồi đẩy
 *   [HandlerCommand.BheroStakePush] async. Client không block — UI cập nhật khi
 *   push tới.
 *
 * Optional flag `debug_fake_push=true` (Editor): skip on-chain HTTP check vì
 * Editor build không stake được, gọi blockchain cũng chỉ trả về state cũ. Khi
 * skip thì push gần như instant → race với client (UI Processing chưa kịp tắt
 * trước khi DialogStakingResult hiện) → delay 1s trước khi push.
 * Production không cần delay vì on-chain HTTP check đã có độ trễ tự nhiên.
 *
 * V2 vẫn được register cho client cũ (đã build), không động tới.
 */
class CheckBomberStakeV3Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CHECK_BOMBER_STAKE_V3

    companion object {
        private const val DEBUG_PUSH_DELAY_MS = 1000L
    }

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }
        val bomberId = data.getLong(SFSField.ID).toInt()
        val debugFakePush = data.containsKey("debug_fake_push") && data.getBool("debug_fake_push")
        val heroFiManager = controller.masterUserManager.heroFiManager
        val bomber = heroFiManager.getHero(bomberId, HeroType.FI) ?: return

        if (debugFakePush) {
            coroutine.scope.launch(Dispatchers.Default) {
                delay(DEBUG_PUSH_DELAY_MS)
                controller.sendDataEncryption(
                    HandlerCommand.BheroStakePush,
                    heroFiManager.heroToSfsObject(bomber),
                    true
                )
            }
            return
        }

        val heroesStakeManager = controller.svServices.get<IHeroStakeManager>()
        heroesStakeManager.checkBomberStake(controller.dataType, bomber)
        controller.sendDataEncryption(
            HandlerCommand.BheroStakePush,
            heroFiManager.heroToSfsObject(bomber),
            true
        )
    }
}
