package com.example.jereby.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jereby.data.model.PlayerStats
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: PlayerStats)

    @Query("SELECT * FROM PlayerStats WHERE tournamentId=:tid")
    fun byTournament(tid: Long): Flow<List<PlayerStats>>

    @Query("SELECT * FROM PlayerStats WHERE tournamentId=:tid AND playerId=:pid")
    suspend fun get(tid: Long, pid: Long): PlayerStats?

    @Query("DELETE FROM PlayerStats WHERE tournamentId=:tid")
    suspend fun deleteByTournament(tid: Long)

}