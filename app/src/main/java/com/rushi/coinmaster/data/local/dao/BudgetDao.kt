package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
import com.rushi.coinmaster.data.local.entity.EnvelopeAllocationEntity
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
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

    @Update
    suspend fun updateBudgetMonth(budgetMonth: BudgetMonthEntity)

    @Query("SELECT * FROM envelope_allocations WHERE budget_month_id = :budgetMonthId")
    fun getAllocationsFlow(budgetMonthId: Int): Flow<List<EnvelopeAllocationEntity>>

    @Query("SELECT * FROM envelope_allocations WHERE budget_month_id = :budgetMonthId")
    suspend fun getAllocations(budgetMonthId: Int): List<EnvelopeAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<EnvelopeAllocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: EnvelopeAllocationEntity)

    @Query("DELETE FROM envelope_allocations WHERE budget_month_id = :budgetMonthId")
    suspend fun deleteAllocationsForMonth(budgetMonthId: Int)

    @Transaction
    suspend fun replaceAllocations(budgetMonthId: Int, allocations: List<EnvelopeAllocationEntity>) {
        deleteAllocationsForMonth(budgetMonthId)
        insertAllocations(allocations)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM envelope_allocations WHERE budget_month_id = :budgetMonthId LIMIT 1)")
    fun hasAllocationsFlow(budgetMonthId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM envelope_allocations WHERE budget_month_id = :budgetMonthId LIMIT 1)")
    suspend fun hasAllocations(budgetMonthId: Int): Boolean

    @Query("""
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            c.bucket_type AS bucketType,
            c.color_hex AS colorHex,
            c.icon_name AS iconName,
            COALESCE(ea.allocated_amount_paise, 0) AS allocatedAmountPaise,
            COALESCE((
                SELECT SUM(t.amount_paise) 
                FROM transactions t 
                WHERE t.category_id = c.id 
                  AND t.budget_month_id = :budgetMonthId 
                  AND t.type = 'EXPENSE' 
                  AND t.is_deleted = 0
            ), 0) AS spentAmountPaise
        FROM categories c
        LEFT JOIN envelope_allocations ea ON c.id = ea.category_id AND ea.budget_month_id = :budgetMonthId
        WHERE c.is_deleted = 0 AND c.bucket_type IS NOT NULL
        ORDER BY c.display_order ASC
    """)
    fun getEnvelopesWithAllocationsFlow(budgetMonthId: Int): Flow<List<EnvelopeWithAllocation>>
}
