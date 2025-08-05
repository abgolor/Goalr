package com.ajaytechsolutions.goalr.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val username: String,
    val name: String,
    val dailyGoal: Int
)
