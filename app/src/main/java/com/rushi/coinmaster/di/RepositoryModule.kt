package com.rushi.coinmaster.di

import com.rushi.coinmaster.data.repository.IncomeStreamRepository
import com.rushi.coinmaster.data.repository.IncomeStreamRepositoryImpl
import com.rushi.coinmaster.data.repository.ExpenseCategoryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindIncomeStreamRepository(
        incomeStreamRepositoryImpl: IncomeStreamRepositoryImpl
    ): IncomeStreamRepository

    @Binds
    @Singleton
    abstract fun bindExpenseCategoryRepository(
        expenseCategoryRepositoryImpl: ExpenseCategoryRepositoryImpl
    ): com.rushi.coinmaster.data.repository.ExpenseCategoryRepository
}
