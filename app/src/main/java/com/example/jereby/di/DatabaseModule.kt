package com.example.jereby.di

import android.content.Context
import androidx.room.Room
import com.example.jereby.data.AppDatabase
import com.example.jereby.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "jereby.db").build()

    @Provides fun clubDao(db: AppDatabase): ClubDao = db.clubDao()
    @Provides fun playerDao(db: AppDatabase): PlayerDao = db.playerDao()
    @Provides fun tournamentDao(db: AppDatabase): TournamentDao = db.tournamentDao()
    @Provides fun roundDao(db: AppDatabase): RoundDao = db.roundDao()
    @Provides fun matchDao(db: AppDatabase): MatchDao = db.matchDao()
    @Provides fun playerStatsDao(db: AppDatabase): PlayerStatsDao = db.playerStatsDao()
    @Provides fun clubStatsDao(db: AppDatabase): ClubStatsDao = db.clubStatsDao()

}
