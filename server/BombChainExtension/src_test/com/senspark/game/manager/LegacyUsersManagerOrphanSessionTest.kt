package com.senspark.game.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.Landing
import com.senspark.game.extension.GlobalServices
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.extensions.SFSExtension
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Vòng đời phiên trong [LegacyUsersManager] (BNB/POLYGON). */
class LegacyUsersManagerOrphanSessionTest {

    private val uid = 1001
    private val dataType = DataType.POLYGON
    private val landing = Landing.TREASURE
    private val userName = "0x00000000000000000000000000000000000000aapolygon"

    private val logger = mockk<ILogger>(relaxed = true)
    private val extension = mockk<SFSExtension>(relaxed = true)
    private val services = mockk<GlobalServices>(relaxed = true)

    /**
     * B takeover slot của A, rồi USER_DISCONNECT của A mới tới và dispose A lần nữa. B vẫn sống
     * nên phải còn là conflict.
     */
    @Test
    fun `late dispose of the old session must not take the new session keep-alive`() {
        val manager = LegacyUsersManager(logger)

        val controllerA = admit(manager)
        val controllerB = admit(manager)

        assertSame(controllerB, manager.getUserController(uid, dataType, landing))

        // Dispose muộn của A, lúc slot đã trỏ B.
        manager.remove(controllerA)

        assertSame(controllerB, manager.getUserController(uid, dataType, landing))
        assertTrue(
            manager.hasLiveConflict(uid, dataType, landing),
            "trả false nghĩa là keep-alive của B đã bị dispose muộn của A xoá mất",
        )
    }

    @Test
    fun `no session means no conflict`() {
        val manager = LegacyUsersManager(logger)

        assertFalse(manager.hasLiveConflict(uid, dataType, landing))
    }

    @Test
    fun `freshly admitted session is a live conflict`() {
        val manager = LegacyUsersManager(logger)

        admit(manager)

        assertTrue(manager.hasLiveConflict(uid, dataType, landing))
    }

    @Test
    fun `session that left cleanly is no longer a conflict`() {
        val manager = LegacyUsersManager(logger)

        val controller = admit(manager)
        manager.remove(controller)

        assertFalse(manager.hasLiveConflict(uid, dataType, landing))
    }

    private fun admit(manager: LegacyUsersManager): IUserController {
        val userInfo = mockk<IUserInfo>(relaxed = true)
        every { userInfo.id } returns uid
        every { userInfo.dataType } returns dataType
        every { userInfo.username } returns userName

        val controller = mockk<IUserController>(relaxed = true)
        every { controller.userId } returns uid
        every { controller.userName } returns userName
        every { controller.userInfo } returns userInfo
        every { controller.landing } returns landing
        every { controller.verifyAndUpdateUserHash() } returns true

        var admitted: IUserController? = null
        manager.createUserController(
            extension,
            services,
            mockk<User>(relaxed = true),
            userInfo,
            landing,
            false,
            { controller },
        ) { admitted = it }

        assertSame(controller, admitted)
        return controller
    }
}
