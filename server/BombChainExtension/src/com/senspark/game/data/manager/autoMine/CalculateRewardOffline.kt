package com.senspark.game.data.manager.autoMine

import com.senspark.game.data.model.config.OfflineRewardTHModeConfig
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.declare.GameConstants.BOMBER_STAGE
import com.senspark.game.exception.CustomException
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class CalculateInfo(
    val energyMax: Int,
    val dmgPerMinute: Double,
    val energyUsedPerMinute: Double,
    val energyRecoverPerMinute: Double,
    val blockHp: Double,
    val coinPerBlock: Double,
    val startEnergy: Int,
    val offlineMinutes: Double,
    val currentHeroState: Int,
)

class CalculateRewardOffline {
    companion object {
        fun calculate(heroes: List<Hero>, house: House?, config: OfflineRewardTHModeConfig, timeOffline: Double): Double {
            val damageAllHeroes = mutableListOf<Double>()
            heroes.sortedBy { it.rarity }
            val blockHp = config.blockHp
            val blockValue = config.blockValue
            var houseSlot = house?.capacity ?: 0

            // tính tổng damage gây ra rồi mới tính reward
            heroes.forEach {
                val damage = config.heroesDamage.getOrNull(it.rarity) ?: 0.0
                val energyUsed = config.energyUsed.getOrNull(it.rarity) ?: 0.0
                if (house != null && houseSlot > 0) {
                    damageAllHeroes.add(calculateDamageHero(it, damage, energyUsed, house.recovery.toDouble(), blockHp, blockValue, timeOffline))
                    houseSlot -= 1
                } else {
                    damageAllHeroes.add(calculateDamageHero(it, damage, energyUsed, 0.5, blockHp, blockValue, timeOffline))
                }
            }
            val totalDamage = damageAllHeroes.sum()
            return coinEarnedCalculate(totalDamage, blockHp, blockValue)
        }

        private fun calculateDamageHero(
            hero: Hero,
            damage: Double,
            energyUsed: Double,
            energyRecoverPerMinute: Double,
            blockHp: Double,
            blockValue: Double,
            timeOffline: Double
        ): Double {
            try {
                if (damage == 0.0 || blockHp == 0.0 || energyUsed == 0.0) {
                    throw CustomException("Damage or Block Hp or energy used is 0")
                }
                val info = CalculateInfo(
                    // hồi 70% năng lượng là bắt đầu work
                    (hero.stamina * 50 * 0.7).toInt(),
                    damage,
                    energyUsed,
                    energyRecoverPerMinute,
                    blockHp,
                    blockValue,
                    hero.energy,
                    timeOffline,
                    hero.stage
                )
                return if (info.currentHeroState == BOMBER_STAGE.WORK) {
                    calculateDamageFromWork(info)
                } else {
                    calculateDamageFromSleep(info)
                }
            } catch (e: Exception) {
                return 0.0
            }
        }

        private fun calculateDamageFromWork(info: CalculateInfo): Double {
            // dùng hết năng lượng rồi hồi năng lượng đến max rồi mới bắt đầu tính damage
            val timeToSpendStartEnergy = info.startEnergy / info.energyUsedPerMinute
            if (timeToSpendStartEnergy > info.offlineMinutes) {
                return info.offlineMinutes * info.dmgPerMinute
            }
            val dmgAtStart = timeToSpendStartEnergy * info.dmgPerMinute

            val timeToRecoverFullEnergy = info.energyMax / info.energyRecoverPerMinute

            val timeLeftAfterSpendStartEnergy =
                max(0.0, info.offlineMinutes - timeToSpendStartEnergy - timeToRecoverFullEnergy)
            val result = damageCalculate(info, timeLeftAfterSpendStartEnergy, dmgAtStart)
            return result
        }

        private fun calculateDamageFromSleep(info: CalculateInfo): Double {
            // trừ thời gian hồi năng lượng đến max rồi mới bắt đầu tính damage
            var timeNeedToRecoverFromStartEnergyToFull =
                (info.energyMax - info.startEnergy) / info.energyRecoverPerMinute
            if (timeNeedToRecoverFromStartEnergyToFull < 0) {
                timeNeedToRecoverFromStartEnergyToFull = 0.0
            }
            val timeLeftAfterSpendStartRecover = info.offlineMinutes - timeNeedToRecoverFromStartEnergyToFull

            val result = damageCalculate(info, timeLeftAfterSpendStartRecover, 0.0)
            return result
        }

        private fun damageCalculate(
            info: CalculateInfo,
            offlineMinutesLeft: Double,
            initialDmg: Double
        ): Double {
            // thời gian âm nên không có dmg theo chu kì
            if (offlineMinutesLeft < 0) {
                return initialDmg
            }
            val timeToSpendAllEnergy = info.energyMax / info.energyUsedPerMinute
            val timeToRecoverFullEnergy = info.energyMax / info.energyRecoverPerMinute

            // thời gian 1 chu kì dùng hết năng lượng, hồi lại full và dmg gây ra ở 1 chu kì
            val timeOneCycle = timeToSpendAllEnergy + timeToRecoverFullEnergy
            val totalCycles = offlineMinutesLeft / timeOneCycle
            val dmgPerOneCycle = info.dmgPerMinute * timeToSpendAllEnergy

            // hết 1 chu kì hero full năng lượng nên tính thời gian có thể gây dmg còn lại
            val timeRemainingAfterFullCycles = offlineMinutesLeft % timeOneCycle
            val timeToSpendAllEnergyAfterFullCycles = min(timeRemainingAfterFullCycles, timeToSpendAllEnergy)

            // tính tổng damage con hero này gây ra được
            val totalDmg =
                initialDmg + dmgPerOneCycle * floor(totalCycles) + timeToSpendAllEnergyAfterFullCycles * info.dmgPerMinute

            return totalDmg
        }

        private fun coinEarnedCalculate(
            totalDamage: Double,
            blockHp: Double,
            blockValue: Double
        ): Double {
            val totalBlockBreak = totalDamage / blockHp
            val totalCoinEarned = totalBlockBreak * blockValue

            return totalCoinEarned
        }
    }
}