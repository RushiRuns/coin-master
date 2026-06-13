package com.rushi.coinmaster.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.DebtRepository
import com.rushi.coinmaster.ui.home.TransactionDisplayItem
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DebtDetailsEvent {
    object Success : DebtDetailsEvent()
    data class Error(val message: String) : DebtDetailsEvent()
    object DebtDeleted : DebtDetailsEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DebtDetailsViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _debtId = MutableStateFlow<Long?>(null)

    val debtState: StateFlow<DebtEntity?> = _debtId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else debtRepository.getDebtByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accountsState: StateFlow<List<AccountEntity>> = accountRepository.getAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsState: StateFlow<List<TransactionDisplayItem>> = _debtId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else {
                combine(
                    debtRepository.getTransactionsForDebtFlow(id),
                    accountRepository.getAccountsFlow(),
                    budgetRepository.getCategoriesFlow()
                ) { transactions, accounts, categories ->
                    val accountMap = accounts.associateBy { it.id }
                    val categoryMap = categories.associateBy { it.id }

                    transactions.map { t ->
                        TransactionDisplayItem(
                            id = t.id,
                            amountPaise = t.amountPaise,
                            type = t.type,
                            accountName = accountMap[t.accountId]?.name ?: "Unknown",
                            transferToAccountName = t.transferToAccountId?.let { toId -> accountMap[toId]?.name },
                            categoryName = t.categoryId?.let { cId -> categoryMap[cId]?.name },
                            categoryColorHex = t.categoryId?.let { cId -> categoryMap[cId]?.colorHex },
                            dateMillis = t.date,
                            note = t.note
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventFlow = MutableSharedFlow<DebtDetailsEvent>()
    val eventFlow: SharedFlow<DebtDetailsEvent> = _eventFlow.asSharedFlow()

    fun loadDebt(id: Long) {
        _debtId.value = id
    }

    fun recordRepayment(amountStr: String, accountId: Long, note: String?) {
        val debtId = _debtId.value ?: return
        viewModelScope.launch {
            if (amountStr.isBlank()) {
                _eventFlow.emit(DebtDetailsEvent.Error("Amount is required"))
                return@launch
            }

            val amountPaise = MoneyMath.rupeesToPaise(amountStr)
            if (amountPaise <= 0L) {
                _eventFlow.emit(DebtDetailsEvent.Error("Amount must be greater than zero"))
                return@launch
            }

            val currentDebt = debtState.value
            if (currentDebt == null) {
                _eventFlow.emit(DebtDetailsEvent.Error("Debt details not loaded"))
                return@launch
            }

            if (amountPaise > currentDebt.remainingPaise) {
                _eventFlow.emit(DebtDetailsEvent.Error("Repayment amount cannot exceed remaining debt"))
                return@launch
            }

            if (accountId == 0L) {
                _eventFlow.emit(DebtDetailsEvent.Error("Account is required"))
                return@launch
            }

            try {
                debtRepository.recordRepayment(
                    debtId = debtId,
                    amountPaise = amountPaise,
                    accountId = accountId,
                    dateMillis = System.currentTimeMillis(),
                    note = note?.trim()?.takeIf { it.isNotEmpty() }
                )
                _eventFlow.emit(DebtDetailsEvent.Success)
            } catch (e: Exception) {
                _eventFlow.emit(DebtDetailsEvent.Error(e.message ?: "Failed to record repayment"))
            }
        }
    }

    fun deleteDebt() {
        val debtId = _debtId.value ?: return
        viewModelScope.launch {
            try {
                debtRepository.deleteDebt(debtId)
                _eventFlow.emit(DebtDetailsEvent.DebtDeleted)
            } catch (e: Exception) {
                _eventFlow.emit(DebtDetailsEvent.Error(e.message ?: "Failed to delete debt"))
            }
        }
    }
}
