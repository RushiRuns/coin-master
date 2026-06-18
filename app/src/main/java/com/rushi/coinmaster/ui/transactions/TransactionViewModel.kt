package com.rushi.coinmaster.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.entity.TransferRecipientEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.TransferRecipientRepository
import com.rushi.coinmaster.data.repository.TransactionRepository
import com.rushi.coinmaster.domain.usecase.AddTransactionUseCase
import android.content.Context
import com.rushi.coinmaster.R
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TransactionUiEvent {
    object Success : TransactionUiEvent()
    data class Error(val message: String) : TransactionUiEvent()
}

@HiltViewModel
class TransactionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val transferRecipientRepository: TransferRecipientRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val accountsState: StateFlow<List<AccountEntity>> = accountRepository.getAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesState: StateFlow<List<CategoryEntity>> = budgetRepository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipientsState: StateFlow<List<TransferRecipientEntity>> = transferRecipientRepository.getTransferRecipientsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _transactionToEdit = MutableStateFlow<TransactionEntity?>(null)
    val transactionToEdit: StateFlow<TransactionEntity?> = _transactionToEdit.asStateFlow()

    private val _uiEvent = MutableSharedFlow<TransactionUiEvent>()
    val uiEvent: SharedFlow<TransactionUiEvent> = _uiEvent.asSharedFlow()

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            _transactionToEdit.value = transactionRepository.getTransactionById(id)
        }
    }

    fun saveTransaction(
        amountStr: String,
        type: TransactionType,
        accountId: Long,
        transferToAccountId: Long?,
        transferRecipientId: Long?,
        categoryId: Long?,
        date: Long,
        note: String?,
        editingTransactionId: Long = 0L
    ) {
        viewModelScope.launch {
            if (amountStr.isBlank()) {
                _uiEvent.emit(TransactionUiEvent.Error(context.getString(R.string.error_amount_empty)))
                return@launch
            }

            val amountPaise = MoneyMath.rupeesToPaise(amountStr)
            if (amountPaise <= 0L) {
                _uiEvent.emit(TransactionUiEvent.Error(context.getString(R.string.error_amount_must_be_greater_than_zero)))
                return@launch
            }

            if (accountId == 0L) {
                _uiEvent.emit(TransactionUiEvent.Error(context.getString(R.string.error_source_account_required)))
                return@launch
            }

            if (type == TransactionType.EXPENSE && categoryId == null) {
                _uiEvent.emit(TransactionUiEvent.Error(context.getString(R.string.error_category_required)))
                return@launch
            }

            if (type == TransactionType.TRANSFER) {
                if (transferToAccountId == null || transferToAccountId == 0L) {
                    _uiEvent.emit(TransactionUiEvent.Error(context.getString(R.string.error_dest_account_required)))
                    return@launch
                }
                if (accountId == transferToAccountId) {
                    _uiEvent.emit(TransactionUiEvent.Error(context.getString(R.string.error_same_accounts)))
                    return@launch
                }
            }

            if (type == TransactionType.EXTERNAL_TRANSFER) {
                if (transferRecipientId == null || transferRecipientId == 0L) {
                    _uiEvent.emit(TransactionUiEvent.Error(context.getString(R.string.error_recipient_required)))
                    return@launch
                }
            }

            // If we are editing, first delete/revert the old transaction
            if (editingTransactionId != 0L) {
                try {
                    transactionRepository.deleteTransaction(editingTransactionId)
                } catch (e: Exception) {
                    _uiEvent.emit(TransactionUiEvent.Error("Failed to update: unable to revert original transaction."))
                    return@launch
                }
            }

            val transaction = TransactionEntity(
                amountPaise = amountPaise,
                type = type,
                accountId = accountId,
                transferToAccountId = if (type == TransactionType.TRANSFER) transferToAccountId else null,
                transferRecipientId = if (type == TransactionType.EXTERNAL_TRANSFER) transferRecipientId else null,
                categoryId = if (type == TransactionType.EXPENSE || type == TransactionType.TRANSFER) categoryId else null,
                date = date,
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )

            addTransactionUseCase(transaction).fold(
                onSuccess = {
                    _uiEvent.emit(TransactionUiEvent.Success)
                },
                onFailure = { error ->
                    _uiEvent.emit(TransactionUiEvent.Error(error.message ?: context.getString(R.string.error_transaction_save_failed)))
                }
            )
        }
    }
}
