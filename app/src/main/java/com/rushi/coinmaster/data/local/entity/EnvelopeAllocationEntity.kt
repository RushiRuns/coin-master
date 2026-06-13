package com.rushi.coinmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "envelope_allocations",
    indices = [
        Index(value = ["budget_period_id", "category_id"], unique = true),
        Index("category_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = BudgetPeriodEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_period_id"],
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
    @ColumnInfo(name = "budget_period_id") val budgetPeriodId: Int,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "allocated_amount_paise") val allocatedAmountPaise: Long
)

