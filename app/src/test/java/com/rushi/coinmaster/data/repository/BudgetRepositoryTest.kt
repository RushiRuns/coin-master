package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.BudgetDao
import com.rushi.coinmaster.data.local.dao.CategoryDao
import com.rushi.coinmaster.data.local.entity.EnvelopeAllocationEntity
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepositoryTest {

    private val budgetDao: BudgetDao = mockk(relaxed = true)
    private val categoryDao: CategoryDao = mockk(relaxed = true)
    private val repository = BudgetRepository(budgetDao, categoryDao)

    @Test
    fun `copyAllocations clones previous month allocations into new month`() = runTest {
        val previousAllocations = listOf(
            EnvelopeAllocationEntity(budgetMonthId = 202601, categoryId = 1L, allocatedAmountPaise = 5000L),
            EnvelopeAllocationEntity(budgetMonthId = 202601, categoryId = 2L, allocatedAmountPaise = 10000L)
        )
        coEvery { budgetDao.getAllocations(202601) } returns previousAllocations

        repository.copyAllocations(202601, 202602)

        coVerify(exactly = 1) {
            budgetDao.replaceAllocations(202602, match { allocations ->
                allocations.size == 2 &&
                        allocations[0].budgetMonthId == 202602 &&
                        allocations[0].categoryId == 1L &&
                        allocations[0].allocatedAmountPaise == 5000L &&
                        allocations[1].budgetMonthId == 202602 &&
                        allocations[1].categoryId == 2L &&
                        allocations[1].allocatedAmountPaise == 10000L
            })
        }
    }

    @Test
    fun `copyAllocations does nothing if previous month has no allocations`() = runTest {
        coEvery { budgetDao.getAllocations(202601) } returns emptyList()

        repository.copyAllocations(202601, 202602)

        coVerify(exactly = 0) {
            budgetDao.replaceAllocations(any(), any())
        }
    }
}
