package com.ajaytechsolutions.goalr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class GoalrViewModel : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _dailyProgress = MutableStateFlow<List<DailyProgress>>(emptyList())
    val dailyProgress: StateFlow<List<DailyProgress>> = _dailyProgress

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak

    fun saveUser(user: User) {
        _user.value = user
    }

    fun addSteps(steps: Int) {
        val today = LocalDate.now()
        val currentProgress = _dailyProgress.value.toMutableList()

        val index = currentProgress.indexOfFirst { it.date == today }
        if (index >= 0) {
            currentProgress[index] = currentProgress[index].copy(
                steps = steps,
                goalMet = steps >= (_user.value?.dailyGoal ?: 0)
            )
        } else {
            currentProgress.add(
                DailyProgress(
                    date = today,
                    steps = steps,
                    goalMet = steps >= (_user.value?.dailyGoal ?: 0)
                )
            )
        }

        _dailyProgress.value = currentProgress
        updateStreak()
    }

    private fun updateStreak() {
        val sorted = _dailyProgress.value.sortedByDescending { it.date }
        var count = 0
        var day = LocalDate.now()

        for (progress in sorted) {
            if (progress.date == day && progress.goalMet) {
                count++
                day = day.minusDays(1)
            } else break
        }
        _streak.value = count
    }
}
