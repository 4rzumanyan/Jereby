package com.example.jereby.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.jereby.data.model.Tournament
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {

    @Insert
    suspend fun insert(t: Tournament): Long

    @Update
    suspend fun update(t: Tournament)

    @Query("SELECT * FROM Tournament WHERE id=:id")
    fun observe(id: Long): Flow<Tournament>

    @Query("SELECT * FROM Tournament WHERE id=:id")
    suspend fun get(id: Long): Tournament?

    @Query("SELECT * FROM Tournament ORDER BY createdAt DESC")
    fun list(): Flow<List<Tournament>>

    @Delete
    suspend fun delete(t: Tournament)

    @Query("DELETE FROM Tournament WHERE id=:tid")
    suspend fun deleteById(tid: Long)

}
