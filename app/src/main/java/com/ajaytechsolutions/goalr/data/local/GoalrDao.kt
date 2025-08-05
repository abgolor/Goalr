package com.ajaytechsolutions.goalr.data

import androidx.room.*
import com.ajaytechsolutions.goalr.data.entities.DailyProgressEntity
import com.ajaytechsolutions.goalr.data.entities.UserEntity

@Dao
interface GoalrDao {

    // User
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: UserEntity)

    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getUser(): UserEntity?

    // Daily Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyProgress(progress: DailyProgressEntity)

    @Query("SELECT * FROM daily_progress WHERE date = :date LIMIT 1")
    suspend fun getDailyProgress(date: String): DailyProgressEntity?

    @Query("SELECT * FROM daily_progress ORDER BY date DESC LIMIT 7")
    suspend fun getLast7Days(): List<DailyProgressEntity>
}
