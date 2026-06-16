package com.rushi.coinmaster.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 5,
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
}
