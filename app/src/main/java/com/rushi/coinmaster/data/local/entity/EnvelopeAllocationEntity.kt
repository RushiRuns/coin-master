package com.rushi.coinmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "envelope_allocations",
    indices = [
        Index("budget_month_id"),
        Index("category_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = BudgetMonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_month_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EnvelopeAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "budget_month_id") val budgetMonthId: Int,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "allocated_amount_paise") val allocatedAmountPaise: Long
)
