package com.rushi.coinmaster.ui.transfers

import com.rushi.coinmaster.data.local.entity.TransferRecipientEntity
import com.rushi.coinmaster.data.repository.TransferRecipientRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransfersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transferRecipientRepository: TransferRecipientRepository = mockk(relaxed = true)
    private lateinit var viewModel: TransfersViewModel

    private val testRecipients = listOf(
        TransferRecipientEntity(id = 1L, name = "John Doe", type = "PERSON", bankDetails = "SBI 123456")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { transferRecipientRepository.getTransferRecipientsFlow() } returns flowOf(testRecipients)
        viewModel = TransfersViewModel(transferRecipientRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRecipientsStateExposesRepositoryFlow() = runTest {
        val list = mutableListOf<List<TransferRecipientEntity>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.recipientsState.collect {
                list.add(it)
            }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(testRecipients, viewModel.recipientsState.value)
        assertEquals(testRecipients, list.last())
    }

    @Test
    fun testSaveNewRecipientInsertsInRepository() = runTest {
        viewModel.saveRecipient(0L, "Jane Doe", "PERSON", "HDFC 789")
        testScheduler.advanceUntilIdle()
        coVerify { transferRecipientRepository.insertTransferRecipient(match {
            it.id == 0L && it.name == "Jane Doe" && it.type == "PERSON" && it.bankDetails == "HDFC 789"
        }) }
    }

    @Test
    fun testSaveExistingRecipientUpdatesInRepository() = runTest {
        viewModel.saveRecipient(1L, "John Smith", "BANK", "ICICI 555")
        testScheduler.advanceUntilIdle()
        coVerify { transferRecipientRepository.updateTransferRecipient(match {
            it.id == 1L && it.name == "John Smith" && it.type == "BANK" && it.bankDetails == "ICICI 555"
        }) }
    }

    @Test
    fun testDeleteRecipientDeletesInRepository() = runTest {
        viewModel.deleteRecipient(1L)
        testScheduler.advanceUntilIdle()
        coVerify { transferRecipientRepository.softDeleteTransferRecipient(1L) }
    }
}
