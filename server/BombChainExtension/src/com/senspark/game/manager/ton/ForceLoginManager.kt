package com.senspark.game.manager.ton

import com.senspark.common.utils.ILogger
import com.senspark.game.declare.EnumConstants
import com.senspark.game.manager.IUsersManager
import com.senspark.game.utils.ServerError
import com.smartfoxserver.v2.exceptions.SFSErrorData
import com.smartfoxserver.v2.exceptions.SFSLoginException

class ForceLoginManager(
    private val _usersManager: IUsersManager,
    private val _logger: ILogger,
) : IForceLoginManager {

    override fun initialize() {
    }

    // Check DUY NHẤT ở login (post-login, đã có uid), key thuần theo (uid, dataType, landing) — KHÔNG tra
    // theo username nữa. Chỉ là best-effort UX hiện dialog sớm; atomic-admission ở join zone (takeover/
    // reclaim ghost/kick force/reject) mới là chốt cuối.
    override fun checkToKickAccountFi(
        uid: Int,
        dataType: EnumConstants.DataType,
        landing: EnumConstants.Landing,
        forceLogin: Boolean
    ) {
        // force_login -> để admission kick phiên cũ va chạm, KHÔNG chặn ở login.
        if (forceLogin) return
        // Chỉ reject khi có phiên SỐNG va chạm; ghost (timeout) bỏ qua -> admission tự RECLAIM, tránh reject
        // oan lúc reconnect sau timeout.
        if (_usersManager.hasLiveConflict(uid, dataType, landing)) {
            _logger.log("[ForceLogin] reject login uid=$uid dt=$dataType landing=$landing -> AlREADY_LOGIN (đã có phiên sống)")
            val err = SFSErrorData(ServerError.AlREADY_LOGIN)
            throw SFSLoginException("Your account is already logged in on another device.", err)
        }
    }
}