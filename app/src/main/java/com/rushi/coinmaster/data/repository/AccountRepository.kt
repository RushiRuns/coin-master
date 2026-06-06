package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.AccountDao
import com.rushi.coinmaster.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun getAccountsFlow(): Flow<List<AccountEntity>> = accountDao.getAccountsFlow()
    
    suspend fun getAccounts(): List<AccountEntity> = accountDao.getAccounts()

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)

    suspend fun softDeleteAccount(id: Long) = accountDao.softDeleteAccount(id)
}
