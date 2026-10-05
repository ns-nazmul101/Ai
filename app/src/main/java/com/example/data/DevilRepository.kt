package com.example.data

import com.example.data.dao.ActionLogDao
import com.example.data.dao.ChatMessageDao
import com.example.data.model.ActionLog
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class DevilRepository(
    private val chatDao: ChatMessageDao,
    private val logDao: ActionLogDao
) {
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()
    val allLogs: Flow<List<ActionLog>> = logDao.getAllLogs()

    suspend fun insertMessage(message: ChatMessage): Long = chatDao.insertMessage(message)

    suspend fun clearMessages() = chatDao.clearAllMessages()

    suspend fun insertLog(log: ActionLog): Long = logDao.insertLog(log)

    suspend fun clearLogs() = logDao.clearAllLogs()
}
