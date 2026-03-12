package com.senspark.game.manager.ton

import com.senspark.common.utils.ILogger
import com.senspark.game.declare.EnumConstants
import com.senspark.game.manager.IUsersManager
import com.senspark.game.utils.ServerError
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.exceptions.SFSErrorData
import com.smartfoxserver.v2.exceptions.SFSLoginException

class ForceLoginManager(
    private val _usersManager: IUsersManager,
    private val _logger: ILogger,
) : IForceLoginManager {
    private val userForceLogin: MutableMap<String, String> = mutableMapOf() // username, hashSession

    override fun initialize() {
    }

    override fun checkToForceLogin(
        data: SFSObject,
        username: String,
        dataType: EnumConstants.DataType,
        sessionId: String?
    ) {
        // Check để clear session cũ trước khi check force login
        _usersManager.safeCheckAndDisposeOldSession(username)

        // Nếu có force_login, kick device đã login trước đó để thực hiện login lần này
        // Nếu userMapForceLogin đã có key của user này rồi tức là có 1 user khác đã force_login trước mà chưa vào đc
        // nên key này còn => ko cho user này force login nữa, phải đợi user kia vào, user này sẽ đc pass login handler
        // và bị kick ở join zone => Trường hợp này thường chỉ xảu ra khi user bị kick là user đã force_login trước
        // đó và đây là do reconnect lại nên phải xử lý kick ở join zone, kick xong user force login kia sẽ vào đc
        if (data.containsKey("force_login")) {
            if (data.getBool("force_login")) {
                if (sessionId != null && !userForceLogin.containsKey(username)) {
                    _logger.log("$username force login")
                    userForceLogin[username] = sessionId
                }
            }
        }
        // Trường hợp này là 1 user bth đang chơi bị user khác force login và reconnect lại sẽ đi tới đây
        // và bị throw ex này đồng thời xoá key ở userMapForceLogin, để support những lần force login sau
        // Sau khi user này bị throw ex, user force_login sẽ vào game đc
        else if (userForceLogin.containsKey(username)) {
            if (userForceLogin[username] != sessionId) {
                userForceLogin.remove(username)
                _logger.log("$username be kick")
                val err = SFSErrorData(ServerError.KICK_BY_OTHER_DEVICE)
                throw SFSLoginException("Kick by other device", err)
            }
        }
        //Đây là trường hợp 1 user đang login bình thường thì găp account của mình đang đc sử dụng ở thiết bị khác
        // => hiện dialog cho phép chọn force login hoặc logout ở client
        // Throw ex này ở đây này để fix thỉnh thoảng user bị đứng 50% nhưng client cũ sẽ ko vào đc nên có thể ra sau
        // Khi ko có ex này thì smartfox vẫn throw ex LOGIN_ALREADY_LOGGED (cái mà client cũ đang dùng)

        else {
            val userId = _usersManager.getUserId(username)
            // Tìm ko thấy user này, có thể đang chơi bằng wallet và giờ vô bằng account fi nên cần check address mà account này sử dung
            if (userId != -1) {
                //Kiểm tra xem user với network này có đang chơi trong game ko, check cho cả account fi và wallet
                // Vì account fi vào game với network bsc sẽ đc xem như user vào game bằng ví bsc
                if (!_usersManager.isLoggedIn(userId, dataType)) {
                    // uid này chưa login vào game bằng network này nên đc phép login
                    return
                }

                if (_usersManager.isUserLoggedOut(userId, dataType)) {
                    //Nếu user này đã logout thì ko hiện thông báo cho user mới login
                    return
                }
                val err = SFSErrorData(ServerError.AlREADY_LOGIN)
                throw SFSLoginException("Your account is already logged in on another device.", err)
            }
        }

    }

    override fun checkToKickUser(username: String, sessionHash: String): Boolean {
        if (userForceLogin.containsKey(username)) {
            if (userForceLogin[username] != sessionHash) {
                //Remove key này khỏi map force login để support những lần force login sau
                userForceLogin.remove(username)
                return true
            }
        }
        return false
    }

    // Do bây giở 1 account có thể chơi nhiều network cùng 1 lúc, thêm cả account fi với 1 uid duy nhất nên sẽ phải check thêm lần nữa
    // bắng uid và dataType để đảm bảo user này chưa login vô bằng network đó
    override fun checkToKickAccountFi(uid: Int, dataType: EnumConstants.DataType) {
        if (_usersManager.isLoggedIn(uid, dataType)) {
            val err = SFSErrorData(ServerError.AlREADY_LOGIN)
            throw SFSLoginException("Your account is already logged in on another device.", err)
        }
    }
}