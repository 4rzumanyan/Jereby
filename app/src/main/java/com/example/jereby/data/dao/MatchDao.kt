package com.example.jereby.data.dao

import androidx.room.*
import com.example.jereby.data.Match
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Insert suspend fun insertAll(matches: List<Match>)
    @Update suspend fun update(m: Match)
    @Query("SELECT * FROM `match` WHERE roundId=:rid ORDER BY position")
    fun byRound(rid: Long): Flow<List<Match>>

    @Query("SELECT * FROM `match` WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<Match>

    @Query("SELECT * FROM `match` WHERE roundId=:rid")
    suspend fun listOnce(rid: Long): List<Match>

}
