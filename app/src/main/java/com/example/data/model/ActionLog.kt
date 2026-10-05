package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_logs")
data class ActionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val actionType: String,
    val isRoot: Boolean = false,
    val isSuccess: Boolean = true,
    val output: String = "",
    val executionTimeMs: Long = 0
)
