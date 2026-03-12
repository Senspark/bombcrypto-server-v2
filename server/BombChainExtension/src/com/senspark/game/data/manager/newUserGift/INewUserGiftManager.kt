package com.senspark.game.data.manager.newUserGift

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.config.NewUserGift

interface INewUserGiftManager : IServerService {
    fun takeNewUserAndAddGifts(userController: IUserController): List<NewUserGift>
}