package com.example.jereby.data.dao

import androidx.room.*
import com.example.jereby.data.PlayerStats
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(stat: PlayerStats)
    @Query("SELECT * FROM PlayerStats WHERE tournamentId=:tid")
    fun byTournament(tid: Long): Flow<List<PlayerStats>>

    @Query("SELECT * FROM PlayerStats WHERE tournamentId=:tid AND playerId=:pid")
    suspend fun get(tid: Long, pid: Long): PlayerStats?

}