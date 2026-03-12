package com.senspark.game.api

import com.senspark.common.utils.ILogger
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class PVPConfigsApi(
    envManager: IEnvManager,
    private val _logger: ILogger,
) : IPVPConfigsApi {
    private val _api: IRestApi = OkHttpRestApi()
    private val _url = envManager.apPvpMatchingConfigUrl
    
    override fun getConfig(): PVPConfig {
        try {
            val responseBody = _api.get(_url)
            val code = Json.parseToJsonElement(responseBody).jsonObject["code"].toString().toInt()
            if (code != 0)
                throw CustomException("Cannot get pvp config, code: $code")
            val message = Json.parseToJsonElement(responseBody).jsonObject["message"]
                ?: throw CustomException("Api missing message")
            val zones = message.jsonObject["zones"]
                ?: throw CustomException("Api missing zones config")
            val servers = message.jsonObject["servers"]
                ?: throw CustomException("Api missing servers config")
            val zonesMap = zones.jsonArray.map {
                val id = it.jsonObject["id"] ?: throw CustomException("Missing id")
                val host = it.jsonObject["host"] ?: throw CustomException("Missing host")
                return@map ZoneConfig(id.jsonPrimitive.content, host.jsonPrimitive.content)
            }
            val serverConfigs = servers.jsonArray.map {
                val id = it.jsonObject["id"] ?: throw CustomException("Missing id")
                val zone = it.jsonObject["zone"] ?: throw CustomException("Missing zone")
                val host = it.jsonObject["host"] ?: throw CustomException("Missing host")
                val port = it.jsonObject["port"] ?: throw CustomException("Missing port")
                val useSsl = it.jsonObject["use_ssl"] ?: throw CustomException("Missing useSsl")
                val udpHost = it.jsonObject["udp_host"] ?: throw CustomException("Missing udpHost")
                val udpPort = it.jsonObject["udp_port"] ?: throw CustomException("Missing udpPort")
                return@map ServerConfig(
                    id.jsonPrimitive.content,
                    zone.jsonPrimitive.content,
                    host.jsonPrimitive.content,
                    port.jsonPrimitive.int,
                    useSsl.jsonPrimitive.boolean,
                    udpHost.jsonPrimitive.content,
                    udpPort.jsonPrimitive.int,
                )
            }
            return PVPConfig(zonesMap, serverConfigs)
        } catch (e: Exception) {
            _logger.error("API Fail get server PVP:\n" + e.message)
            return PVPConfig(listOf(), listOf())
        }
    }
}

@Serializable
class PVPConfig(val zones: List<ZoneConfig>, val servers: List<ServerConfig>)

@Serializable
class ZoneConfig(val id: String, val host: String)

@Serializable
class ServerConfig(
    val id: String,
    val zone: String,
    val host: String,
    val port: Int,
    @SerialName("use_ssl") val useSSL: Boolean,
    @SerialName("udp_host") val udpHost: String,
    @SerialName("udp_port") val udpPort: Int,
)