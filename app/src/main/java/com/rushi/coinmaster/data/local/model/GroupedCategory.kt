package com.rushi.coinmaster.data.local.model

data class GroupedCategory(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val bucketType: BucketType,
    val allocatedAmountPaise: Long,
    val spentAmountPaise: Long,
    val envelopes: List<EnvelopeWithAllocation>
)
