package com.example.jereby.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.jereby.data.model.Player
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
