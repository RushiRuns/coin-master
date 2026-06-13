package com.rushi.coinmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rushi.coinmaster.data.local.model.DebtType

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "person_name") val personName: String,
    val type: DebtType,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    @ColumnInfo(name = "remaining_paise") val remainingPaise: Long,
    @ColumnInfo(name = "due_date") val dueDate: Long? = null,
    @ColumnInfo(name = "is_settled") val isSettled: Boolean = false,
    val date: Long,
    val note: String? = null
)
