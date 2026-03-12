package com.senspark.game.data.manager.newUserGift

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.config.NewUserGift

class NullNewUserGiftManager : INewUserGiftManager {

    override fun initialize() {
    }

    override fun takeNewUserAndAddGifts(userController: IUserController): List<NewUserGift> {
        return emptyList()
    }
}