package com.example.storage

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class JsonStorageEngine(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val locks = ConcurrentHashMap<String, ReentrantReadWriteLock>()

    private fun getLock(filename: String): ReentrantReadWriteLock {
        return locks.computeIfAbsent(filename) { ReentrantReadWriteLock() }
    }

    private fun getTargetFile(filename: String): File {
        val baseDir = File(context.filesDir, "json_storage")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return File(baseDir, filename)
    }

    /**
     * Reads and parses a JSON file into type [T].
     * If the main file is missing or corrupted, attempts fallback to the backup file (.bak).
     */
    fun <T> read(filename: String, type: Type, defaultProvider: () -> T): T {
        val file = getTargetFile(filename)
        val backupFile = File(file.parentFile, "$filename.bak")
        val lock = getLock(filename)

        return lock.read {
            if (!file.exists()) {
                if (backupFile.exists()) {
                    try {
                        val json = backupFile.readText()
                        val adapter = moshi.adapter<T>(type)
                        val parsed = adapter.fromJson(json)
                        if (parsed != null) {
                            // Restore main file from backup
                            backupFile.copyTo(file, overwrite = true)
                            return@read parsed
                        }
                    } catch (e: Exception) {
                        // Backup also unparseable
                    }
                }
                return@read defaultProvider()
            }

            try {
                val json = file.readText()
                val adapter = moshi.adapter<T>(type)
                val parsed = adapter.fromJson(json)
                parsed ?: defaultProvider()
            } catch (e: Exception) {
                // If main file is corrupted, attempt recovery from backup
                if (backupFile.exists()) {
                    try {
                        val backupJson = backupFile.readText()
                        val adapter = moshi.adapter<T>(type)
                        val backupParsed = adapter.fromJson(backupJson)
                        if (backupParsed != null) {
                            backupFile.copyTo(file, overwrite = true)
                            return@read backupParsed
                        }
                    } catch (_: Exception) {}
                }
                defaultProvider()
            }
        }
    }

    /**
     * Atomically writes data [data] of type [T] to [filename].
     * 1. Preserves existing file to .bak
     * 2. Writes new content to .tmp
     * 3. Syncs file descriptor to physical disk
     * 4. Atomically renames .tmp to target file
     */
    fun <T> write(filename: String, type: Type, data: T): Boolean {
        val file = getTargetFile(filename)
        val tempFile = File(file.parentFile, "$filename.tmp")
        val backupFile = File(file.parentFile, "$filename.bak")
        val lock = getLock(filename)

        return lock.write {
            try {
                val adapter = moshi.adapter<T>(type)
                val json = adapter.indent("  ").toJson(data)

                // 1. Create backup of current file if it exists
                if (file.exists() && file.length() > 0) {
                    try {
                        file.copyTo(backupFile, overwrite = true)
                    } catch (_: Exception) {}
                }

                // 2. Write to temp file with physical disk flush
                FileOutputStream(tempFile).use { fos ->
                    fos.write(json.toByteArray(Charsets.UTF_8))
                    fos.fd.sync()
                }

                // 3. Atomic rename
                val success = tempFile.renameTo(file)
                if (!success) {
                    // Fallback copy if renameTo fails across file systems
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }
                true
            } catch (e: Exception) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                false
            }
        }
    }

    /**
     * Deletes a storage file and its backup.
     */
    fun delete(filename: String): Boolean {
        val file = getTargetFile(filename)
        val backupFile = File(file.parentFile, "$filename.bak")
        val lock = getLock(filename)

        return lock.write {
            val mainDeleted = if (file.exists()) file.delete() else true
            val backupDeleted = if (backupFile.exists()) backupFile.delete() else true
            mainDeleted && backupDeleted
        }
    }
}
