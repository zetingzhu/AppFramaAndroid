package com.trade.zt_speed_device

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object DownloadQuestionnaireHelper {

    /** 以 . 开头，在 Download 目录中视为隐藏文件（多数文件管理器默认不显示） */
    const val FILE_NAME = "zt_questionnaire.txt"
    const val FILE_CONTENT = "AAAAAA"

    data class WriteResult(
        val success: Boolean,
        val pathHint: String,
        val errorMessage: String? = null
    )

    data class ReadResult(
        val exists: Boolean,
        val content: String?,
        val pathHint: String,
        val errorMessage: String? = null
    )

    data class DownloadEntry(
        val displayName: String,
        val sizeBytes: Long?,
        val lastModifiedMillis: Long?,
        val pathHint: String
    )

    data class ListResult(
        val success: Boolean,
        val entries: List<DownloadEntry>,
        val errorMessage: String? = null
    )

    fun writeToDownloads(context: Context): WriteResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaMediaStore(context)
            } else {
                writeLegacy(context)
            }
        } catch (e: Exception) {
            WriteResult(false, downloadsPathHint(), e.message)
        }
    }

    fun readFromDownloadsIfExists(context: Context): ReadResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                readViaMediaStore(context)
            } else {
                readLegacy(context)
            }
        } catch (e: Exception) {
            ReadResult(false, null, downloadsPathHint(), e.message)
        }
    }

    /** 列出当前应用能访问到的 Download 目录下文件（受分区存储策略限制） */
    fun listAccessibleDownloadFiles(context: Context): ListResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                listViaMediaStore(context)
            } else {
                listLegacy()
            }
        } catch (e: Exception) {
            ListResult(false, emptyList(), e.message)
        }
    }

    fun formatListResult(result: ListResult): String {
        if (!result.success) {
            return "列表失败: ${result.errorMessage.orEmpty()}"
        }
        if (result.entries.isEmpty()) {
            return "Download 目录下未查到可访问文件（或目录为空）"
        }
        return buildString {
            appendLine("共 ${result.entries.size} 项（应用可见范围）")
            result.entries.forEachIndexed { index, entry ->
                appendLine("--- [$index] ${entry.displayName} ---")
                appendLine("size=${entry.sizeBytes ?: "unknown"}")
                appendLine("modified=${entry.lastModifiedMillis ?: "unknown"}")
                appendLine("path=${entry.pathHint}")
            }
        }.trimEnd()
    }

    fun downloadsPathHint(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Download/$FILE_NAME"
        } else {
            @Suppress("DEPRECATION")
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                FILE_NAME
            ).absolutePath
        }
    }

    private fun writeViaMediaStore(context: Context): WriteResult {
        val resolver = context.contentResolver
        val existing = findDownloadUri(context)
        val uri = existing ?: run {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } ?: return WriteResult(false, downloadsPathHint(), "insert failed")

        resolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(FILE_CONTENT.toByteArray(Charsets.UTF_8))
        } ?: return WriteResult(false, downloadsPathHint(), "openOutputStream failed")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && existing == null) {
            val clearPending = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, clearPending, null, null)
        }

        return WriteResult(true, "Download/$FILE_NAME (uri=$uri)")
    }

    private fun readViaMediaStore(context: Context): ReadResult {
        val uri = findDownloadUri(context)
            ?: return ReadResult(false, null, "Download/$FILE_NAME", "文件不存在")

        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        }
        return ReadResult(
            exists = true,
            content = text,
            pathHint = "Download/$FILE_NAME (uri=$uri)"
        )
    }

    private fun listViaMediaStore(context: Context): ListResult {
        val entries = mutableListOf<DownloadEntry>()
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.DATE_MODIFIED,
            MediaStore.Downloads.RELATIVE_PATH
        )
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("${Environment.DIRECTORY_DOWNLOADS}/%")
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Downloads.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
            val pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx)
                val relativePath = cursor.getString(pathIdx)
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                val size = cursor.getLong(sizeIdx)
                val modified = cursor.getLong(dateIdx)
                entries.add(
                    DownloadEntry(
                        displayName = name,
                        sizeBytes = size.takeIf { it >= 0 },
                        lastModifiedMillis = modified.takeIf { it > 0 },
                        pathHint = "${relativePath}$name (uri=$uri)"
                    )
                )
            }
        }
        return ListResult(true, entries)
    }

    @Suppress("DEPRECATION")
    private fun listLegacy(): ListResult {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists() || !dir.isDirectory) {
            return ListResult(true, emptyList())
        }
        val entries = dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                DownloadEntry(
                    displayName = file.name,
                    sizeBytes = file.length(),
                    lastModifiedMillis = file.lastModified(),
                    pathHint = file.absolutePath
                )
            }
            ?: emptyList()
        return ListResult(true, entries)
    }

    private fun findDownloadUri(context: Context): Uri? {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME}=?"
        context.contentResolver.query(
            collection,
            projection,
            selection,
            arrayOf(FILE_NAME),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun writeLegacy(context: Context): WriteResult {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists() && !dir.mkdirs()) {
            return WriteResult(false, dir.absolutePath, "mkdirs failed")
        }
        val file = File(dir, FILE_NAME)
        file.writeText(FILE_CONTENT, Charsets.UTF_8)
        return WriteResult(true, file.absolutePath)
    }

    @Suppress("DEPRECATION")
    private fun readLegacy(context: Context): ReadResult {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            FILE_NAME
        )
        if (!file.exists()) {
            return ReadResult(false, null, file.absolutePath, "文件不存在")
        }
        return ReadResult(true, file.readText(Charsets.UTF_8), file.absolutePath)
    }
}
