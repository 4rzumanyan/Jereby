package com.example.jereby.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.example.jereby.data.Club
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubDao {

    @Query("SELECT * FROM Club ORDER BY name")
    fun all(): Flow<List<Club>>

    @Insert(onConflict = REPLACE)
    suspend fun upsertAll(clubs: List<Club>)

    @Query("SELECT * FROM Club")
    suspend fun allOnce(): List<Club>

}
