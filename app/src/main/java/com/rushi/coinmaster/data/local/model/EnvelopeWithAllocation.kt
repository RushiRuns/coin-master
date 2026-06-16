package com.rushi.coinmaster.data.local.model

data class EnvelopeWithAllocation(
    val categoryId: Long,
    val categoryName: String,
    val bucketType: BucketType,
    val colorHex: String,
    val iconName: String,
    val allocatedAmountPaise: Long,
    val spentAmountPaise: Long,
    val expenseCategoryId: Long? = null,
    val expenseType: ExpenseType = ExpenseType.VARIABLE
)
