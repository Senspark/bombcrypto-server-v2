package com.senspark.game.handler.subscription

import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.customEnum.IapStore
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.room.BaseGameRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class SubscribeSubscriptionHandler : UserSubscriptionHandler(SFSCommand.SUBSCRIBE_SUBSCRIPTION)
class SubscriptionsHandler : UserSubscriptionHandler(SFSCommand.SUBSCRIPTIONS)
class CancelSubscribeSubscriptionHandler : UserSubscriptionHandler(SFSCommand.CANCEL_SUBSCRIBE_SUBSCRIPTION)

open class UserSubscriptionHandler(
    val command: String
) : BaseGameRequestHandler() {
    override val serverCommand = command

    override fun handleGameClientRequest(controller: LegacyUserController, params: ISFSObject) {
        return try {
            val userManager = controller.masterUserManager.userSubscriptionManager
            when (command) {
                SFSCommand.SUBSCRIBE_SUBSCRIPTION -> {
                    val subscriptionProduct = SubscriptionProduct.fromNormalizeName(params.getUtfString("product_id"))
                    val userToken = params.getUtfString("token")
                    val store = IapStore.fromValue(params.getInt("store_id"))
                    userManager.subscribe(subscriptionProduct, userToken, store)
                    sendSuccessClient(controller)
                }

                SFSCommand.CANCEL_SUBSCRIBE_SUBSCRIPTION -> {
                    val subscriptionProduct = SubscriptionProduct.fromNormalizeName(params.getUtfString("product_id"))
                    userManager.cancelSubscribe(subscriptionProduct)
                    sendSuccessClient(controller)
                }

                SFSCommand.SUBSCRIPTIONS -> {
                    sendResponseToClient(userManager.subscriptionPackages, controller)
                }

                else -> throw CustomException("[UserSubscriptionHandler] Command invalid")
            }
        } catch (ex: Exception) {
            sendMessageError(ex, controller)
        }
    }
}