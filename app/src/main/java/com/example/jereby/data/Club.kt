package com.example.jereby.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class Club(
    @PrimaryKey val id: String,      // stable slug, e.g. "real-madrid"
    val name: String,
    val country: String? = null,
    val logoUrl: String? = null
)
