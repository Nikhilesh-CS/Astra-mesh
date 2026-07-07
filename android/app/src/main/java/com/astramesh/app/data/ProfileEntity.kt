package com.astramesh.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val contactKey: String,
    val name: String,
    val bio: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
