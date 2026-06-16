package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.domain.model.IncomeStream
import kotlinx.coroutines.flow.Flow

interface IncomeStreamRepository {
    fun getIncomeStreamsFlow(): Flow<List<IncomeStream>>
    suspend fun getIncomeStreams(): List<IncomeStream>
    suspend fun getIncomeStreamById(id: Long): IncomeStream?
    suspend fun insertIncomeStream(incomeStream: IncomeStream): Long
    suspend fun updateIncomeStream(incomeStream: IncomeStream)
    suspend fun softDeleteIncomeStream(id: Long)
}
