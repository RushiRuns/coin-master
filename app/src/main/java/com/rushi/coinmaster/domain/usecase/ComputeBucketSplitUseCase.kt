package com.rushi.coinmaster.domain.usecase

import javax.inject.Inject

class ComputeBucketSplitUseCase @Inject constructor() {
    operator fun invoke(incomePaise: Long, needsPercent: Int, wantsPercent: Int, savingsPercent: Int): BucketSplit {
        val needs = (incomePaise * needsPercent) / 100
        val wants = (incomePaise * wantsPercent) / 100
        val savings = incomePaise - needs - wants // Remainder to savings to protect integer precision (Principle III)
        return BucketSplit(needs, wants, savings)
    }
}

data class BucketSplit(
    val needsPaise: Long,
    val wantsPaise: Long,
    val savingsPaise: Long
)
