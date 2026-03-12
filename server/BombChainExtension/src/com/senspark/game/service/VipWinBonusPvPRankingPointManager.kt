package com.senspark.game.service

/* FIXME: Unused
class VipWinBonusPvPRankingPointManager(
    vip: List<VipWinBonusPvPRankingPointData>
) : PvPRankManager.IWinBonusPointHandler {
    private val _vip = vip.associateBy { it.vip }
    override fun getPoint(controller: IPvPUserController, win: Boolean, rank: Int, isRankOverTooMany: Boolean): Int {
        val vip = controller.serviceLocator.resolve<UserStakeVipManager>().isVip()
        val vipData = _vip[vip] ?: throw Exception("Could not find vip: $vip")
        return vipData.value
    }
}
 */