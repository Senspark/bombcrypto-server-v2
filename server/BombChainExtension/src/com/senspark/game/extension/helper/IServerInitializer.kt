package com.senspark.game.extension.helper

interface IServerInitializer {
    fun initHandlers(helper: AddRequestHandlerHelper)

    fun initStreamListeners()

    fun initSchedulers()
}