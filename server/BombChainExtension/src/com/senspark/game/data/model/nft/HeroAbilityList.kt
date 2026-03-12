package com.senspark.game.data.model.nft

import com.senspark.game.utils.serialize

class HeroAbilityList(val items: List<Int>) {
    fun has(ability: Int): Boolean {
        return items.contains(ability)
    }

    override fun toString(): String {
        return items.serialize()
    }
}