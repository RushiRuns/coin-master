package com.rushi.coinmaster.ui.home

import com.rushi.coinmaster.data.local.model.TransactionType

data class TransactionDisplayItem(
    val id: Long,
    val amountPaise: Long,
    val type: TransactionType,
    val accountName: String,
    val transferToAccountName: String?,
    val categoryName: String?,
    val categoryColorHex: String?,
    val dateMillis: Long,
    val note: String?
)
