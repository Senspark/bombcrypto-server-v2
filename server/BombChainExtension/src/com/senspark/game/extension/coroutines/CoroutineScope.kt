package com.senspark.game.extension.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Implementation of ICoroutineScopeService providing a reusable CoroutineScope
 * that can be used with different dispatchers
 */
class CoroutineScope : ICoroutineScope {
    /**
     * The main application CoroutineScope with SupervisorJob
     * Clients should specify which dispatcher to use when launching coroutines
     */
    override val scope = CoroutineScope(SupervisorJob())
    
    /**
     * Cancels all coroutines when the service is no longer needed
     */
    override fun cancelAllCoroutines() {
        scope.cancel("Service shutdown")
    }

    override fun initialize() {
    }
}
