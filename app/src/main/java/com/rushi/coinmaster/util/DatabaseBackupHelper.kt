package com.rushi.coinmaster.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.rushi.coinmaster.data.local.database.CoinMasterDatabase
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException

object DatabaseBackupHelper {
    private const val DB_NAME = "coin_master_db"

    /**
     * Checkpoints the database and copies the database file to the output stream.
     */
    fun exportDatabase(context: Context, database: CoinMasterDatabase, outputStream: OutputStream) {
        // 1. Checkpoint the WAL database to write all current logs to the main database file
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

        // 2. Locate the database file path
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) {
            throw IOException("Database file does not exist")
        }

        // 3. Write data to the destination output stream
        dbFile.inputStream().use { input ->
            input.copyTo(outputStream)
        }
    }

    /**
     * Checks if the provided input stream is a valid CoinMaster database backup file.
     * Copying to a temp file is done to safely check it.
     */
    fun validateBackup(context: Context, inputStream: InputStream): File {
        val tempFile = File(context.cacheDir, "temp_backup_check.db")
        try {
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            val db = SQLiteDatabase.openDatabase(
                tempFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            val tables = mutableListOf<String>()
            if (cursor.moveToFirst()) {
                do {
                    tables.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()

            // Verify essential tables exist
            val requiredTables = listOf("accounts", "transactions", "categories")
            val isValid = requiredTables.all { tables.contains(it) }
            if (!isValid) {
                tempFile.delete()
                throw IOException("Selected backup file is missing required CoinMaster database tables")
            }

            return tempFile
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            throw IOException("Selected backup file is invalid: ${e.message}", e)
        }
    }

    /**
     * Replaces the current database with the validated backup file.
     * Note: database must be closed before calling this.
     */
    fun restoreDatabase(context: Context, backupFile: File) {
        val dbFile = context.getDatabasePath(DB_NAME)
        val walFile = context.getDatabasePath("$DB_NAME-wal")
        val shmFile = context.getDatabasePath("$DB_NAME-shm")

        // Delete existing DB files
        if (dbFile.exists()) dbFile.delete()
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()

        // Copy backup to database path
        backupFile.inputStream().use { input ->
            dbFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Clean up temp file
        backupFile.delete()
    }
}
