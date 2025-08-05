package com.ajaytechsolutions.goalr.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object StepTracker {
    // SharedFlow to broadcast step updates to ViewModels and UI
    private val _stepUpdates = MutableSharedFlow<Int>(replay = 1)
    val stepUpdates = _stepUpdates.asSharedFlow()

    fun emitSteps(steps: Int) {
        _stepUpdates.tryEmit(steps)
    }
}
