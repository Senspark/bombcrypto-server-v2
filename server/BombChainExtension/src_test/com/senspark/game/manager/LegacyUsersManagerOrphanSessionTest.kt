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

/**
 * Khoá lại vòng đời phiên trong [LegacyUsersManager] (BNB/POLYGON).
 *
 * Nền: 27/08/2026 một tài khoản POLYGON/TREASURE bị "Your account is already logged in on another
 * device" suốt 20.5 giờ qua 45 lần thử, không có [Dispose] và không có evict ghost nào ở giữa.
 * Slot nằm chết trong _usersIds vì bản ghi keep-alive của nó bị xoá oan: removeFromMaps guard việc
 * gỡ slot bằng reference-equality nhưng lại dọn keep-alive vô điều kiện, nên dispose MUỘN của phiên
 * cũ (USER_DISCONNECT tới sau khi phiên mới đã takeover cùng slot) cuốn theo bản ghi của phiên MỚI.
 * Phiên mất bản ghi thì checkKeepAlive không thấy -> không bao giờ vào _timeOutUserIds ->
 * isHaveOldSession vĩnh viễn false -> evict không với tới và hasLiveConflict chặn login mãi mãi.
 */
class LegacyUsersManagerOrphanSessionTest {

    private val uid = 1001
    private val dataType = DataType.POLYGON
    private val landing = Landing.TREASURE
    private val userName = "0x00000000000000000000000000000000000000aapolygon"

    private val logger = mockk<ILogger>(relaxed = true)
    private val extension = mockk<SFSExtension>(relaxed = true)
    private val services = mockk<GlobalServices>(relaxed = true)

    /**
     * Đúng chuỗi sự kiện của prod: phiên A đang chơi, phiên B vào cùng slot (refresh / đổi thiết bị)
     * nên A bị takeover, RỒI USER_DISCONNECT của A mới tới và dispose A lần nữa.
     *
     * Sau chuỗi đó B vẫn là phiên sống thật, nên nó phải còn được tính là conflict. Nếu keep-alive
     * của B bị dispose muộn của A cuốn đi thì B thành phiên mồ côi và hasLiveConflict trả false —
     * đó là trạng thái không ai đánh timeout được, và là gốc của vụ khoá login 20.5 giờ.
     */
    @Test
    fun `late dispose of the old session must not take the new session keep-alive`() {
        val manager = LegacyUsersManager(logger)

        val controllerA = admit(manager)
        val controllerB = admit(manager)

        // Takeover đã dọn A khỏi map ngay trong admission của B.
        assertSame(controllerB, manager.getUserController(uid, dataType, landing))

        // USER_DISCONNECT của A tới muộn -> dispose A lần thứ hai, lúc slot đã trỏ B.
        manager.remove(controllerA)

        assertSame(
            controllerB,
            manager.getUserController(uid, dataType, landing),
            "dispose muộn của A không được gỡ slot của B",
        )
        assertTrue(
            manager.hasLiveConflict(uid, dataType, landing),
            "B vẫn đang sống nên phải còn là conflict; trả false nghĩa là keep-alive của B đã bị " +
                "dispose muộn của A xoá mất -> B thành phiên mồ côi, không bao giờ timeout được",
        )
    }

    /** Không có phiên nào thì không có gì để va chạm. */
    @Test
    fun `no session means no conflict`() {
        val manager = LegacyUsersManager(logger)

        assertFalse(manager.hasLiveConflict(uid, dataType, landing))
    }

    /** Một phiên vừa được nhận vào là phiên sống -> chặn login trùng. Giữ cho fix không nới quá tay. */
    @Test
    fun `freshly admitted session is a live conflict`() {
        val manager = LegacyUsersManager(logger)

        admit(manager)

        assertTrue(manager.hasLiveConflict(uid, dataType, landing))
    }

    /** Phiên thoát sạch thì slot phải trống, login sau không bị chặn. */
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

        assertSame(controller, admitted, "admission phải nhận phiên này")
        return controller
    }
}
