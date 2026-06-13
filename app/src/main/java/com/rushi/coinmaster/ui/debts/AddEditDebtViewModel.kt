package com.rushi.coinmaster.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.local.model.DebtType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.DebtRepository
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DebtUiEvent {
    object Success : DebtUiEvent()
    data class Error(val message: String) : DebtUiEvent()
}

@HiltViewModel
class AddEditDebtViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val accountsState: StateFlow<List<AccountEntity>> = accountRepository.getAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<DebtUiEvent>()
    val uiEvent: SharedFlow<DebtUiEvent> = _uiEvent.asSharedFlow()

    fun saveDebt(
        personName: String,
        type: DebtType,
        amountStr: String,
        accountId: Long,
        dueDate: Long?,
        note: String?
    ) {
        viewModelScope.launch {
            if (personName.isBlank()) {
                _uiEvent.emit(DebtUiEvent.Error("Person name is required"))
                return@launch
            }

            if (amountStr.isBlank()) {
                _uiEvent.emit(DebtUiEvent.Error("Amount is required"))
                return@launch
            }

            val amountPaise = MoneyMath.rupeesToPaise(amountStr)
            if (amountPaise <= 0L) {
                _uiEvent.emit(DebtUiEvent.Error("Amount must be greater than zero"))
                return@launch
            }

            if (accountId == 0L) {
                _uiEvent.emit(DebtUiEvent.Error("An account must be linked for the initial transaction"))
                return@launch
            }

            // Find default category ID for "Lending & Debts"
            val categories = budgetRepository.getCategoriesFlow().first()
            val categoryId = categories.find { it.name.equals("Lending & Debts", ignoreCase = true) }?.id

            val debt = DebtEntity(
                personName = personName.trim(),
                type = type,
                amountPaise = amountPaise,
                remainingPaise = amountPaise,
                dueDate = dueDate,
                isSettled = false,
                date = System.currentTimeMillis(),
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )

            try {
                debtRepository.insertDebtWithTransaction(
                    debt = debt,
                    accountId = accountId,
                    categoryId = categoryId,
                    dateMillis = System.currentTimeMillis()
                )
                _uiEvent.emit(DebtUiEvent.Success)
            } catch (e: Exception) {
                _uiEvent.emit(DebtUiEvent.Error(e.message ?: "Failed to save debt"))
            }
        }
    }
}
