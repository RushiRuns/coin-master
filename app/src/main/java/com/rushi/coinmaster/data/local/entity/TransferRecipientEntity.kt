package com.rushi.coinmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_recipients")
data class TransferRecipientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "PERSON" or "BANK"
    @ColumnInfo(name = "bank_details") val bankDetails: String?,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
