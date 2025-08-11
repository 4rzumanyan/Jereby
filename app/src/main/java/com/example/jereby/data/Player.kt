package com.example.jereby.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String
)
