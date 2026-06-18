package com.rushi.coinmaster.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rushi.coinmaster.data.local.dao.*
import com.rushi.coinmaster.data.local.entity.*

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        BudgetPeriodEntity::class,
        EnvelopeAllocationEntity::class,
        TransactionEntity::class,
        SinkingFundEntity::class,
        DebtEntity::class,
        ExpenseCategoryEntity::class,
        IncomeStreamEntity::class,
        NoteEntity::class,
        TransferRecipientEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class CoinMasterDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun sinkingFundDao(): SinkingFundDao
    abstract fun debtDao(): DebtDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun incomeStreamDao(): IncomeStreamDao
    abstract fun noteDao(): NoteDao
    abstract fun transferRecipientDao(): TransferRecipientDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create expense_categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expense_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color_hex` TEXT NOT NULL,
                        `icon_name` TEXT NOT NULL,
                        `is_deleted` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create income_streams table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `income_streams` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount_paise` INTEGER NOT NULL,
                        `account_id` INTEGER,
                        `is_deleted` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())

                // Create notes table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create transfer_recipients table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transfer_recipients` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `bank_details` TEXT,
                        `is_deleted` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Alter categories table to add new columns
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `expense_category_id` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `expense_type` TEXT NOT NULL DEFAULT 'VARIABLE'")

                // Alter transactions table to add new column
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `transfer_recipient_id` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expense_categories` ADD COLUMN `bucket_type` TEXT DEFAULT NULL")
            }
        }
    }
}
