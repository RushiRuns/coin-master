package com.rushi.coinmaster.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.domain.usecase.AddTransactionUseCase
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TransactionUiEvent {
    object Success : TransactionUiEvent()
    data class Error(val message: String) : TransactionUiEvent()
}

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    val accountsState: StateFlow<List<AccountEntity>> = accountRepository.getAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesState: StateFlow<List<CategoryEntity>> = budgetRepository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<TransactionUiEvent>()
    val uiEvent: SharedFlow<TransactionUiEvent> = _uiEvent.asSharedFlow()

    fun saveTransaction(
        amountStr: String,
        type: TransactionType,
        accountId: Long,
        transferToAccountId: Long?,
        categoryId: Long?,
        date: Long,
        note: String?
    ) {
        viewModelScope.launch {
            if (amountStr.isBlank()) {
                _uiEvent.emit(TransactionUiEvent.Error("Amount cannot be empty."))
                return@launch
            }

            val amountPaise = MoneyMath.rupeesToPaise(amountStr)
            if (amountPaise <= 0L) {
                _uiEvent.emit(TransactionUiEvent.Error("Amount must be greater than zero."))
                return@launch
            }

            if (accountId == 0L) {
                _uiEvent.emit(TransactionUiEvent.Error("Please select a source account."))
                return@launch
            }

            if (type == TransactionType.EXPENSE && categoryId == null) {
                _uiEvent.emit(TransactionUiEvent.Error("Please select a category."))
                return@launch
            }

            if (type == TransactionType.TRANSFER) {
                if (transferToAccountId == null || transferToAccountId == 0L) {
                    _uiEvent.emit(TransactionUiEvent.Error("Please select a destination account."))
                    return@launch
                }
                if (accountId == transferToAccountId) {
                    _uiEvent.emit(TransactionUiEvent.Error("Source and destination accounts must be different."))
                    return@launch
                }
            }

            val transaction = TransactionEntity(
                amountPaise = amountPaise,
                type = type,
                accountId = accountId,
                transferToAccountId = if (type == TransactionType.TRANSFER) transferToAccountId else null,
                categoryId = if (type == TransactionType.EXPENSE) categoryId else null,
                date = date,
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )

            addTransactionUseCase(transaction).fold(
                onSuccess = {
                    _uiEvent.emit(TransactionUiEvent.Success)
                },
                onFailure = { error ->
                    _uiEvent.emit(TransactionUiEvent.Error(error.message ?: "Failed to save transaction."))
                }
            )
        }
    }
}
