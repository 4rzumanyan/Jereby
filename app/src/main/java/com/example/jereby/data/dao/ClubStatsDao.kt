package com.example.jereby.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jereby.data.ClubStats
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: ClubStats)

    @Query("SELECT * FROM ClubStats WHERE tournamentId=:tid")
    fun byTournament(tid: Long): Flow<List<ClubStats>>

    @Query("SELECT * FROM ClubStats WHERE tournamentId=:tid AND clubId=:cid")
    suspend fun get(tid: Long, cid: String): ClubStats?

}