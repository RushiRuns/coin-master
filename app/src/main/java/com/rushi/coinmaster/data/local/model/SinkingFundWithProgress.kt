package com.rushi.coinmaster.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.rushi.coinmaster.data.local.entity.SinkingFundEntity

data class SinkingFundWithProgress(
    @Embedded val sinkingFund: SinkingFundEntity,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "computed_saved_amount") val computedSavedAmountPaise: Long
)
