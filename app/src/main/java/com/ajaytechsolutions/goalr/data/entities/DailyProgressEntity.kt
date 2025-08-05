package com.ajaytechsolutions.goalr.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "daily_progress")
data class DailyProgressEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val steps: Int,
    val goalMet: Boolean
)
