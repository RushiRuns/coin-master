package com.rushi.coinmaster.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rushi.coinmaster.data.local.dao.AccountDao
import com.rushi.coinmaster.data.local.dao.DebtDao
import com.rushi.coinmaster.data.local.dao.TransactionDao
import com.rushi.coinmaster.data.local.database.CoinMasterDatabase
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.DebtType
import com.rushi.coinmaster.data.local.model.TransactionType
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
class DebtDaoTest {

    private lateinit var db: CoinMasterDatabase
    private lateinit var debtDao: DebtDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var accountDao: AccountDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CoinMasterDatabase::class.java).build()
        debtDao = db.debtDao()
        transactionDao = db.transactionDao()
        accountDao = db.accountDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadDebt() = runBlocking {
        val debt = DebtEntity(
            personName = "Sunita Sharma",
            type = DebtType.LENT,
            amountPaise = 200000L, // ₹2000
            remainingPaise = 200000L,
            dueDate = null,
            isSettled = false,
            date = System.currentTimeMillis()
        )
        val id = debtDao.insertDebt(debt)
        val fetched = debtDao.getDebtById(id)

        assertEquals("Sunita Sharma", fetched?.personName)
        assertEquals(DebtType.LENT, fetched?.type)
        assertEquals(200000L, fetched?.remainingPaise)
    }

    @Test
    fun testActiveAndSettledDebtsFilters() = runBlocking {
        val debt1 = DebtEntity(
            personName = "Sunita",
            type = DebtType.LENT,
            amountPaise = 200000L,
            remainingPaise = 200000L,
            isSettled = false,
            date = System.currentTimeMillis()
        )
        val debt2 = DebtEntity(
            personName = "Rohit",
            type = DebtType.BORROWED,
            amountPaise = 100000L,
            remainingPaise = 0L,
            isSettled = true,
            date = System.currentTimeMillis()
        )
        debtDao.insertDebt(debt1)
        debtDao.insertDebt(debt2)

        val activeDebts = debtDao.getActiveDebtsFlow().first()
        val settledDebts = debtDao.getSettledDebtsFlow().first()

        assertEquals(1, activeDebts.size)
        assertEquals("Sunita", activeDebts[0].personName)

        assertEquals(1, settledDebts.size)
        assertEquals("Rohit", settledDebts[0].personName)
    }

    @Test
    fun testTransactionForeignKeySetNullOnDelete() = runBlocking {
        // Create source account
        val account = AccountEntity(
            name = "Bank",
            type = AccountType.BANK_ACCOUNT,
            balancePaise = 500000L,
            colorHex = "#ffffff",
            iconName = "ic_bank"
        )
        val accountId = accountDao.insertAccount(account)

        // Insert Debt
        val debt = DebtEntity(
            personName = "Sunita",
            type = DebtType.LENT,
            amountPaise = 200000L,
            remainingPaise = 200000L,
            date = System.currentTimeMillis()
        )
        val debtId = debtDao.insertDebt(debt)

        // Insert Transaction linked to Debt
        val transaction = TransactionEntity(
            amountPaise = 200000L,
            type = TransactionType.EXPENSE,
            accountId = accountId,
            debtId = debtId,
            date = System.currentTimeMillis()
        )
        val transactionId = transactionDao.insertTransaction(transaction)

        // Verify relationship is correct
        val fetchedTransactionBefore = transactionDao.getTransactionById(transactionId)
        assertEquals(debtId, fetchedTransactionBefore?.debtId)

        // Delete Debt
        debtDao.deleteDebtById(debtId)

        // Verify transaction survives but debtId is set to NULL (due to ON DELETE SET_NULL)
        val fetchedTransactionAfter = transactionDao.getTransactionById(transactionId)
        assertTrue(fetchedTransactionAfter != null)
        assertNull(fetchedTransactionAfter?.debtId)
    }
}
