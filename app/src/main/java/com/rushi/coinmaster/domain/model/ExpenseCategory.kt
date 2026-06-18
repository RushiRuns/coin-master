package com.rushi.coinmaster.domain.model

import com.rushi.coinmaster.data.local.model.BucketType

data class ExpenseCategory(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val isDeleted: Boolean = false,
    val bucketType: BucketType? = null
)
