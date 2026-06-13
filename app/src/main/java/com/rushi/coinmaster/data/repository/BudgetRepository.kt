package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.BudgetDao
import com.rushi.coinmaster.data.local.dao.CategoryDao
import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.entity.EnvelopeAllocationEntity
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao
) {
    suspend fun getBudgetMonth(id: Int): BudgetMonthEntity? = budgetDao.getBudgetMonth(id)

    suspend fun getBudgetMonths(): List<BudgetMonthEntity> = budgetDao.getBudgetMonths()

    fun getBudgetMonthsFlow(): Flow<List<BudgetMonthEntity>> = budgetDao.getBudgetMonthsFlow()

    suspend fun insertBudgetMonth(budgetMonth: BudgetMonthEntity) = budgetDao.insertBudgetMonth(budgetMonth)

    suspend fun updateBudgetMonth(budgetMonth: BudgetMonthEntity) = budgetDao.updateBudgetMonth(budgetMonth)

    fun getEnvelopesWithAllocationsFlow(budgetMonthId: Int): Flow<List<EnvelopeWithAllocation>> {
        return budgetDao.getEnvelopesWithAllocationsFlow(budgetMonthId)
    }

    suspend fun saveAllocation(budgetMonthId: Int, categoryId: Long, allocatedAmountPaise: Long) {
        val allocation = EnvelopeAllocationEntity(
            budgetMonthId = budgetMonthId,
            categoryId = categoryId,
            allocatedAmountPaise = allocatedAmountPaise
        )
        budgetDao.insertAllocation(allocation)
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
