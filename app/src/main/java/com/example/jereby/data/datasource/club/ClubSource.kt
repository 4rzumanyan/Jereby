package com.example.jereby.data.datasource.club

import com.example.jereby.data.model.Club

interface ClubSource {
    suspend fun load(limit: Int? = null): List<Club>
}