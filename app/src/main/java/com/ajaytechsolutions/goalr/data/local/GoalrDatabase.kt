package com.ajaytechsolutions.goalr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ajaytechsolutions.goalr.data.entities.DailyProgressEntity
import com.ajaytechsolutions.goalr.data.entities.UserEntity

@Database(entities = [UserEntity::class, DailyProgressEntity::class], version = 1)
abstract class GoalrDatabase : RoomDatabase() {
    abstract fun goalrDao(): GoalrDao

    companion object {
        @Volatile
        private var INSTANCE: GoalrDatabase? = null

        fun getDatabase(context: Context): GoalrDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoalrDatabase::class.java,
                    "goalr_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
