package com.rushi.coinmaster.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.domain.usecase.GetNetWorthUseCase
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val netWorth: Long = 0L
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getNetWorthUseCase: GetNetWorthUseCase
) : ViewModel() {

    // Expose UI state by mapping repository flow to compute net worth
    val uiState: StateFlow<AccountsUiState> = accountRepository.getAccountsFlow()
        .map { accounts ->
            val netWorth = getNetWorthUseCase(accounts)
            AccountsUiState(accounts, netWorth)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountsUiState()
        )

    fun saveAccount(
        id: Long,
        name: String,
        type: AccountType,
        balanceStr: String,
        colorHex: String,
        iconName: String
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                // Create Mode: Use input balance
                val balancePaise = MoneyMath.rupeesToPaise(balanceStr)
                val newAccount = AccountEntity(
                    name = name.trim(),
                    type = type,
                    balancePaise = balancePaise,
                    colorHex = colorHex,
                    iconName = iconName
                )
                accountRepository.insertAccount(newAccount)
            } else {
                // Edit Mode: Keep existing balance, update other properties
                val current = accountRepository.getAccountById(id) ?: return@launch
                val updated = current.copy(
                    name = name.trim(),
                    type = type,
                    colorHex = colorHex,
                    iconName = iconName
                )
                accountRepository.updateAccount(updated)
            }
        }
    }

    suspend fun getAccountById(id: Long): AccountEntity? {
        return accountRepository.getAccountById(id)
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.softDeleteAccount(account.id)
        }
    }

    fun restoreAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.updateAccount(account.copy(isDeleted = false))
        }
    }
}
