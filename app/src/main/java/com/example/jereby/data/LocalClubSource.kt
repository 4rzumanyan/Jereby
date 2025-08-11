package com.example.jereby.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalClubSource @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun load(limit: Int?): List<Club> {
        val text = ctx.assets.open("clubs.json").bufferedReader().use { it.readText() }
        val all = json.decodeFromString<List<Club>>(text)
        return limit?.let { all.take(it) } ?: all
    }
}
