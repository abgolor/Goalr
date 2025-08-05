package com.ajaytechsolutions.goalr.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ajaytechsolutions.goalr.data.GoalrDatabase
import com.ajaytechsolutions.goalr.data.entities.DailyProgressEntity
import com.ajaytechsolutions.goalr.data.entities.UserEntity
import com.ajaytechsolutions.goalr.util.StepTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GoalrViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val dao = GoalrDatabase.getDatabase(application).goalrDao()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // --- State for UI ---
    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> get() = _user

    private val _currentSteps = MutableStateFlow(0)
    val currentSteps: StateFlow<Int> get() = _currentSteps

    private val _weeklyProgress = MutableStateFlow<List<DailyProgressEntity>>(emptyList())
    val weeklyProgress: StateFlow<List<DailyProgressEntity>> get() = _weeklyProgress

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> get() = _streak

    private val _goalProgress = MutableStateFlow(0f)
    val goalProgress: StateFlow<Float> get() = _goalProgress

    // --- Selected date for weekly chart ---
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> get() = _selectedDate

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadUser()
            loadWeeklyProgress()
            calculateStreak()
            loadTodaySteps()
        }

        // 🔹 Listen for step updates globally
        viewModelScope.launch {
            StepTracker.stepUpdates.collect { steps ->
                onStepsUpdated(steps)
            }
        }
    }

    // -------------------------
    // USER HANDLING
    // -------------------------
    fun saveUser(user: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.saveUser(user)
            _user.value = user
            updateGoalProgress()
        }
    }

    private suspend fun loadUser() {
        _user.value = dao.getUser()
        updateGoalProgress()
    }

    // -------------------------
    // STEP UPDATES
    // -------------------------
    fun onStepsUpdated(stepsToday: Int) {
        _currentSteps.value = stepsToday
        updateGoalProgress()

        // Refresh weekly progress and streak when steps update
        viewModelScope.launch(Dispatchers.IO) {
            loadWeeklyProgress()
            calculateStreak()
        }
    }

    private suspend fun loadTodaySteps() {
        val today = dateFormatter.format(Date())
        val todayProgress = dao.getDailyProgress(today)
        todayProgress?.let {
            _currentSteps.value = it.steps
            updateGoalProgress()
        }
    }

    private fun updateGoalProgress() {
        val userGoal = _user.value?.dailyGoal ?: 4000
        val progress = if (userGoal > 0) {
            (_currentSteps.value.toFloat() / userGoal.toFloat()).coerceAtMost(1f)
        } else {
            0f
        }
        _goalProgress.value = progress
    }

    private fun loadWeeklyProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            val last7 = dao.getLast7Days().reversed()
            _weeklyProgress.value = last7
        }
    }

    // -------------------------
    // STREAK CALCULATION
    // -------------------------
    private fun calculateStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            val progressList = dao.getLast7Days().sortedByDescending { it.date }
            var streakCount = 0
            val calendar = Calendar.getInstance()

            for (p in progressList) {
                val expectedDate = dateFormatter.format(calendar.time)
                if (p.date == expectedDate && p.goalMet) {
                    streakCount++
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
            _streak.value = streakCount
        }
    }

    // -------------------------
    // DATE SELECTION FOR UI
    // -------------------------
    fun onDateSelected(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }

    // -------------------------
    // UTILITY FUNCTIONS
    // -------------------------
    fun isGoalMet(): Boolean {
        val userGoal = _user.value?.dailyGoal ?: 4000
        return _currentSteps.value >= userGoal
    }

    fun getStepsRemaining(): Int {
        val userGoal = _user.value?.dailyGoal ?: 4000
        return maxOf(0, userGoal - _currentSteps.value)
    }
}
