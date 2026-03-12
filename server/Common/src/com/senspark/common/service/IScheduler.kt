package com.senspark.common.service

interface IScheduler : IGlobalService {
    /**
     * Schedules a delayed action.
     * @param key Unique key.
     * @param delay Delay time in milliseconds.
     * @param action Scheduled action.
     */
    fun scheduleOnce(
        key: String,
        delay: Int,
        action: () -> Unit,
    )

    fun fireAndForget(action: () -> Unit)

    /**
     * Schedules a repeated action.
     * @param key Unique key.
     * @param delay Delay time in milliseconds.
     * @param interval Interval time in milliseconds.
     * @param action Scheduled action.
     */
    fun schedule(
        key: String,
        delay: Int,
        interval: Int,
        action: () -> Unit,
    )

    /**
     * Checks whether the specified key is scheduled.
     * @param key The desired key.
     */
    fun isScheduled(key: String): Boolean

    /**
     * Clears a scheduled action.
     * @param key The associated key.
     */
    fun clear(key: String)

    /**
     * Clears all scheduled actions.
     */
    fun clearAll()
}