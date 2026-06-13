package com.rushi.coinmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_periods")
data class BudgetPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "start_date") val startDate: Long, // Epoch millis (start of day)
    @ColumnInfo(name = "end_date") val endDate: Long,     // Epoch millis (end of day)
    @ColumnInfo(name = "income_paise") val incomePaise: Long,
    @ColumnInfo(name = "needs_percent") val needsPercent: Int = 50,
    @ColumnInfo(name = "wants_percent") val wantsPercent: Int = 30,
    @ColumnInfo(name = "savings_percent") val savingsPercent: Int = 20,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false
)
