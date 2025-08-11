package com.example.jereby.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.jereby.data.model.Match
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Insert suspend fun insertAll(matches: List<Match>)
    @Update suspend fun update(m: Match)
    @Query("SELECT * FROM `match` WHERE roundId=:rid ORDER BY position")
    fun byRound(rid: Long): Flow<List<Match>>

    @Query("SELECT * FROM `match` WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<Match>

    @Query("DELETE FROM `match` WHERE roundId IN (:roundIds)")
    suspend fun deleteByRoundIds(roundIds: List<Long>)

    @Query("SELECT * FROM `match` WHERE roundId=:rid")
    suspend fun listOnce(rid: Long): List<Match>

}
