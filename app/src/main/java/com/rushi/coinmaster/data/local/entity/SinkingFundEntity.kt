package com.rushi.coinmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sinking_funds",
    indices = [
        Index("category_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SinkingFundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "target_amount_paise") val targetAmountPaise: Long,
    @ColumnInfo(name = "saved_amount_paise") val savedAmountPaise: Long,
    @ColumnInfo(name = "target_date") val targetDate: Long, // Epoch millis
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false
)
