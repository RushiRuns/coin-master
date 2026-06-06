package com.rushi.coinmaster.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rushi.coinmaster.data.local.dao.AccountDao
import com.rushi.coinmaster.data.local.database.CoinMasterDatabase
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.model.AccountType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AccountDaoTest {

    private lateinit var db: CoinMasterDatabase
    private lateinit var accountDao: AccountDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CoinMasterDatabase::class.java).build()
        accountDao = db.accountDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadAccount() = runBlocking {
        val account = AccountEntity(
            name = "Test Bank",
            type = AccountType.BANK_ACCOUNT,
            balancePaise = 50000L,
            colorHex = "#123456",
            iconName = "ic_bank"
        )
        val id = accountDao.insertAccount(account)
        val fetched = accountDao.getAccountById(id)

        assertEquals("Test Bank", fetched?.name)
        assertEquals(AccountType.BANK_ACCOUNT, fetched?.type)
        assertEquals(50000L, fetched?.balancePaise)
    }

    @Test
    fun testSoftDelete() = runBlocking {
        val account = AccountEntity(
            name = "Cash Wallet",
            type = AccountType.CASH,
            balancePaise = 1000L,
            colorHex = "#000000",
            iconName = "ic_cash"
        )
        val id = accountDao.insertAccount(account)

        val accountsBefore = accountDao.getAccountsFlow().first()
        assertEquals(1, accountsBefore.size)

        accountDao.softDeleteAccount(id)

        val accountsAfter = accountDao.getAccountsFlow().first()
        assertTrue(accountsAfter.isEmpty())
        assertNull(accountDao.getAccountById(id))
    }
}
