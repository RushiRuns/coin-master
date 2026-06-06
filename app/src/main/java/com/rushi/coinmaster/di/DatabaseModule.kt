package com.rushi.coinmaster.di

import android.content.Context
import androidx.room.Room
import com.rushi.coinmaster.data.local.dao.*
import com.rushi.coinmaster.data.local.database.CoinMasterDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "coin_master_db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CoinMasterDatabase {
        return Room.databaseBuilder(
            context,
            CoinMasterDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideAccountDao(db: CoinMasterDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: CoinMasterDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBudgetDao(db: CoinMasterDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideTransactionDao(db: CoinMasterDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideSinkingFundDao(db: CoinMasterDatabase): SinkingFundDao = db.sinkingFundDao()
}
