package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val colorHex: String = "#FFF2C2", // Default light warm yellow yellow
    val category: String = "General",
    val isPinned: Boolean = false,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false, // Soft delete for cloud synchronization simulation
    val lastModified: Long = System.currentTimeMillis()
)
