package com.senspark.game.handler

import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.MainGameExtension
import com.senspark.game.extension.modules.ISvServicesContainer
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler
import com.smartfoxserver.v2.extensions.BaseServerEventHandler

abstract class MainGameExtensionBaseRequestHandler : BaseClientRequestHandler() {
    protected val services: GlobalServices = MainGameExtension.Services
}

abstract class MainGameExtensionBaseEventHandler : BaseServerEventHandler() {
    protected val globalServices: GlobalServices = MainGameExtension.Services
    protected val svServices: ISvServicesContainer = globalServices.get<ISvServicesContainer>()
}