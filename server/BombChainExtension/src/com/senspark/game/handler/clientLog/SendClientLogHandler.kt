package com.senspark.game.handler.clientLog

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.ISFSArray
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

enum class ClientLogType {
    INFO,
    WARNING,
    ERROR
}
// Handler này chỉ để log các message của client chỉ định lên xem, ko cần response về
class SendClientLogHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SEND_CLIENT_LOG
    private val Tag = "[CLIENT_LOG]"

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val userManager = controller.svServices.get<IUsersManager>()
            // Client này ko đc set bởi admin command trước nên sẽ ko log
            if(!userManager.isClientLoggingEnabled(controller.userId))
                return;
            val basePath = services.get<IEnvManager>().saveClientLogPath
            
            val logArray = data.getSFSArray("data")
            
            // Process logs
            handleLogs(controller, logArray)
            
            // Save logs to file if basePath is not empty
            if (basePath.isEmpty()) {
                controller.logger.warn("$Tag [SERVER] Save path is empty, only logging to server logs.")
            } else {
                handleSaveToFile(controller, basePath, logArray)
            }
        } catch (e: Exception) {
            controller.logger.error("$Tag [SERVER] ${controller.userId} - ${controller.userName}: ", e)
        }
    }
    
    /**
     * Handles logging messages to the server logger
     * 
     * @param controller The user controller
     * @param logArray Array of logs to process
     */
    private fun handleLogs(controller: IUserController, logArray: ISFSArray) {
        val logger = controller.logger
        
        for (i in 0 until logArray.size()) {
            val logData = logArray.getSFSObject(i)
            val typeInt = logData.getInt("type")
            val type = ClientLogType.entries[typeInt]
            val message = "$Tag [${controller.userName}] ${logData.getUtfString("message")}"
            
            // Log to server logger
            when (type) {
                ClientLogType.INFO -> logger.log(message)
                ClientLogType.WARNING -> logger.warn(message)
                ClientLogType.ERROR -> logger.error(message)
            }
        }
    }
    
    /**
     * Handles saving logs to a file
     * 
     * @param controller The user controller
     * @param basePath The base path where to save the log file
     * @param logArray Array of logs to save
     */
    private fun handleSaveToFile(controller: IUserController, basePath: String, logArray: ISFSArray) {
        val dateFormat = SimpleDateFormat("MM-yyyy")
        val currentDate = Date()
        val monthYear = dateFormat.format(currentDate)
        val fileName = "[${controller.userId}] _ [${controller.userName}] _ [${monthYear}].txt"
        val filePath = "$basePath/${controller.dataType}/$fileName"
        
        // Get absolute file path for clearer debugging
        val directory = File("$basePath/${controller.dataType}")
        val absoluteFilePath = File(filePath).absolutePath
        
        controller.logger.log("$Tag [SERVER] [${controller.userId}] Save file to: $absoluteFilePath")
        
        // Create directory if it doesn't exist
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        // Prepare to write logs to file
        val file = File(filePath)
        val append = file.exists()
        
        try {
            // Use PrintWriter with FileWriter(append=true) to append to existing file
            PrintWriter(FileWriter(file, append)).use { writer ->
                // Add a header if we're creating a new file
                if (!append) {
                    writer.println("=== Client Log for User: ${controller.userName} (ID: ${controller.userId}) ===")
                    writer.println("=== Started on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())} ===")
                    writer.println("========================================================")
                }
                
                // Process and write each log entry to file
                for (i in 0 until logArray.size()) {
                    val logData = logArray.getSFSObject(i)
                    val typeInt = logData.getInt("type")
                    val type = ClientLogType.entries[typeInt]
                    val message = logData.getUtfString("message")
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                    
                    // Format the log message
                    val logEntry = "[$timestamp] [${type.name}] $message"
                    
                    // Write to file
                    writer.println(logEntry)
                }
                
                // Add separator between log sessions when appending
                if (append) {
                    writer.println("------")
                }
            }
        } catch (e: Exception) {
            controller.logger.error("$Tag [SERVER] [${controller.userId}] handleSaveToFile: ERROR writing to file: ${e.message}")
            controller.logger.error("$Tag [SERVER] [${controller.userId}] handleSaveToFile: File path attempted: $absoluteFilePath")
            throw e
        }
    }
}