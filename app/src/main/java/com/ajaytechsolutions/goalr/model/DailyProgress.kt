package com.ajaytechsolutions.goalr.model

import java.time.LocalDate

data class DailyProgress(
    val date: LocalDate,
    val steps: Int,
    val goalMet: Boolean
)