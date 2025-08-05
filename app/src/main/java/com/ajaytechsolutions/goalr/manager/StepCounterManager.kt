package com.ajaytechsolutions.goalr.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sqrt

class StepCounterManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Hardware step sensor (preferred)
    private val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    // REQUIRED validation sensors for anti-cheating
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var currentSteps = 0
    private var currentDate: LocalDate = LocalDate.now()

    // Detection mode
    private var usingStepDetector = false
    private var usingAccelerometer = false

    // Ultra-strict validation variables
    private var lastStepTime = 0L
    private var lastMagnitude = 0.0
    private val stepThreshold = 2.0f
    private val minStepInterval = 400L // Stricter minimum
    private val maxStepInterval = 2000L

    // Advanced anti-cheating detection
    private val recentStepTimes = mutableListOf<Long>()
    private val maxRecentSteps = 6
    private var consecutiveRapidSteps = 0
    private var suspiciousPatternCount = 0

    // Multi-sensor validation
    private var lastGravity = floatArrayOf(0f, 0f, 9.81f)
    private var lastAcceleration = floatArrayOf(0f, 0f, 0f)
    private var lastGyroscope = floatArrayOf(0f, 0f, 0f)
    private var lastGravityTime = 0L
    private var lastAccelTime = 0L
    private var lastGyroTime = 0L

    // Sophisticated pattern analysis
    private var deviceVerticalStability = 0.0
    private val stabilityWindow = mutableListOf<Double>()
    private val rotationWindow = mutableListOf<Double>()
    private val verticalAccelWindow = mutableListOf<Double>()

    // Walking confidence scoring
    private var walkingConfidence = 0.0
    private var stationaryTime = 0L
    private var lastSignificantMovement = 0L

    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    fun startTracking(initialSteps: Int = 0) {
        currentSteps = initialSteps
        resetValidationVariables()
        startSensorListening()
        _stepsToday.value = currentSteps
        android.util.Log.d("StepCounter", "Ultra-strict tracking started with initial steps: $initialSteps")
    }

    fun startTrackingAfterReboot(initialSteps: Int = 0) {
        currentSteps = initialSteps
        resetValidationVariables()
        startSensorListening()
        _stepsToday.value = currentSteps
        android.util.Log.d("StepCounter", "Ultra-strict tracking started after reboot with initial steps: $initialSteps")
    }

    private fun resetValidationVariables() {
        lastStepTime = 0L
        lastMagnitude = 0.0
        recentStepTimes.clear()
        consecutiveRapidSteps = 0
        suspiciousPatternCount = 0
        lastGravity = floatArrayOf(0f, 0f, 9.81f)
        lastAcceleration = floatArrayOf(0f, 0f, 0f)
        lastGyroscope = floatArrayOf(0f, 0f, 0f)
        lastGravityTime = 0L
        lastAccelTime = 0L
        lastGyroTime = 0L
        deviceVerticalStability = 0.0
        stabilityWindow.clear()
        rotationWindow.clear()
        verticalAccelWindow.clear()
        walkingConfidence = 0.0
        stationaryTime = 0L
        lastSignificantMovement = 0L
    }

    private fun startSensorListening() {
        if (stepDetectorSensor != null && accelerometer != null && gravitySensor != null) {
            usingStepDetector = true
            usingAccelerometer = false

            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }

            android.util.Log.d("StepCounter", "Using STEP_DETECTOR with ULTRA-STRICT multi-sensor validation")
        } else if (accelerometer != null && gravitySensor != null) {
            usingStepDetector = false
            usingAccelerometer = true

            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }

            android.util.Log.d("StepCounter", "Using accelerometer with ultra-strict validation")
        } else {
            android.util.Log.e("StepCounter", "Required sensors not available - cannot prevent cheating!")
        }
    }

    fun stopTracking() {
        sensorManager.unregisterListener(this)
        android.util.Log.d("StepCounter", "Ultra-strict tracking stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_STEP_DETECTOR -> handleStepDetector(event)
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GRAVITY -> handleGravity(event)
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event)
        }
    }

    private fun handleGravity(event: SensorEvent) {
        lastGravity = event.values.clone()
        lastGravityTime = System.currentTimeMillis()
        updateDeviceStability()
    }

    private fun handleGyroscope(event: SensorEvent) {
        lastGyroscope = event.values.clone()
        lastGyroTime = System.currentTimeMillis()
        updateRotationAnalysis()
    }

    private fun handleStepDetector(event: SensorEvent) {
        val today = LocalDate.now()
        if (today != currentDate) {
            handleDateChange(today)
            return
        }

        val now = System.currentTimeMillis()
        val timeSinceLastStep = now - lastStepTime

        // Basic rapid-fire filter
        if (timeSinceLastStep < 250L && lastStepTime > 0) {
            android.util.Log.d("StepCounter", "Filtered rapid step event: ${timeSinceLastStep}ms")
            return
        }

        // ULTRA-STRICT validation - must pass ALL checks
        val validationResult = performUltraStrictValidation(now, timeSinceLastStep)
        if (!validationResult.isValid) {
            android.util.Log.d("StepCounter", "REJECTED: ${validationResult.reason}")
            suspiciousPatternCount++

            // If too many rejections, increase scrutiny
            if (suspiciousPatternCount > 5) {
                android.util.Log.w("StepCounter", "High suspicious activity detected - increasing validation strictness")
            }
            return
        }

        // Step accepted - update confidence and counters
        currentSteps++
        _stepsToday.value = currentSteps
        lastStepTime = now
        lastSignificantMovement = now

        recentStepTimes.add(now)
        if (recentStepTimes.size > maxRecentSteps) {
            recentStepTimes.removeAt(0)
        }

        consecutiveRapidSteps = 0
        walkingConfidence = minOf(1.0, walkingConfidence + 0.2)

        // Reduce suspicion on valid steps
        suspiciousPatternCount = maxOf(0, suspiciousPatternCount - 1)

        android.util.Log.d("StepCounter", "VALID STEP: $currentSteps (confidence: $walkingConfidence, interval: ${timeSinceLastStep}ms)")
    }

    private fun handleAccelerometer(event: SensorEvent) {
        lastAcceleration = event.values.clone()
        lastAccelTime = System.currentTimeMillis()
        updateVerticalAccelAnalysis()
        updateMovementDetection()

        // Only used as fallback when no hardware step detector is available
        if (usingStepDetector) return

        val today = LocalDate.now()
        if (today != currentDate) {
            handleDateChange(today)
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())

        val now = System.currentTimeMillis()
        val timeSinceLastStep = now - lastStepTime
        val magnitudeChange = magnitude - lastMagnitude

        val validationResult = performUltraStrictValidation(now, timeSinceLastStep)

        if (magnitudeChange > stepThreshold &&
            timeSinceLastStep > minStepInterval &&
            magnitude > 8.0 &&
            validationResult.isValid) {

            currentSteps++
            _stepsToday.value = currentSteps
            lastStepTime = now
            recentStepTimes.add(now)
            if (recentStepTimes.size > maxRecentSteps) {
                recentStepTimes.removeAt(0)
            }

            android.util.Log.d("StepCounter", "Accelerometer step: $currentSteps (magnitude: $magnitude)")
        }

        lastMagnitude = magnitude
    }

    data class ValidationResult(val isValid: Boolean, val reason: String)

    private fun performUltraStrictValidation(currentTime: Long, timeSinceLastStep: Long): ValidationResult {
        val now = System.currentTimeMillis()

        // 1. Device orientation - VERY strict
        val gravityZ = abs(lastGravity[2])
        val gravityMagnitude = sqrt((lastGravity[0] * lastGravity[0] +
                lastGravity[1] * lastGravity[1] +
                lastGravity[2] * lastGravity[2]).toDouble())

        if (gravityMagnitude > 6.0 && gravityZ < 7.5 && lastGravityTime > 0) {
            return ValidationResult(false, "Device not sufficiently vertical (gravityZ=$gravityZ)")
        }

        // 2. Device stability - much stricter
        if (deviceVerticalStability > 0.8 && stabilityWindow.size >= 3) {
            return ValidationResult(false, "Device too unstable (stability=$deviceVerticalStability)")
        }

        // 3. Rotation analysis - detect hand swinging
        if (rotationWindow.isNotEmpty()) {
            val avgRotation = rotationWindow.average()
            if (avgRotation > 1.5) {
                return ValidationResult(false, "Excessive rotation detected (rotation=$avgRotation)")
            }
        }

        // 4. Vertical acceleration pattern
        if (verticalAccelWindow.size >= 3) {
            val verticalVariation = verticalAccelWindow.map { abs(it - verticalAccelWindow.average()) }.average()
            if (verticalVariation < 0.5) {
                return ValidationResult(false, "Unnatural vertical acceleration pattern")
            }
        }

        // 5. Overall acceleration - much stricter
        if (lastAccelTime > 0 && (now - lastAccelTime) < 1000) {
            val totalAccel = sqrt((lastAcceleration[0] * lastAcceleration[0] +
                    lastAcceleration[1] * lastAcceleration[1] +
                    lastAcceleration[2] * lastAcceleration[2]).toDouble())

            if (totalAccel > 15.0) { // Much lower threshold
                return ValidationResult(false, "Excessive acceleration ($totalAccel)")
            }

            if (totalAccel < 8.5) { // Must have minimum movement
                return ValidationResult(false, "Insufficient acceleration for walking ($totalAccel)")
            }
        }

        // 6. Step interval pattern analysis - much stricter
        if (recentStepTimes.size >= 4) {
            val intervals = recentStepTimes.zipWithNext { a, b -> b - a }
            val avgInterval = intervals.average()
            val intervalVariation = intervals.map { abs(it - avgInterval) }.average()

            // Natural walking has some variation
            if (intervalVariation < 80 && avgInterval < 600) {
                return ValidationResult(false, "Unnatural step rhythm (variation=$intervalVariation, avg=$avgInterval)")
            }
        }

        // 7. Walking confidence check
        if (walkingConfidence < 0.3 && recentStepTimes.size > 2) {
            return ValidationResult(false, "Low walking confidence ($walkingConfidence)")
        }

        // 8. Stationary detection
        val timeSinceMovement = now - lastSignificantMovement
        if (timeSinceMovement > 3000 && stationaryTime > 5000) {
            return ValidationResult(false, "Device appears stationary for too long")
        }

        // 9. Suspicious pattern detection
        if (suspiciousPatternCount > 3) {
            // Require longer intervals when suspicious
            if (timeSinceLastStep < 500L) {
                return ValidationResult(false, "Suspicious pattern - requiring longer intervals")
            }
        }

        return ValidationResult(true, "All validations passed")
    }

    private fun updateDeviceStability() {
        val gravityMagnitude = sqrt((lastGravity[0] * lastGravity[0] +
                lastGravity[1] * lastGravity[1] +
                lastGravity[2] * lastGravity[2]).toDouble())

        stabilityWindow.add(gravityMagnitude)
        if (stabilityWindow.size > 5) {
            stabilityWindow.removeAt(0)
        }

        if (stabilityWindow.size >= 3) {
            val variance = stabilityWindow.map { (it - stabilityWindow.average()).let { diff -> diff * diff } }.average()
            deviceVerticalStability = variance
        }
    }

    private fun updateRotationAnalysis() {
        val rotationMagnitude = sqrt((lastGyroscope[0] * lastGyroscope[0] +
                lastGyroscope[1] * lastGyroscope[1] +
                lastGyroscope[2] * lastGyroscope[2]).toDouble())

        rotationWindow.add(rotationMagnitude)
        if (rotationWindow.size > 4) {
            rotationWindow.removeAt(0)
        }
    }

    private fun updateVerticalAccelAnalysis() {
        // Project acceleration onto gravity vector for vertical component
        val gravityMagnitude = sqrt((lastGravity[0] * lastGravity[0] +
                lastGravity[1] * lastGravity[1] +
                lastGravity[2] * lastGravity[2]).toDouble())

        if (gravityMagnitude > 0) {
            val dotProduct = (lastAcceleration[0] * lastGravity[0] +
                    lastAcceleration[1] * lastGravity[1] +
                    lastAcceleration[2] * lastGravity[2]).toDouble()
            val verticalAccel = abs(dotProduct / gravityMagnitude)

            verticalAccelWindow.add(verticalAccel)
            if (verticalAccelWindow.size > 4) {
                verticalAccelWindow.removeAt(0)
            }
        }
    }

    private fun updateMovementDetection() {
        val now = System.currentTimeMillis()
        val totalAccel = sqrt((lastAcceleration[0] * lastAcceleration[0] +
                lastAcceleration[1] * lastAcceleration[1] +
                lastAcceleration[2] * lastAcceleration[2]).toDouble())

        if (totalAccel > 11.0) { // Significant movement threshold
            lastSignificantMovement = now
            stationaryTime = 0
            walkingConfidence = minOf(1.0, walkingConfidence + 0.1)
        } else {
            stationaryTime = now - lastSignificantMovement
            if (stationaryTime > 2000) {
                walkingConfidence = maxOf(0.0, walkingConfidence - 0.1)
            }
        }
    }

    private fun handleDateChange(newDate: LocalDate) {
        android.util.Log.d("StepCounter", "Date changed from $currentDate to $newDate")
        currentDate = newDate
        currentSteps = 0
        resetValidationVariables()
        _stepsToday.value = 0
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        android.util.Log.d("StepCounter", "Sensor accuracy changed: ${sensor?.name} = $accuracy")
    }

    fun getSensorInfo(): String {
        val sb = StringBuilder()
        sb.append("=== ULTRA-STRICT Step Counter ===\n")
        sb.append("Step Detector: ${stepDetectorSensor != null}\n")
        sb.append("Accelerometer: ${accelerometer != null}\n")
        sb.append("Gravity Sensor: ${gravitySensor != null}\n")
        sb.append("Gyroscope: ${gyroscope != null}\n")
        sb.append("\nUsing Step Detector: $usingStepDetector\n")
        sb.append("Steps Today: $currentSteps\n")
        sb.append("Walking Confidence: ${"%.2f".format(walkingConfidence)}\n")
        sb.append("Suspicious Patterns: $suspiciousPatternCount\n")
        sb.append("Device Stability: ${"%.3f".format(deviceVerticalStability)}\n")
        sb.append("Stationary Time: ${stationaryTime}ms\n")
        sb.append("Gravity Vector: ${lastGravity.joinToString { "%.2f".format(it) }}\n")
        return sb.toString()
    }

    fun isUsingHardwareSensors(): Boolean = usingStepDetector

    fun getCurrentDetectionMethod(): String {
        return when {
            usingStepDetector -> "Ultra-Strict Hardware Step Detector"
            usingAccelerometer -> "Ultra-Strict Accelerometer"
            else -> "No sensors available"
        }
    }

    fun resetSteps() {
        currentSteps = 0
        resetValidationVariables()
        _stepsToday.value = 0
        android.util.Log.d("StepCounter", "Steps manually reset")
    }

    fun addSteps(steps: Int) {
        currentSteps += steps
        _stepsToday.value = currentSteps
        android.util.Log.d("StepCounter", "Added $steps steps manually. Total: $currentSteps")
    }
}