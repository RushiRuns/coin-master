package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts ORDER BY date DESC")
    fun getDebtsFlow(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE is_settled = 0 ORDER BY date DESC")
    fun getActiveDebtsFlow(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE is_settled = 1 ORDER BY date DESC")
    fun getSettledDebtsFlow(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Query("SELECT * FROM debts WHERE id = :id")
    fun getDebtByIdFlow(id: Long): Flow<DebtEntity?>

    @Query("SELECT * FROM debts")
    suspend fun getDebts(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)

    @Query("SELECT * FROM transactions WHERE debt_id = :debtId AND is_deleted = 0 ORDER BY date DESC")
    fun getTransactionsForDebtFlow(debtId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE debt_id = :debtId AND is_deleted = 0 ORDER BY date DESC")
    suspend fun getTransactionsForDebt(debtId: Long): List<TransactionEntity>
}
