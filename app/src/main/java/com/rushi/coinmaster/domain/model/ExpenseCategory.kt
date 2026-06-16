package com.rushi.coinmaster.domain.model

data class ExpenseCategory(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val isDeleted: Boolean = false
)
