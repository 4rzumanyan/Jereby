package com.example.jereby.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.jereby.data.model.Round
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {

    @Insert suspend fun insert(r: Round): Long
    @Query("SELECT * FROM Round WHERE tournamentId=:tid ORDER BY indexInTournament")
    fun byTournament(tid: Long): Flow<List<Round>>

    @Query("SELECT * FROM Round WHERE tournamentId=:tid ORDER BY indexInTournament DESC LIMIT 1")
    suspend fun latest(tid: Long): Round?

    @Query("SELECT * FROM Round WHERE id=:rid")
    suspend fun get(rid: Long): Round?

    @Query("SELECT * FROM Round WHERE tournamentId=:tid AND indexInTournament > :fromIndex ORDER BY indexInTournament")
    suspend fun laterRounds(tid: Long, fromIndex: Int): List<Round>

    @Query("DELETE FROM Round WHERE id IN (:ids)")
    suspend fun deleteRounds(ids: List<Long>)

    @Query("SELECT id FROM Round WHERE tournamentId=:tid")
    suspend fun idsByTournament(tid: Long): List<Long>

    @Query("DELETE FROM Round WHERE tournamentId=:tid")
    suspend fun deleteByTournament(tid: Long)

}
