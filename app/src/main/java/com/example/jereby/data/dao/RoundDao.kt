package com.example.jereby.data.dao

import androidx.room.*
import com.example.jereby.data.Round
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {

    @Insert suspend fun insert(r: Round): Long
    @Query("SELECT * FROM Round WHERE tournamentId=:tid ORDER BY indexInTournament")
    fun byTournament(tid: Long): Flow<List<Round>>

    @Query("SELECT * FROM Round WHERE id=:rid")
    suspend fun get(rid: Long): Round?

    @Query("SELECT * FROM Round WHERE tournamentId=:tid ORDER BY indexInTournament DESC LIMIT 1")
    suspend fun latest(tid: Long): Round?

}
