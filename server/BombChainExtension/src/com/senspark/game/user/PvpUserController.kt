package com.senspark.game.user

import com.senspark.common.constant.ItemId
import com.senspark.common.pvp.IMatchHeroInfo
import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.game.constant.Booster
import com.senspark.game.constant.ItemType
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.manager.IEnvManager
import com.senspark.game.pvp.info.MatchHeroInfo
import com.senspark.game.pvp.info.MatchUserInfo

private fun createPvPMapHeroData(
    hero: Hero,
    skinChests: Map<ItemType, List<ItemId>>,
    calculator: IInitHeroStatCalculator
): IMatchHeroInfo {

    return MatchHeroInfo(
        id = hero.heroId,
        color = hero.color,
        skin = hero.skin,
        skinChests = skinChests.mapKeys { it.key.value },
        health = calculator.hp,
        speed = calculator.speed,
        damage = calculator.dmg,
        bombCount = calculator.bomb,
        bombRange = calculator.range,
        maxHealth = hero.maxHp,
        maxSpeed = hero.maxSpeed,
        maxDamage = hero.maxDmg,
        maxBombCount = hero.maxBomb,
        maxBombRange = hero.maxRange,
    )
}

class PvpUserController(
    private val _info: IUserInfo,
    private val _envManager: IEnvManager,
    private val _configItemManager: IConfigItemManager,
    private val _skinChestManager: IUserInventoryManager,
) {

    private val fullName = _info.name ?: _info.secondUsername
    private val walletAddress = _info.username

    fun getMatchInfo(
        controller: IUserController,
        matchId: String,
        mode: Int,
        isTest: Boolean,
        hero: Hero,
        boosters: Map<Int, Int>,
        avatar: Int
    ): IMatchUserInfo {
        val manager = controller.masterUserManager
        return MatchUserInfo(
            serverId = _envManager.serverId,
            buildVersion = 1,
            matchId = matchId,
            mode = mode,
            isTest = isTest,
            isWhitelisted = false,
            isBot = false,
            userId = controller.userId,
            username = controller.userName,
            displayName = fullName ?: walletAddress,
            totalMatchCount = manager.userConfigManager.miscConfigs.pvpMatchCount,
            matchCount = controller.pvpRank.matchCount,
            winMatchCount = controller.pvpRank.winMatch,
            rank = controller.pvpRank.bombRank,
            point = controller.pvpRank.point.value,
            boosters = manager.userPvPBoosterManager.getSelectBoosters().map { it.itemId },
            availableBoosters = boosters,
            hero = createPvPMapHeroData(
                hero,
                _skinChestManager.activeSkinChests,
                InitHeroStatCalculator(
                    _configItemManager,
                    _skinChestManager.activeSkinChests,
                    manager.userPvPBoosterManager.getSelectBoosters().map { Booster.fromValue(it.itemId) }.toSet(),
                    hero
                )
            ),
            avatar = avatar
        )
    }
}