package com.watxaut.fontsreviewer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.watxaut.fontsreviewer.data.local.dao.FountainDao
import com.watxaut.fontsreviewer.data.local.entity.FountainEntity

@Database(
    entities = [FountainEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fountainDao(): FountainDao

    companion object {
        const val DATABASE_NAME = "fonts_reviewer_db"
    }
}
