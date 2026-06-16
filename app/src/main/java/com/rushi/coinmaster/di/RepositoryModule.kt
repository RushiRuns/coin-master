package com.rushi.coinmaster.di

import com.rushi.coinmaster.data.repository.IncomeStreamRepository
import com.rushi.coinmaster.data.repository.IncomeStreamRepositoryImpl
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
}
