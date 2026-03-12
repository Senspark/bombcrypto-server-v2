package com.senspark.game.extension

import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IServerService
import com.senspark.common.service.ServiceContainer

typealias GlobalServices = ServiceContainer<IGlobalService>
typealias ServerServices = ServiceContainer<IServerService>
