package com.rushi.coinmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rushi.coinmaster.data.local.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index("account_id"),
        Index("transfer_to_account_id"),
        Index("category_id"),
        Index("budget_month_id"),
        Index("debt_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["transfer_to_account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BudgetMonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_month_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debt_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    val type: TransactionType,
    @ColumnInfo(name = "account_id") val accountId: Long,
    @ColumnInfo(name = "transfer_to_account_id") val transferToAccountId: Long? = null,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "budget_month_id") val budgetMonthId: Int? = null,
    @ColumnInfo(name = "debt_id") val debtId: Long? = null,
    val date: Long, // Epoch millis
    val note: String? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
