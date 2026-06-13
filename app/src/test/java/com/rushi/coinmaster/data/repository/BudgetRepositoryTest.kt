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
    fun `copyAllocations clones previous period allocations into new period`() = runTest {
        val previousAllocations = listOf(
            EnvelopeAllocationEntity(budgetPeriodId = 1, categoryId = 1L, allocatedAmountPaise = 5000L),
            EnvelopeAllocationEntity(budgetPeriodId = 1, categoryId = 2L, allocatedAmountPaise = 10000L)
        )
        coEvery { budgetDao.getAllocations(1) } returns previousAllocations

        repository.copyAllocations(1, 2)

        coVerify(exactly = 1) {
            budgetDao.replaceAllocations(2, match { allocations ->
                allocations.size == 2 &&
                        allocations[0].budgetPeriodId == 2 &&
                        allocations[0].categoryId == 1L &&
                        allocations[0].allocatedAmountPaise == 5000L &&
                        allocations[1].budgetPeriodId == 2 &&
                        allocations[1].categoryId == 2L &&
                        allocations[1].allocatedAmountPaise == 10000L
            })
        }
    }

    @Test
    fun `copyAllocations does nothing if previous period has no allocations`() = runTest {
        coEvery { budgetDao.getAllocations(1) } returns emptyList()

        repository.copyAllocations(1, 2)

        coVerify(exactly = 0) {
            budgetDao.replaceAllocations(any(), any())
        }
    }
}
