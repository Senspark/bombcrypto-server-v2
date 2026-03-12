package com.senspark.game.handler.room

import com.senspark.common.service.IScheduler
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.manager.IUsersManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BaseGameRequestMessageQueue(
    name: String,
    private val _usersManager: IUsersManager,
    private val _scheduler: IScheduler,
    private val _handler: (legacyUserController: LegacyUserController, params: ISFSObject) -> Unit,
) {
    private val _userRequests: ConcurrentHashMap<LegacyUserController, Queue<ISFSObject>> = ConcurrentHashMap()
    private val _schedulerName = "BGRMQ_${name}"

    init {
        _scheduler.schedule(_schedulerName, 0, 1000, ::handleRequest)
    }

    fun remove(user: LegacyUserController) {
        _userRequests.remove(user)
    }

    fun add(user: LegacyUserController, request: ISFSObject) {
        if (!_userRequests.containsKey(user)) {
            _userRequests[user] = LinkedList()
        }
        _userRequests[user]?.add(request)
    }

    fun dispose() {
        _scheduler.clear(_schedulerName)
        _userRequests.clear()
    }

    private fun handleRequest() {
        val willRemove = mutableListOf<LegacyUserController>()
        _userRequests.forEach { (user, requests) ->

            if (user.isDisposed() || !_usersManager.checkExistence(user)) {
                willRemove.add(user)
                return@forEach
            }

            if (!user.isInitialized()) {
                return@forEach
            }

            requests.forEach {
                try {
                    _handler(user, it)
                } catch (e: Exception) {
                    // ignore
                }
            }

            requests.clear()
        }

        willRemove.forEach {
            _userRequests.remove(it)
        }
    }
}