package com.ajaytechsolutions.goalr.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

/**
 * Simplified step counting manager using device-native step sensors with minimal fallback.
 * Uses Room database for persistence and handles device reboots by starting with saved steps.
 *
 * Key features:
 * - Prioritizes Sensor.TYPE_STEP_DETECTOR for accurate step detection
 * - Falls back to Sensor.TYPE_STEP_COUNTER with reboot handling
 * - Basic accelerometer fallback for devices without step sensors
 * - Initializes daily steps from Room database
 * - Handles device reboots to maintain step count continuity
 *
 * @param context Android application context
 */
class StepCounterManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Sensors prioritized by accuracy
    private val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Step counting state
    private var dailySteps = 0
    private var sessionStartSteps = 0
    private var lastKnownHardwareCount = 0L
    private var currentDate: LocalDate = LocalDate.now()
    private var isActive = false

    // Basic accelerometer detection variables
    private val accelBuffer = mutableListOf<Float>()
    private var lastStepTime = 0L

    // Detection parameters
    private val STEP_THRESHOLD = 10.0f // Simple threshold for accelerometer
    private val MIN_STEP_INTERVAL = 250L // Minimum time between steps (ms)
    private val BUFFER_SIZE = 20 // Size of accelerometer buffer

    // Public state flows
    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Starts step tracking with an initial step count from the database
     * @param initialSteps Steps loaded from the Room database for the current day
     */
    fun startTracking(initialSteps: Int) {
        if (isActive) {
            android.util.Log.d("StepCounter", "Already tracking, ignoring start request")
            return
        }

        isActive = true
        dailySteps = initialSteps // Initialize with database steps
        checkDateRollover()

        // Register sensors in order of preference
        when {
            stepDetectorSensor != null -> {
                sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
                android.util.Log.i("StepCounter", "Using hardware step detector")
            }
            stepCounterSensor != null -> {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
                initializeHardwareStepCounter()
                android.util.Log.i("StepCounter", "Using hardware step counter")
            }
            accelerometer != null -> {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                android.util.Log.i("StepCounter", "Using accelerometer-based detection")
            }
            else -> {
                android.util.Log.w("StepCounter", "No suitable sensors available")
                return
            }
        }

        sessionStartSteps = dailySteps
        _isTracking.value = true
        _stepsToday.value = dailySteps

        android.util.Log.i("StepCounter", "Step tracking started - Initial steps: $dailySteps")
    }

    /**
     * Initialize hardware step counter baseline
     */
    private fun initializeHardwareStepCounter() {
        stepCounterSensor?.let { sensor ->
            sensorManager.registerListener(object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        lastKnownHardwareCount = it.values[0].toLong()
                        sensorManager.unregisterListener(this)
                        android.util.Log.d("StepCounter", "Hardware baseline set: $lastKnownHardwareCount")
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isActive || event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> handleStepDetector(event)
            Sensor.TYPE_STEP_COUNTER -> handleStepCounter(event)
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometerData(event)
        }

        checkDateRollover()
    }

    /**
     * Handle step detector sensor (most accurate)
     */
    private fun handleStepDetector(event: SensorEvent) {
        val steps = event.values[0].toInt() // Each event represents one step
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastStepTime >= MIN_STEP_INTERVAL) {
            repeat(steps) {
                registerStep(currentTime)
            }
        }
    }

    /**
     * Handle hardware step counter (cumulative since boot)
     */
    private fun handleStepCounter(event: SensorEvent) {
        val currentHardwareCount = event.values[0].toLong()

        // Detect reboot (hardware count resets to a lower value)
        if (currentHardwareCount < lastKnownHardwareCount) {
            android.util.Log.i("StepCounter", "Device reboot detected. Resetting hardware baseline from $lastKnownHardwareCount to 0")
            lastKnownHardwareCount = 0L // Reset baseline, keep dailySteps from database
        }

        if (lastKnownHardwareCount >= 0) {
            val stepIncrement = (currentHardwareCount - lastKnownHardwareCount).toInt()
            if (stepIncrement in 1..10) { // Reasonable limit to avoid large jumps
                val currentTime = System.currentTimeMillis()
                repeat(stepIncrement) {
                    registerStep(currentTime)
                }
            } else if (stepIncrement > 10) {
                android.util.Log.w("StepCounter", "Large step increment ignored: $stepIncrement")
            }
        }

        lastKnownHardwareCount = currentHardwareCount
    }

    /**
     * Basic accelerometer-based step detection
     */
    private fun handleAccelerometerData(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate magnitude
        val magnitude = sqrt(x * x + y * y + z * z)
        accelBuffer.add(magnitude)

        if (accelBuffer.size > BUFFER_SIZE) {
            accelBuffer.removeAt(0)
        }

        if (accelBuffer.size >= 3) {
            val current = accelBuffer.last()
            val previous = accelBuffer[accelBuffer.size - 2]
            val beforePrevious = accelBuffer[accelBuffer.size - 3]
            val currentTime = System.currentTimeMillis()

            // Simple peak detection
            val isPeak = current > STEP_THRESHOLD &&
                    current > previous &&
                    previous > beforePrevious &&
                    currentTime - lastStepTime >= MIN_STEP_INTERVAL

            if (isPeak) {
                registerStep(currentTime)
            }
        }
    }

    /**
     * Register a validated step
     */
    private fun registerStep(timestamp: Long) {
        dailySteps++
        lastStepTime = timestamp

        handler.post {
            _stepsToday.value = dailySteps
        }

        android.util.Log.d("StepCounter", "Registered step #$dailySteps at $timestamp")
    }

    /**
     * Check for date changes and reset daily count
     */
    private fun checkDateRollover() {
        val today = LocalDate.now()
        if (today != currentDate) {
            android.util.Log.i("StepCounter", "Date rollover: $currentDate -> $today")
            currentDate = today
            dailySteps = 0 // Will be reinitialized by service on next start
            lastStepTime = 0L
            accelBuffer.clear()
            lastKnownHardwareCount = 0L // Reset hardware count for new day
            _stepsToday.value = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        android.util.Log.d("StepCounter", "Sensor accuracy changed: ${sensor?.name} = $accuracy")
    }

    /**
     * Stop tracking and cleanup
     */
    fun stopTracking() {
        if (!isActive) return

        isActive = false
        sensorManager.unregisterListener(this)
        _isTracking.value = false

        android.util.Log.i("StepCounter", "Tracking stopped. Final count: $dailySteps")
    }

    // Public API
    fun getCurrentSteps(): Int = dailySteps
    fun getSessionSteps(): Int = dailySteps - sessionStartSteps

    fun getDetectionInfo(): String {
        return buildString {
            appendLine("=== Step Detection Info ===")
            appendLine("Steps Today: $dailySteps")
            appendLine("Session Steps: ${getSessionSteps()}")
            appendLine("Is Tracking: $isActive")
            appendLine("Current Date: $currentDate")
            appendLine("Last Hardware Count: $lastKnownHardwareCount")
            appendLine()
            appendLine("Available Sensors:")
            appendLine("• Step Detector: ${stepDetectorSensor != null}")
            appendLine("• Step Counter: ${stepCounterSensor != null}")
            appendLine("• Accelerometer: ${accelerometer != null}")
            appendLine()
            appendLine("Last Step Time: ${if (lastStepTime > 0) "${System.currentTimeMillis() - lastStepTime}ms ago" else "N/A"}")
        }
    }

    fun resetDailySteps() {
        dailySteps = 0
        sessionStartSteps = 0
        accelBuffer.clear()
        lastKnownHardwareCount = 0L
        _stepsToday.value = 0
        android.util.Log.i("StepCounter", "Daily steps reset manually")
    }
}