package com.rushi.coinmaster.domain.model

data class IncomeStream(
    val id: Long = 0,
    val name: String,
    val amountPaise: Long,
    val accountId: Long?,
    val isDeleted: Boolean = false
)
