package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
import com.rushi.coinmaster.data.local.entity.EnvelopeAllocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget_months WHERE id = :id")
    suspend fun getBudgetMonth(id: Int): BudgetMonthEntity?

    @Query("SELECT * FROM budget_months ORDER BY id DESC")
    fun getBudgetMonthsFlow(): Flow<List<BudgetMonthEntity>>

    @Query("SELECT * FROM budget_months ORDER BY id DESC")
    suspend fun getBudgetMonths(): List<BudgetMonthEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetMonth(budgetMonth: BudgetMonthEntity)

    @Query("SELECT * FROM envelope_allocations WHERE budget_month_id = :budgetMonthId")
    fun getAllocationsFlow(budgetMonthId: Int): Flow<List<EnvelopeAllocationEntity>>

    @Query("SELECT * FROM envelope_allocations WHERE budget_month_id = :budgetMonthId")
    suspend fun getAllocations(budgetMonthId: Int): List<EnvelopeAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<EnvelopeAllocationEntity>)

    @Query("DELETE FROM envelope_allocations WHERE budget_month_id = :budgetMonthId")
    suspend fun deleteAllocationsForMonth(budgetMonthId: Int)

    @Transaction
    suspend fun replaceAllocations(budgetMonthId: Int, allocations: List<EnvelopeAllocationEntity>) {
        deleteAllocationsForMonth(budgetMonthId)
        insertAllocations(allocations)
    }
}
