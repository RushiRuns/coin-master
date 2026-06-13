package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.entity.EnvelopeAllocationEntity
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget_periods WHERE id = :id")
    suspend fun getBudgetPeriod(id: Int): BudgetPeriodEntity?

    @Query("SELECT * FROM budget_periods ORDER BY start_date DESC")
    fun getBudgetPeriodsFlow(): Flow<List<BudgetPeriodEntity>>

    @Query("SELECT * FROM budget_periods ORDER BY start_date DESC")
    suspend fun getBudgetPeriods(): List<BudgetPeriodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetPeriod(budgetPeriod: BudgetPeriodEntity): Long

    @Update
    suspend fun updateBudgetPeriod(budgetPeriod: BudgetPeriodEntity)

    @Query("SELECT * FROM budget_periods WHERE :dateMillis >= start_date AND :dateMillis <= end_date LIMIT 1")
    suspend fun getBudgetPeriodForDate(dateMillis: Long): BudgetPeriodEntity?

    @Query("SELECT * FROM budget_periods WHERE (:endDate >= start_date) AND (:startDate <= end_date) AND id != :excludeId LIMIT 1")
    suspend fun getOverlappingPeriod(startDate: Long, endDate: Long, excludeId: Int): BudgetPeriodEntity?

    @Query("""
        UPDATE transactions 
        SET budget_period_id = (
            SELECT id FROM budget_periods 
            WHERE transactions.date >= start_date AND transactions.date <= end_date 
            LIMIT 1
        )
        WHERE is_deleted = 0
    """)
    suspend fun recalculateTransactionBudgetPeriods()

    @Query("SELECT * FROM envelope_allocations WHERE budget_period_id = :budgetPeriodId")
    fun getAllocationsFlow(budgetPeriodId: Int): Flow<List<EnvelopeAllocationEntity>>

    @Query("SELECT * FROM envelope_allocations WHERE budget_period_id = :budgetPeriodId")
    suspend fun getAllocations(budgetPeriodId: Int): List<EnvelopeAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<EnvelopeAllocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: EnvelopeAllocationEntity)

    @Query("DELETE FROM envelope_allocations WHERE budget_period_id = :budgetPeriodId")
    suspend fun deleteAllocationsForPeriod(budgetPeriodId: Int)

    @Transaction
    suspend fun replaceAllocations(budgetPeriodId: Int, allocations: List<EnvelopeAllocationEntity>) {
        deleteAllocationsForPeriod(budgetPeriodId)
        insertAllocations(allocations)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM envelope_allocations WHERE budget_period_id = :budgetPeriodId LIMIT 1)")
    fun hasAllocationsFlow(budgetPeriodId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM envelope_allocations WHERE budget_period_id = :budgetPeriodId LIMIT 1)")
    suspend fun hasAllocations(budgetPeriodId: Int): Boolean

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
                  AND t.budget_period_id = :budgetPeriodId 
                  AND t.type = 'EXPENSE' 
                  AND t.is_deleted = 0
            ), 0) AS spentAmountPaise
        FROM categories c
        LEFT JOIN envelope_allocations ea ON c.id = ea.category_id AND ea.budget_period_id = :budgetPeriodId
        WHERE c.is_deleted = 0 AND c.bucket_type IS NOT NULL
        ORDER BY c.display_order ASC
    """)
    fun getEnvelopesWithAllocationsFlow(budgetPeriodId: Int): Flow<List<EnvelopeWithAllocation>>
}
