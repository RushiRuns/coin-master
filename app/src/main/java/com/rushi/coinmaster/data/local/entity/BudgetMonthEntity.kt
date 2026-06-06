package com.rushi.coinmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_months")
data class BudgetMonthEntity(
    @PrimaryKey val id: Int, // YYYYMM format, e.g., 202606
    val month: Int, // 1 - 12
    val year: Int,
    @ColumnInfo(name = "income_paise") val incomePaise: Long,
    @ColumnInfo(name = "needs_percent") val needsPercent: Int = 50,
    @ColumnInfo(name = "wants_percent") val wantsPercent: Int = 30,
    @ColumnInfo(name = "savings_percent") val savingsPercent: Int = 20,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false
)
