package com.example.jereby.di

import com.example.jereby.data.datasource.club.ClubSource
import com.example.jereby.data.datasource.club.local.LocalClubSource
import com.example.jereby.data.datasource.club.remote.UefaClubSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier annotation class LocalClubs
@Qualifier annotation class UefaClubs

@Module
@InstallIn(SingletonComponent::class)
object ClubSourceModule {
    @Provides @Singleton @LocalClubs fun provideLocal(local: LocalClubSource): ClubSource = local
    @Provides @Singleton @UefaClubs  fun provideUefa(uefa: UefaClubSource): ClubSource = uefa
}
