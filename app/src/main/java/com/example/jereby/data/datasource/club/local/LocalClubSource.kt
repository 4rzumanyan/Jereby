package com.example.jereby.data.datasource.club.local

import android.content.Context
import com.example.jereby.data.datasource.club.ClubSource
import com.example.jereby.data.model.Club
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalClubSource @Inject constructor(
    @ApplicationContext private val ctx: Context
) : ClubSource {
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun load(limit: Int?): List<Club> {
        val text = ctx.assets.open("clubs.json").bufferedReader().use { it.readText() }
        val all = json.decodeFromString<List<Club>>(text)
        return limit?.let { all.take(it) } ?: all
    }
}