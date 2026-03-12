package com.senspark.game.manager.ton

import com.senspark.common.utils.toSFSArray
import com.senspark.game.api.HttpClient
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.data.model.user.ClubInfo
import com.senspark.game.data.model.user.UserClub
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.ITHModeDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.utils.HashIdGenerator
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import java.util.Calendar
import java.util.TimeZone

@Serializable
data class CreateClubInfo(
    val uid: Int,
    val name: String,
    val link: String,
    val avatar_name: String,
    val telegram_id: Long //Id_telegram
)

@Serializable
data class JoinClubInfo(
    val uid: Int,
    val club_id: Int //id club từ database
)

@Serializable
data class LeaveClubInfo(
    val uid: Int
)

class UserClubManager(
    private val thModeDataAccess: ITHModeDataAccess,
    private val userDataAccess: IUserDataAccess,
    private val gameDataAccess: IGameDataAccess,
    private val coinRankingManager: ICoinRankingManager,
    private val usersManager: IUsersManager,
    private val envManager: IEnvManager,
    private val gameConfigManager: IGameConfigManager,
    private val hashIdGenerator: HashIdGenerator,
) : IClubManager {
    private var _configBidPrice: Map<Int, Int> = mutableMapOf()

    private var _allClubs: MutableList<ClubInfo> = mutableListOf()
    private var _userClubs: MutableMap<Int, UserClub> = mutableMapOf()

    // lấy danh sách top 10 bid club và điểm bid của ngày hôm trước
    private var _topClubBid: MutableMap<Int, Int> = mutableMapOf()

    // danh sách những club bid hôm nay
    private var _currentClubBid: MutableMap<Int, Int> = mutableMapOf()

    private var _timeSummaryClubPoint: Long? = null

    override fun initialize() {
        _configBidPrice = thModeDataAccess.getBidPrice()
        _allClubs = thModeDataAccess.getAllClub(coinRankingManager.currentSeasonNumber)
        _userClubs = thModeDataAccess.getUserClubs(coinRankingManager.currentSeasonNumber)
        _currentClubBid = thModeDataAccess.getCurrentClubBid()
        _timeSummaryClubPoint = coinRankingManager.currentSeason.endDate

        val topClub = thModeDataAccess.getTopClubBid()
        if (topClub.size < 10) {
            val additionalClubs =
                _allClubs.filter { !topClub.containsKey(it.id) }.sortedByDescending { getClubPoint(it, false) }
                    .take(10 - topClub.size)
            additionalClubs.forEach {
                topClub[it.id] = 0
            }
        }
        _topClubBid = topClub

        if (topClub.size < 10) {
            summaryClubBid()
        }
    }

    // Gọi cho club hiện tại của user
    override fun getClubInfo(uid: Int): ISFSObject {
        if (!_userClubs.containsKey(uid)) {
            return SFSObject()
        }
        val clubId = _userClubs[uid]!!.clubId
        return getClubInfoById(clubId).apply {
            putUtfString("referral_code", hashIdGenerator.encode(uid))
            putSFSObject("current_member", _userClubs[uid]!!.toSFSObject())
        }
    }

    // Gọi cho club khác
    override fun getClubInfoById(clubId: Int): ISFSObject {
        val clubInfo = _allClubs.first { it.id == clubId }
        val clubMembers = _userClubs.filter { it.value.clubId == clubId }

        val result = SFSObject().apply {
            //support old Ton client
            putLong("club_id", clubInfo.id.toLong())
            putInt("id", clubInfo.id)
            putUtfString("club_name", clubInfo.name)
            putUtfString("club_link", clubInfo.link ?: "")
            if (clubInfo.avatarName != null) {
                val clubAvatar = getClubAvatar(clubInfo.avatarName)
                if (clubAvatar != null) {
                    putByteArray("club_avatar", clubAvatar)
                }
            }
            putDouble("club_point_total", getClubPoint(clubInfo, true))
            putDouble("club_point_current_season", getClubPoint(clubInfo, false))
            putSFSArray("club_members", clubMembers.toSFSArray { it.value.toSFSObject() })
            putBool("is_top_bid_club", _topClubBid.containsKey(clubId))
        }
        return result
    }

    override fun getListClub(): ISFSObject {
        val result = SFSObject().apply {
            putSFSArray("data", _allClubs.toSFSArray {
                SFSObject().apply {
                    //support old Ton client
                    putLong("club_id", it.id.toLong())
                    putInt("id", it.id)
                    putUtfString("club_name", it.name)
                    putDouble("club_point_total", getClubPoint(it, true))
                    putDouble("club_point_current_season", getClubPoint(it, false))
                }
            })
        }
        return result
    }

    override fun getTopBidClub(): ISFSObject {
        val topClub = _allClubs.filter { _topClubBid.containsKey(it.id) && getTotalBidOfTopClub(it.id) != 0 }
            .sortedByDescending { getTotalBidOfTopClub(it.id) }.toMutableList()
        // những club được thêm vào do thiếu club mà chưa có điểm bid sẽ được lấy theo sort sẵn
        _topClubBid.forEach { entry ->
            if (getTotalBidOfTopClub(entry.key) == 0) {
                topClub.add(_allClubs.first { it.id == entry.key })
            }
        }
        val result = SFSObject().apply {
            putSFSArray("data", topClub.toSFSArray {
                SFSObject().apply {
                    //support old Ton client
                    putLong("club_id", it.id.toLong())
                    putInt("id", it.id)
                    putUtfString("club_name", it.name)
                    putDouble("club_point_total", getClubPoint(it, true))
                    putDouble("club_point_current_season", getClubPoint(it, false))
                }
            })
        }
        return result
    }

    override fun getBidPackage(clubId: Int): MutableMap<Int, Int> {
        if (_topClubBid.containsKey(clubId)) {
            val topBid = mutableMapOf<Int, Int>()
            // cộng điểm bid hôm trước và điểm bid hôm nay để ra được kết quả trong top 10
            // (first, second): (club id, bid tổng)
            val topClub = _topClubBid.map { it.key to getTotalBidOfTopClub(it.key) }
                .sortedByDescending { it.second }

            val top1club = topClub.first()
            // top 1 chỉ có package giá cao nhất
            if (top1club.first == clubId) {
                topBid[1] = _configBidPrice[1]!!
            } else {
                val totalBid = getTotalBidOfTopClub(clubId)
                for (index in topClub.indices) {
                    if (topClub[index].first == clubId) {
                        break
                    }
                    // skip những club có điểm bid bằng nhau
                    if (index - 1 >= 0 && topClub[index].second == topClub[index - 1].second) {
                        continue
                    }
                    if (topClub[index].second >= totalBid) {
                        topBid[index + 1] = topClub[index].second - totalBid + 1
                    }
                    if (topClub[index].second == 0) {
                        break
                    }
                }
            }
            return topBid
        } else {
            return _configBidPrice.toMutableMap()
        }
    }

    // Bot gọi tạo club có id telegram
    override fun createClub(json: String) {
        val info = Json.decodeFromString<CreateClubInfo>(json)
        val uid = info.uid
        val displayName = userDataAccess.getDisplayNameUser(uid)
        val id = createClub(info.telegram_id, info.name, info.link, info.avatar_name)
        joinClub(uid, displayName, id, true)
        usersManager.getUserController(uid)?.sendDataEncryption("USER_JOIN_CLUB", getClubInfo(uid))
    }


    // Bot telegram dùng để tạo club có id telegram
    override fun createClub(telegramId: Long, name: String, link: String, avatarName: String): Int {
        val id = thModeDataAccess.addNewClub(telegramId, name, link, coinRankingManager.currentSeasonNumber, avatarName)
        _allClubs.add(ClubInfo(id, name, link, 0.0, 0.0, avatarName, telegramId))
        return id;
    }

    override fun createClubV3(uid: Int, nameClub: String) {
        if (_allClubs.any { it.name == nameClub }) {
            throw CustomException("Club already exists")
        }
        val displayName = userDataAccess.getDisplayNameUser(uid)
        val clubId =
            thModeDataAccess.addNewClubV2(nameClub, coinRankingManager.currentSeasonNumber, EnumConstants.ClubType.TON)
        _allClubs.add(ClubInfo(clubId, nameClub, "", 0.0, 0.0, ""))
        joinClub(uid, displayName, clubId, false)
    }


    override fun joinClub(json: String) {
        val info = Json.decodeFromString<JoinClubInfo>(json)
        val uid = info.uid
        val displayName = userDataAccess.getDisplayNameUser(uid)
        joinClub(uid, displayName, info.club_id, true)
        usersManager.getUserController(uid)?.sendDataEncryption("USER_JOIN_CLUB", getClubInfo(uid))
    }

    override fun joinClub(uid: Int, displayName: String, clubId: Int, isForceLeave: Boolean) {
        if (_allClubs.firstOrNull { it.id == clubId } == null) {
            return
        }
        if (_userClubs.containsKey(uid)) {
            if (!isForceLeave || _userClubs[uid]!!.clubId == clubId) {
                return
            }
            leaveClub(uid)
        }
        _userClubs[uid] = UserClub(clubId, displayName, 0.0, 0.0)
        thModeDataAccess.joinClub(uid, clubId, coinRankingManager.currentSeasonNumber)
    }

    override fun joinClubV3(uid: Int, nameClub: String) {
        if (_userClubs.containsKey(uid)) {
            throw CustomException("User already in club")
        }
        val club = _allClubs.find { it.name == nameClub }
            ?: throw CustomException("Club does not exist \\n Please try again\n")
        val displayName = userDataAccess.getDisplayNameUser(uid)
        _userClubs[uid] = UserClub(club.id, displayName, 0.0, 0.0)
        thModeDataAccess.joinClub(uid, club.id, coinRankingManager.currentSeasonNumber)
    }


    override fun leaveClub(json: String) {
        val uid = Json.decodeFromString<LeaveClubInfo>(json).uid
        leaveClub(uid)
        usersManager.getUserController(uid)?.sendDataEncryption("USER_LEAVE_CLUB", SFSObject())
    }

    override fun leaveClub(uid: Int) {
        if (!_userClubs.containsKey(uid)) {
            throw CustomException("User isn't in club")
        }
        // khi user out club thì lưu lại point trong club
        val club = _allClubs.first { it.id == _userClubs[uid]!!.clubId }
        val addPoint = _userClubs[uid]!!.pointCurrentSeason
        club.pointTotal += addPoint
        club.pointCurrentSeason += addPoint

        thModeDataAccess.addClubPoint(club.id, coinRankingManager.currentSeasonNumber, addPoint)
        _userClubs.remove(uid)
        thModeDataAccess.leaveClub(uid)
    }

    override fun addMemberPoint(uid: Int, point: Double, season: Int) {
        if (_userClubs.containsKey(uid)) {
            _userClubs[uid]!!.pointCurrentSeason += point
            _userClubs[uid]!!.pointTotal += point
            thModeDataAccess.addMemberClubPoint(uid, _userClubs[uid]!!.clubId, season, point)
        }
    }

    override fun summaryClubPoint() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
        if (_timeSummaryClubPoint != null && now > _timeSummaryClubPoint!!) {
            // Do không có thời gian giữa 2 season nên khi summary đã qua season mới
            thModeDataAccess.summaryClubPoint(coinRankingManager.currentSeasonNumber - 1)
            _userClubs.forEach {
                it.value.pointCurrentSeason = 0.0
            }
            _allClubs.forEach {
                it.pointCurrentSeason = 0.0
            }
            _timeSummaryClubPoint = coinRankingManager.currentSeason.endDate
        }
    }

    override fun boostClub(userController: IUserController, clubId: Int, packageId: Int) {
        val bidPrice = getBidPackage(clubId)
        if (!bidPrice.containsKey(packageId)) {
            throw CustomException("This package isn't exist")
        }
        val bidQuantity = bidPrice[packageId]!!
        val price = ClubHelper.calculateBidPrice(gameConfigManager.bidUnitPrice, bidQuantity)
        gameDataAccess.subUserBlockReward(
            userController.userId, userController.dataType,
            EnumConstants.BLOCK_REWARD_TYPE.TON_DEPOSITED, price, ChangeRewardReason.BUY_BID_CLUB
        )

        if (!_currentClubBid.containsKey(clubId)) {
            _currentClubBid[clubId] = 0
        }
        _currentClubBid[clubId] = _currentClubBid[clubId]!! + bidQuantity
        thModeDataAccess.addClubBidPoint(clubId, bidQuantity)
    }

    override fun summaryClubBid() {
        val topClub = _currentClubBid.toList().sortedByDescending { it.second }.take(10).toMap().toMutableMap()

        if (topClub.size < 10) {
            val additionalClubs =
                _allClubs.filter { !topClub.containsKey(it.id) }.sortedByDescending { getClubPoint(it, false) }
                    .take(10 - topClub.size)
            additionalClubs.forEach { topClub[it.id] = 0 }
        }
        _topClubBid = topClub
        _currentClubBid = mutableMapOf()
    }

    override fun getClubIdByTelegramId(telegramId: Long): Int? {
        return _allClubs.firstOrNull { it.telegramId == telegramId }?.id
    }

    private fun getTotalBidOfTopClub(clubId: Int): Int {
        val currentClubBid = if (_currentClubBid.containsKey(clubId)) {
            _currentClubBid[clubId]!!
        } else 0
        return _topClubBid[clubId]!! + currentClubBid
    }

    private fun getClubPoint(club: ClubInfo, isAllSeason: Boolean): Double {
        var clubPoint = if (isAllSeason) club.pointTotal else club.pointCurrentSeason
        _userClubs.forEach {
            if (it.value.clubId == club.id) {
                clubPoint += if (isAllSeason) {
                    it.value.pointTotal
                } else {
                    it.value.pointCurrentSeason
                }
            }
        }
        return clubPoint
    }

    private fun getClubAvatar(avatarName: String): ByteArray? {
        if (avatarName == "") {
            return null
        }
        try {
            val url = String.format(envManager.avatarClubUrl, avatarName)
            val request = Request.Builder().url(url).build()
            HttpClient.getInstance().newCall(request).execute().use { response: Response ->
                if (!response.isSuccessful) return null
                else return response.body?.bytes()
            }
        } catch (_: Exception) {
            return null
        }
    }
}
