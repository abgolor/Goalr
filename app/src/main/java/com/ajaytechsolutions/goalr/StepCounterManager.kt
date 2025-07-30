package com.ajaytechsolutions.goalr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.LocalDate

class StepCounterManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var initialSteps = -1
    private var lastResetDate: LocalDate = LocalDate.now()

    private val _stepsToday = MutableLiveData(0)
    val stepsToday: LiveData<Int> get() = _stepsToday

    fun startTracking() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopTracking() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val currentSteps = it.values[0].toInt()

            // Reset daily steps at midnight
            if (LocalDate.now() != lastResetDate) {
                lastResetDate = LocalDate.now()
                initialSteps = currentSteps
            }

            if (initialSteps < 0) {
                initialSteps = currentSteps
            }

            _stepsToday.postValue(currentSteps - initialSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
