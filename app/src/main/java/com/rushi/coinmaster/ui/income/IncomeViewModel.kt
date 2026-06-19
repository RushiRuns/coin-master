package com.rushi.coinmaster.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.IncomeStreamRepository
import com.rushi.coinmaster.domain.model.IncomeStream
import com.rushi.coinmaster.domain.usecase.AddTransactionUseCase
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val incomeStreamRepository: IncomeStreamRepository,
    private val accountRepository: AccountRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    /** Live list of active (non-deleted) income streams. */
    val incomeStreams: StateFlow<List<IncomeStream>> = incomeStreamRepository
        .getIncomeStreamsFlow()
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Total of all active income streams in paise. */
    val totalIncomePaise: StateFlow<Long> = incomeStreams
        .map { list -> list.sumOf { it.amountPaise } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    /** Live list of active accounts. */
    val accounts: StateFlow<List<AccountEntity>> = accountRepository
        .getAccountsFlow()
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<IncomeUiEvent>()
    val uiEvent: SharedFlow<IncomeUiEvent> = _uiEvent.asSharedFlow()

    /**
     * Add a new income stream.
     * @param name   Display name for the source (e.g. "Salary").
     * @param amountRupees Amount in decimal rupees (will be converted to paise).
     * @param accountId Target account ID where this income is deposited.
     */
    fun addIncomeStream(name: String, amountRupees: Double, accountId: Long) {
        viewModelScope.launch {
            val amountPaise = MoneyMath.rupeesToPaise(amountRupees)
            incomeStreamRepository.insertIncomeStream(
                IncomeStream(
                    name = name,
                    amountPaise = amountPaise,
                    accountId = accountId
                )
            )
            val transaction = TransactionEntity(
                amountPaise = amountPaise,
                type = TransactionType.INCOME,
                accountId = accountId,
                date = System.currentTimeMillis(),
                note = "Income Stream: $name"
            )
            addTransactionUseCase(transaction)
            syncPlannedBudgetPeriods()
            _uiEvent.emit(IncomeUiEvent.ShowToast("Income stream added"))
        }
    }

    /** Soft-delete an income stream by id. */
    fun deleteIncomeStream(id: Long) {
        viewModelScope.launch {
            incomeStreamRepository.softDeleteIncomeStream(id)
            syncPlannedBudgetPeriods()
            _uiEvent.emit(IncomeUiEvent.ShowToast("Income stream deleted"))
        }
    }

    /**
     * Deposit the income stream amount into its target account.
     */
    fun depositIncomeStream(stream: IncomeStream) {
        val accountId = stream.accountId
        if (accountId == null) {
            viewModelScope.launch {
                _uiEvent.emit(IncomeUiEvent.ShowToast("Error: No account linked to this income stream."))
            }
            return
        }

        viewModelScope.launch {
            val transaction = TransactionEntity(
                amountPaise = stream.amountPaise,
                type = TransactionType.INCOME,
                accountId = accountId,
                date = System.currentTimeMillis(),
                note = "Income Stream: ${stream.name}"
            )

            val result = addTransactionUseCase(transaction)
            if (result.isSuccess) {
                _uiEvent.emit(IncomeUiEvent.ShowToast("Deposited ${stream.name} successfully!"))
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiEvent.emit(IncomeUiEvent.ShowToast("Failed to deposit: $errorMsg"))
            }
        }
    }

    private suspend fun syncPlannedBudgetPeriods() {
        try {
            val activeStreams = incomeStreamRepository.getIncomeStreams()
            val totalIncomePaise = activeStreams.filter { !it.isDeleted }.sumOf { it.amountPaise }
            val periods = budgetRepository.getBudgetPeriods()
            periods.forEach { period ->
                if (!period.isActive && period.incomePaise != totalIncomePaise) {
                    budgetRepository.updateBudgetPeriod(period.copy(incomePaise = totalIncomePaise))
                }
            }
        } catch (e: Exception) {
            // Ignore/handle silently
        }
    }
}

sealed interface IncomeUiEvent {
    data class ShowToast(val message: String) : IncomeUiEvent
}
