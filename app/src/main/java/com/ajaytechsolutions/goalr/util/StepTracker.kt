package com.ajaytechsolutions.goalr.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Centralized step tracking utility for communication between
 * the foreground service and UI components
 */
object StepTracker {

    private val _stepUpdates = MutableSharedFlow<Int>(
        replay = 1, // Keep last value for new subscribers
        extraBufferCapacity = 0
    )

    /**
     * Shared flow for step updates - UI components can collect from this
     */
    val stepUpdates: SharedFlow<Int> = _stepUpdates.asSharedFlow()

    /**
     * Emits new step count - called by the foreground service
     */
    fun emitSteps(steps: Int) {
        try {
            _stepUpdates.tryEmit(steps)
            android.util.Log.d("StepTracker", "Emitted steps: $steps")
        } catch (e: Exception) {
            android.util.Log.e("StepTracker", "Failed to emit steps", e)
        }
    }

    /**
     * Gets the last emitted step count (if any)
     */
    fun getLastStepCount(): Int? {
        return try {
            _stepUpdates.replayCache.lastOrNull()
        } catch (e: Exception) {
            android.util.Log.e("StepTracker", "Failed to get last step count", e)
            null
        }
    }
}