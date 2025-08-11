package com.example.jereby.data.dao

import androidx.room.*
import com.example.jereby.data.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Insert
    suspend fun insertAll(players: List<Player>): List<Long>

    @Query("SELECT * FROM Player")
    suspend fun allOnce(): List<Player>

    @Query("SELECT * FROM Player")
    fun allFlow(): Flow<List<Player>>

}
