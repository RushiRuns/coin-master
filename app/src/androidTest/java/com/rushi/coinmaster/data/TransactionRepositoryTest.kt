package com.rushi.coinmaster.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rushi.coinmaster.data.local.dao.AccountDao
import com.rushi.coinmaster.data.local.dao.BudgetDao
import com.rushi.coinmaster.data.local.dao.CategoryDao
import com.rushi.coinmaster.data.local.dao.TransactionDao
import com.rushi.coinmaster.data.local.database.CoinMasterDatabase
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.TransactionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TransactionRepositoryTest {

    private lateinit var db: CoinMasterDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var repository: TransactionRepository

    private var accountId1: Long = 0
    private var accountId2: Long = 0
    private var categoryId: Long = 0
    private val budgetPeriodId = 202606

    @Before
    fun createDb() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            db = Room.inMemoryDatabaseBuilder(context, CoinMasterDatabase::class.java).build()
            accountDao = db.accountDao()
            categoryDao = db.categoryDao()
            budgetDao = db.budgetDao()
            transactionDao = db.transactionDao()
            repository = TransactionRepository(transactionDao)

            // Seed initial data to satisfy Foreign Key constraints
            accountId1 = accountDao.insertAccount(
                AccountEntity(name = "Bank", type = AccountType.BANK_ACCOUNT, balancePaise = 100000L, colorHex = "#0000FF", iconName = "bank")
            )
            accountId2 = accountDao.insertAccount(
                AccountEntity(name = "Cash", type = AccountType.CASH, balancePaise = 20000L, colorHex = "#00FF00", iconName = "cash")
            )
            categoryId = categoryDao.insertCategory(
                CategoryEntity(name = "Groceries", bucketType = BucketType.NEEDS, colorHex = "#FF0000", iconName = "groceries", displayOrder = 0)
            )
            budgetDao.insertBudgetPeriod(
                BudgetPeriodEntity(
                    id = budgetPeriodId,
                    startDate = System.currentTimeMillis() - 1000000L,
                    endDate = System.currentTimeMillis() + 1000000L,
                    incomePaise = 100000L
                )
            )
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertExpense_decreasesAccountBalance() = runBlocking {
        val transaction = TransactionEntity(
            amountPaise = 15000L,
            type = TransactionType.EXPENSE,
            accountId = accountId1,
            categoryId = categoryId,
            budgetPeriodId = budgetPeriodId,
            date = System.currentTimeMillis()
        )

        repository.insertTransaction(transaction)

        val account = accountDao.getAccountById(accountId1)
        assertEquals(85000L, account?.balancePaise)
    }

    @Test
    fun getEnvelopesWithAllocations_calculatesSpentCorrectly() = runBlocking {
        // Add allocation for the envelope
        budgetDao.insertAllocation(
            com.rushi.coinmaster.data.local.entity.EnvelopeAllocationEntity(
                budgetPeriodId = budgetPeriodId,
                categoryId = categoryId,
                allocatedAmountPaise = 50000L
            )
        )

        // Add a transaction for this envelope
        val transaction = TransactionEntity(
            amountPaise = 15000L,
            type = TransactionType.EXPENSE,
            accountId = accountId1,
            categoryId = categoryId,
            budgetPeriodId = budgetPeriodId,
            date = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(transaction)

        // Query the envelopes with allocations
        val envelopes = budgetDao.getEnvelopesWithAllocationsFlow(budgetPeriodId).first()
        assertEquals(1, envelopes.size)
        assertEquals(15000L, envelopes[0].spentAmountPaise)
    }

    @Test
    fun recalculateTransactionBudgetPeriods_setsPeriodIdCorrectly() = runBlocking {
        // Add a transaction with null budgetPeriodId
        val transaction = TransactionEntity(
            amountPaise = 15000L,
            type = TransactionType.EXPENSE,
            accountId = accountId1,
            categoryId = categoryId,
            budgetPeriodId = null,
            date = System.currentTimeMillis()
        )
        val txId = transactionDao.insertTransaction(transaction)

        // Verify budgetPeriodId is null initially
        val txBefore = transactionDao.getTransactionById(txId)
        org.junit.Assert.assertNull(txBefore?.budgetPeriodId)

        // Recalculate
        budgetDao.recalculateTransactionBudgetPeriods()

        // Verify budgetPeriodId is now set to budgetPeriodId
        val txAfter = transactionDao.getTransactionById(txId)
        assertEquals(budgetPeriodId, txAfter?.budgetPeriodId)
    }

    @Test
    fun insertIncome_increasesAccountBalance() = runBlocking {
        val transaction = TransactionEntity(
            amountPaise = 30000L,
            type = TransactionType.INCOME,
            accountId = accountId1,
            date = System.currentTimeMillis()
        )

        repository.insertTransaction(transaction)

        val account = accountDao.getAccountById(accountId1)
        assertEquals(130000L, account?.balancePaise)
    }

    @Test
    fun insertTransfer_updatesBothAccountBalances() = runBlocking {
        val transaction = TransactionEntity(
            amountPaise = 25000L,
            type = TransactionType.TRANSFER,
            accountId = accountId1,
            transferToAccountId = accountId2,
            date = System.currentTimeMillis()
        )

        repository.insertTransaction(transaction)

        val sourceAccount = accountDao.getAccountById(accountId1)
        val destAccount = accountDao.getAccountById(accountId2)

        assertEquals(75000L, sourceAccount?.balancePaise)
        assertEquals(45000L, destAccount?.balancePaise)
    }

    @Test
    fun deleteExpense_revertsAccountBalance() = runBlocking {
        val transaction = TransactionEntity(
            amountPaise = 15000L,
            type = TransactionType.EXPENSE,
            accountId = accountId1,
            categoryId = categoryId,
            budgetPeriodId = budgetPeriodId,
            date = System.currentTimeMillis()
        )

        val id = repository.insertTransaction(transaction)

        val accountBefore = accountDao.getAccountById(accountId1)
        assertEquals(85000L, accountBefore?.balancePaise)

        repository.deleteTransaction(id)

        val accountAfter = accountDao.getAccountById(accountId1)
        assertEquals(100000L, accountAfter?.balancePaise)
    }

    @Test
    fun deleteTransfer_revertsBothAccountBalances() = runBlocking {
        val transaction = TransactionEntity(
            amountPaise = 25000L,
            type = TransactionType.TRANSFER,
            accountId = accountId1,
            transferToAccountId = accountId2,
            date = System.currentTimeMillis()
        )

        val id = repository.insertTransaction(transaction)

        val sourceBefore = accountDao.getAccountById(accountId1)
        val destBefore = accountDao.getAccountById(accountId2)
        assertEquals(75000L, sourceBefore?.balancePaise)
        assertEquals(45000L, destBefore?.balancePaise)

        repository.deleteTransaction(id)

        val sourceAfter = accountDao.getAccountById(accountId1)
        val destAfter = accountDao.getAccountById(accountId2)
        assertEquals(100000L, sourceAfter?.balancePaise)
        assertEquals(20000L, destAfter?.balancePaise)
    }
}
