package com.senspark.game.extension.coroutines
import com.senspark.common.service.IGlobalService
import kotlinx.coroutines.CoroutineScope

/**
 * Service interface for providing reusable CoroutineScope
 */
interface ICoroutineScope : IGlobalService {
    /**
     * The main application CoroutineScope with SupervisorJob
     * Callers can specify which dispatcher to use when launching coroutines
     */
    val scope: CoroutineScope
    
    /**
     * Cancels all coroutines when the service is no longer needed
     */
    fun cancelAllCoroutines()
}
