package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.SinkingFundDao
import com.rushi.coinmaster.data.local.entity.SinkingFundEntity
import com.rushi.coinmaster.data.local.model.SinkingFundWithProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SinkingFundRepository @Inject constructor(
    private val sinkingFundDao: SinkingFundDao
) {
    fun getSinkingFundsFlow(): Flow<List<SinkingFundEntity>> = sinkingFundDao.getSinkingFundsFlow()

    fun getSinkingFundsWithProgressFlow(): Flow<List<SinkingFundWithProgress>> = 
        sinkingFundDao.getSinkingFundsWithProgressFlow()

    suspend fun getSinkingFundById(id: Long): SinkingFundEntity? = sinkingFundDao.getSinkingFundById(id)

    suspend fun insertSinkingFund(sinkingFund: SinkingFundEntity): Long = sinkingFundDao.insertSinkingFund(sinkingFund)

    suspend fun updateSinkingFund(sinkingFund: SinkingFundEntity) = sinkingFundDao.updateSinkingFund(sinkingFund)

    suspend fun deleteSinkingFund(sinkingFund: SinkingFundEntity) = sinkingFundDao.deleteSinkingFund(sinkingFund)
}
