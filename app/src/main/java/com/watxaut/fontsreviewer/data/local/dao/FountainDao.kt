package com.watxaut.fontsreviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.watxaut.fontsreviewer.data.local.entity.FountainEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FountainDao {

    @Query("SELECT * FROM fountains")
    fun getAllFountains(): Flow<List<FountainEntity>>

    @Query("SELECT * FROM fountains WHERE codi = :codi")
    suspend fun getFountainByCodi(codi: String): FountainEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fountains: List<FountainEntity>)

    @Query("SELECT COUNT(*) FROM fountains")
    suspend fun getFountainCount(): Int

    @Query("DELETE FROM fountains")
    suspend fun deleteAll()
}
