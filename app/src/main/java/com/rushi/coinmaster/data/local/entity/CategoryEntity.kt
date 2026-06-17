package com.rushi.coinmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.ExpenseType

@Entity(
    tableName = "categories",
    indices = [
        Index("expense_category_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "bucket_type") val bucketType: BucketType? = null,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "icon_name") val iconName: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "expense_category_id") val expenseCategoryId: Long? = null,
    @ColumnInfo(name = "expense_type") val expenseType: ExpenseType? = null
)
