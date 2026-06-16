package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

interface ExpenseCategoryRepository {
    fun getExpenseCategoriesFlow(): Flow<List<ExpenseCategory>>
    suspend fun getExpenseCategories(): List<ExpenseCategory>
    suspend fun getExpenseCategoryById(id: Long): ExpenseCategory?
    suspend fun insertExpenseCategory(expenseCategory: ExpenseCategory): Long
    suspend fun updateExpenseCategory(expenseCategory: ExpenseCategory)
    suspend fun softDeleteExpenseCategory(id: Long)
}
