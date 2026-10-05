package com.example.action

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
) {
    val formattedSize: String
        get() = if (isDirectory) {
            "Directory"
        } else {
            val kb = sizeBytes / 1024.0
            if (kb < 1024) String.format(Locale.US, "%.1f KB", kb)
            else String.format(Locale.US, "%.2f MB", kb / 1024.0)
        }

    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastModified))
}

class FileManager(private val context: Context) {

    private val baseDir: File
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                Environment.getExternalStorageDirectory()
            } else {
                context.getExternalFilesDir(null) ?: context.filesDir
            }
        }

    fun hasFullStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun createFolder(folderName: String, parentPath: String? = null): Pair<Boolean, String> {
        val sanitized = folderName.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
        if (sanitized.isEmpty()) return Pair(false, "Invalid folder name")

        val targetDir = if (!parentPath.isNullOrBlank()) File(parentPath) else baseDir
        val newFolder = File(targetDir, sanitized)

        return try {
            if (newFolder.exists()) {
                Pair(false, "Folder already exists: ${newFolder.absolutePath}")
            } else {
                val created = newFolder.mkdirs()
                if (created) {
                    Pair(true, "Created folder: ${newFolder.name} at ${newFolder.absolutePath}")
                } else {
                    Pair(false, "Failed to create folder at ${newFolder.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Error creating folder: ${e.message}")
        }
    }

    fun listFiles(dirPath: String? = null): List<FileItem> {
        val dir = if (!dirPath.isNullOrBlank()) File(dirPath) else baseDir
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = dir.listFiles() ?: return emptyList()
        return files.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified()
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
    }

    fun searchFiles(keyword: String, maxResults: Int = 30): List<FileItem> {
        val term = keyword.trim().lowercase(Locale.ROOT)
        if (term.isEmpty()) return emptyList()

        val results = mutableListOf<FileItem>()
        fun scanDir(dir: File, depth: Int) {
            if (depth > 4 || results.size >= maxResults) return
            val children = dir.listFiles() ?: return
            for (file in children) {
                if (results.size >= maxResults) break
                if (file.name.lowercase(Locale.ROOT).contains(term)) {
                    results.add(
                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            sizeBytes = if (file.isDirectory) 0 else file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
                if (file.isDirectory && !file.name.startsWith(".")) {
                    scanDir(file, depth + 1)
                }
            }
        }

        scanDir(baseDir, 0)
        return results
    }

    fun deletePath(path: String): Pair<Boolean, String> {
        val file = File(path)
        if (!file.exists()) return Pair(false, "Path does not exist: $path")

        return try {
            val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (deleted) {
                Pair(true, "Successfully deleted: ${file.name}")
            } else {
                Pair(false, "Failed to delete: ${file.name}")
            }
        } catch (e: Exception) {
            Pair(false, "Delete error: ${e.message}")
        }
    }

    fun readFileContent(path: String, maxChars: Int = 2000): Pair<Boolean, String> {
        val file = File(path)
        if (!file.exists() || file.isDirectory) return Pair(false, "File not found or is directory")

        return try {
            val text = file.readText().take(maxChars)
            Pair(true, text)
        } catch (e: Exception) {
            Pair(false, "Failed to read file: ${e.message}")
        }
    }
}
