package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.ExpenseCategoryDao
import com.rushi.coinmaster.data.local.entity.ExpenseCategoryEntity
import com.rushi.coinmaster.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseCategoryRepositoryImpl @Inject constructor(
    private val expenseCategoryDao: ExpenseCategoryDao
) : ExpenseCategoryRepository {

    override fun getExpenseCategoriesFlow(): Flow<List<ExpenseCategory>> {
        return expenseCategoryDao.getExpenseCategoriesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getExpenseCategories(): List<ExpenseCategory> {
        return expenseCategoryDao.getExpenseCategories().map { it.toDomain() }
    }

    override suspend fun getExpenseCategoryById(id: Long): ExpenseCategory? {
        return expenseCategoryDao.getExpenseCategoryById(id)?.toDomain()
    }

    override suspend fun insertExpenseCategory(expenseCategory: ExpenseCategory): Long {
        return expenseCategoryDao.insertExpenseCategory(expenseCategory.toEntity())
    }

    override suspend fun updateExpenseCategory(expenseCategory: ExpenseCategory) {
        expenseCategoryDao.updateExpenseCategory(expenseCategory.toEntity())
    }

    override suspend fun softDeleteExpenseCategory(id: Long) {
        expenseCategoryDao.softDeleteExpenseCategory(id)
    }

    private fun ExpenseCategoryEntity.toDomain(): ExpenseCategory {
        return ExpenseCategory(
            id = id,
            name = name,
            colorHex = colorHex,
            iconName = iconName,
            isDeleted = isDeleted
        )
    }

    private fun ExpenseCategory.toEntity(): ExpenseCategoryEntity {
        return ExpenseCategoryEntity(
            id = id,
            name = name,
            colorHex = colorHex,
            iconName = iconName,
            isDeleted = isDeleted
        )
    }
}
