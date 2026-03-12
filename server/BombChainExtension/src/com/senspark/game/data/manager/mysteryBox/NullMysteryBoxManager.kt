package com.senspark.game.data.manager.mysteryBox

import com.senspark.game.data.model.config.IMysteryBox
import com.senspark.game.exception.CustomException

class NullMysteryBoxManager : IMysteryBoxManager {

    override fun initialize() {
    }
    
    override fun getRandomItem(): IMysteryBox {
        throw CustomException("Feature not support")
    }
}