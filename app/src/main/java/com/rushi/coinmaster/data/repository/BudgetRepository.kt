package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.BudgetDao
import com.rushi.coinmaster.data.local.dao.CategoryDao
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.entity.EnvelopeAllocationEntity
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao
) {
    suspend fun getBudgetPeriod(id: Int): BudgetPeriodEntity? = budgetDao.getBudgetPeriod(id)

    suspend fun getBudgetPeriods(): List<BudgetPeriodEntity> = budgetDao.getBudgetPeriods()

    fun getBudgetPeriodsFlow(): Flow<List<BudgetPeriodEntity>> = budgetDao.getBudgetPeriodsFlow()

    suspend fun insertBudgetPeriod(budgetPeriod: BudgetPeriodEntity): Long = budgetDao.insertBudgetPeriod(budgetPeriod)

    suspend fun updateBudgetPeriod(budgetPeriod: BudgetPeriodEntity) = budgetDao.updateBudgetPeriod(budgetPeriod)

    suspend fun getBudgetPeriodForDate(dateMillis: Long): BudgetPeriodEntity? = budgetDao.getBudgetPeriodForDate(dateMillis)

    suspend fun getOverlappingPeriod(startDate: Long, endDate: Long, excludeId: Int): BudgetPeriodEntity? =
        budgetDao.getOverlappingPeriod(startDate, endDate, excludeId)

    suspend fun recalculateTransactionBudgetPeriods() = budgetDao.recalculateTransactionBudgetPeriods()

    fun getEnvelopesWithAllocationsFlow(budgetPeriodId: Int): Flow<List<EnvelopeWithAllocation>> {
        return budgetDao.getEnvelopesWithAllocationsFlow(budgetPeriodId)
    }

    suspend fun saveAllocation(budgetPeriodId: Int, categoryId: Long, allocatedAmountPaise: Long) {
        val allocation = EnvelopeAllocationEntity(
            budgetPeriodId = budgetPeriodId,
            categoryId = categoryId,
            allocatedAmountPaise = allocatedAmountPaise
        )
        budgetDao.insertAllocation(allocation)
    }

    fun hasAllocationsFlow(budgetPeriodId: Int): Flow<Boolean> {
        return budgetDao.hasAllocationsFlow(budgetPeriodId)
    }

    suspend fun hasAllocations(budgetPeriodId: Int): Boolean {
        return budgetDao.hasAllocations(budgetPeriodId)
    }

    suspend fun copyAllocations(fromPeriodId: Int, toPeriodId: Int) {
        val previousAllocations = budgetDao.getAllocations(fromPeriodId)
        if (previousAllocations.isEmpty()) return
        val newAllocations = previousAllocations.map {
            it.copy(id = 0, budgetPeriodId = toPeriodId)
        }
        budgetDao.replaceAllocations(toPeriodId, newAllocations)
    }

    suspend fun getOrCreateBudgetPeriodForDate(dateMillis: Long): BudgetPeriodEntity {
        val existing = budgetDao.getBudgetPeriodForDate(dateMillis)
        if (existing != null) {
            return existing
        }

        // Setup default calendar monthly range fallback
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        val overlapping = budgetDao.getOverlappingPeriod(startDate, endDate, 0)
        if (overlapping != null) {
            return overlapping
        }

        val newPeriod = BudgetPeriodEntity(
            startDate = startDate,
            endDate = endDate,
            incomePaise = 0L,
            needsPercent = 50,
            wantsPercent = 30,
            savingsPercent = 20,
            isActive = false
        )
        val newId = budgetDao.insertBudgetPeriod(newPeriod).toInt()
        return newPeriod.copy(id = newId)
    }

    // Category CRUD helpers for AddEditEnvelope
    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun softDeleteCategory(id: Long) = categoryDao.softDeleteCategory(id)

    fun getCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getCategoriesFlow()

    suspend fun seedDefaultCategories() {
        val existing = categoryDao.getCategories()
        if (existing.isNotEmpty()) return

        val defaults = listOf(
            CategoryEntity(name = "Rent", bucketType = BucketType.NEEDS, colorHex = "#E57373", iconName = "ic_rent", displayOrder = 0),
            CategoryEntity(name = "Groceries", bucketType = BucketType.NEEDS, colorHex = "#81C784", iconName = "ic_groceries", displayOrder = 1),
            CategoryEntity(name = "Utilities", bucketType = BucketType.NEEDS, colorHex = "#64B5F6", iconName = "ic_utilities", displayOrder = 2),
            
            CategoryEntity(name = "Dining Out", bucketType = BucketType.WANTS, colorHex = "#FFD54F", iconName = "ic_dining", displayOrder = 3),
            CategoryEntity(name = "Entertainment", bucketType = BucketType.WANTS, colorHex = "#BA68C8", iconName = "ic_entertainment", displayOrder = 4),
            CategoryEntity(name = "Shopping", bucketType = BucketType.WANTS, colorHex = "#4DB6AC", iconName = "ic_shopping", displayOrder = 5),
            
            CategoryEntity(name = "General Savings", bucketType = BucketType.SAVINGS, colorHex = "#4DD0E1", iconName = "ic_savings", displayOrder = 6),
            CategoryEntity(name = "Emergency Fund", bucketType = BucketType.SAVINGS, colorHex = "#FF8A65", iconName = "ic_emergency", displayOrder = 7),
            CategoryEntity(name = "Lending & Debts", bucketType = BucketType.WANTS, colorHex = "#90A4AE", iconName = "ic_accounts", displayOrder = 8)
        )

        for (category in defaults) {
            categoryDao.insertCategory(category)
        }
    }
}
