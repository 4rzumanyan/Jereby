package com.example.jereby.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.jereby.data.dao.ClubDao
import com.example.jereby.data.dao.ClubStatsDao
import com.example.jereby.data.dao.MatchDao
import com.example.jereby.data.dao.PlayerDao
import com.example.jereby.data.dao.PlayerStatsDao
import com.example.jereby.data.dao.RoundDao
import com.example.jereby.data.dao.TournamentDao
import com.example.jereby.data.model.Club
import com.example.jereby.data.model.ClubStats
import com.example.jereby.data.model.Match
import com.example.jereby.data.model.Player
import com.example.jereby.data.model.PlayerStats
import com.example.jereby.data.model.Round
import com.example.jereby.data.model.Tournament

@Database(
    entities = [
        Club::class, Player::class, Tournament::class, Round::class, Match::class,
        PlayerStats::class, ClubStats::class
    ],
    version = 2, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clubDao(): ClubDao
    abstract fun playerDao(): PlayerDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun roundDao(): RoundDao
    abstract fun matchDao(): MatchDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun clubStatsDao(): ClubStatsDao
}
