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
        BudgetMonthEntity::class,
        EnvelopeAllocationEntity::class,
        TransactionEntity::class,
        SinkingFundEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class CoinMasterDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun sinkingFundDao(): SinkingFundDao
}
