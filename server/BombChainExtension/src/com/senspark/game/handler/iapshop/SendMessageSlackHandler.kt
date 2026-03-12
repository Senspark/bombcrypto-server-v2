package com.senspark.game.handler.iapshop

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.*
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import java.io.IOException
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient
import com.slack.api.model.block.HeaderBlock
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.composition.PlainTextObject

class SendMessageSlackHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SEND_MESSAGE_SLACK_V2

    private val _gameConfigManager = services.get<IGameConfigManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val id = _gameConfigManager.chanelSlackId
        val dataSend = data.getSFSObject("data")
        var textBody = ""
        textBody += "• *Username*: `${controller.userInfo.username}`, *User Id*: `${controller.userId}`\n"
        textBody += "• *Amount*: `${dataSend.getUtfString("Amount")}`\n"
        dataSend.removeElement("Amount")
        textBody += "• *Data*: ${dataSend.toJson()}\n"

        val client: MethodsClient? = Slack.getInstance().methods()
        val blocks = mutableListOf<LayoutBlock>()
        blocks.add(HeaderBlock().apply {
            text = PlainTextObject(dataSend.getUtfString("title"), true)
        })
        val context = SectionBlock().apply {
            text = MarkdownTextObject(textBody, false)
        }
        blocks.add(context)

        try {
            client?.chatPostMessage { r ->
                r.token(_gameConfigManager.tokenBotSlack).channel(id).blocks(blocks)
            }
        } catch (e: IOException) {
            controller.logger.error(e)
        }
    }
}