package com.rushi.coinmaster.data.repository

import android.content.Context
import com.rushi.coinmaster.data.local.dao.AccountDao
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountDao: AccountDao
) {
    fun getAccountsFlow(): Flow<List<AccountEntity>> = accountDao.getAccountsFlow()
    
    suspend fun getAccounts(): List<AccountEntity> = accountDao.getAccounts()

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    suspend fun insertAccount(account: AccountEntity): Long {
        val id = accountDao.insertAccount(account)
        WidgetUpdater.updateWidget(context)
        return id
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
        WidgetUpdater.updateWidget(context)
    }

    suspend fun softDeleteAccount(id: Long) {
        accountDao.softDeleteAccount(id)
        WidgetUpdater.updateWidget(context)
    }
}
