package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.ExpenseCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories WHERE is_deleted = 0 ORDER BY name ASC")
    fun getExpenseCategoriesFlow(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getExpenseCategories(): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM expense_categories WHERE id = :id AND is_deleted = 0")
    suspend fun getExpenseCategoryById(id: Long): ExpenseCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategory(expenseCategory: ExpenseCategoryEntity): Long

    @Update
    suspend fun updateExpenseCategory(expenseCategory: ExpenseCategoryEntity)

    @Query("UPDATE expense_categories SET is_deleted = 1 WHERE id = :id")
    suspend fun softDeleteExpenseCategory(id: Long)
}
