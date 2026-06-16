package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.IncomeStreamDao
import com.rushi.coinmaster.data.local.entity.IncomeStreamEntity
import com.rushi.coinmaster.domain.model.IncomeStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeStreamRepositoryImpl @Inject constructor(
    private val incomeStreamDao: IncomeStreamDao
) : IncomeStreamRepository {

    override fun getIncomeStreamsFlow(): Flow<List<IncomeStream>> {
        return incomeStreamDao.getIncomeStreamsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getIncomeStreams(): List<IncomeStream> {
        return incomeStreamDao.getIncomeStreams().map { it.toDomain() }
    }

    override suspend fun getIncomeStreamById(id: Long): IncomeStream? {
        return incomeStreamDao.getIncomeStreamById(id)?.toDomain()
    }

    override suspend fun insertIncomeStream(incomeStream: IncomeStream): Long {
        return incomeStreamDao.insertIncomeStream(incomeStream.toEntity())
    }

    override suspend fun updateIncomeStream(incomeStream: IncomeStream) {
        incomeStreamDao.updateIncomeStream(incomeStream.toEntity())
    }

    override suspend fun softDeleteIncomeStream(id: Long) {
        incomeStreamDao.softDeleteIncomeStream(id)
    }

    private fun IncomeStreamEntity.toDomain(): IncomeStream {
        return IncomeStream(
            id = id,
            name = name,
            amountPaise = amountPaise,
            accountId = accountId,
            isDeleted = isDeleted
        )
    }

    private fun IncomeStream.toEntity(): IncomeStreamEntity {
        return IncomeStreamEntity(
            id = id,
            name = name,
            amountPaise = amountPaise,
            accountId = accountId,
            isDeleted = isDeleted
        )
    }
}
