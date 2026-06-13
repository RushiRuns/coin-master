package com.rushi.coinmaster.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.TransactionRepository
import com.rushi.coinmaster.domain.usecase.GetNetWorthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class TransactionDisplayItem(
    val id: Long,
    val amountPaise: Long,
    val type: TransactionType,
    val accountName: String,
    val transferToAccountName: String?,
    val categoryName: String?,
    val categoryColorHex: String?,
    val dateMillis: Long,
    val note: String?
)

data class HomeUiState(
    val netWorth: Long = 0L,
    val accounts: List<AccountEntity> = emptyList(),
    val budgetPeriod: BudgetPeriodEntity? = null,
    val envelopes: List<EnvelopeWithAllocation> = emptyList(),
    val recentTransactions: List<TransactionDisplayItem> = emptyList(),
    val totalSpentPaise: Long = 0L,
    val totalBudgetedPaise: Long = 0L,
    val selectedCategoryDetail: EnvelopeWithAllocation? = null,
    val owedToYouPaise: Long = 0L,
    val youOwePaise: Long = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val debtRepository: com.rushi.coinmaster.data.repository.DebtRepository,
    private val getNetWorthUseCase: GetNetWorthUseCase
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    // Flow for accounts and net worth
    private val accountsFlow = accountRepository.getAccountsFlow()
    
    // Flow for debts
    private val debtsFlow = debtRepository.getDebtsFlow()

    // Combined accounts and debts flow
    private val accountsAndDebtsFlow = combine(accountsFlow, debtsFlow) { accounts, debts ->
        Pair(accounts, debts)
    }
    
    // Flow for current budget period containing today's date
    private val budgetPeriodFlow = budgetRepository.getBudgetPeriodsFlow().map { periods ->
        val today = System.currentTimeMillis()
        periods.find { today >= it.startDate && today <= it.endDate } ?: periods.firstOrNull()
    }

    // Flow for current month envelopes with allocations/spent amounts
    private val envelopesFlow = budgetPeriodFlow.flatMapLatest { period ->
        if (period != null) {
            budgetRepository.getEnvelopesWithAllocationsFlow(period.id)
        } else {
            flowOf(emptyList())
        }
    }

    // Flow for recent transactions mapped with details.
    // Uses SQL LIMIT 7 at the DB level (not .take(7) in memory) for performance.
    private val recentTransactionsFlow = combine(
        transactionRepository.getRecentTransactionsFlow(7),
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
                transferToAccountName = t.transferToAccountId?.let { id -> accountMap[id]?.name },
                categoryName = t.categoryId?.let { id -> categoryMap[id]?.name },
                categoryColorHex = t.categoryId?.let { id -> categoryMap[id]?.colorHex },
                dateMillis = t.date,
                note = t.note
            )
        }
    }

    // Expose all states combined into a single HomeUiState
    val uiState: StateFlow<HomeUiState> = combine(
        accountsAndDebtsFlow,
        budgetPeriodFlow,
        envelopesFlow,
        recentTransactionsFlow,
        _selectedCategoryId
    ) { accountsAndDebts, budgetPeriod, envelopes, recentTransactions, selectedCategoryId ->
        val accounts = accountsAndDebts.first
        val debts = accountsAndDebts.second
        val netWorth = getNetWorthUseCase(accounts, debts)
        
        // Sum total allocations and total spent for the active categories
        var totalBudgeted = 0L
        var totalSpent = 0L
        for (env in envelopes) {
            totalBudgeted += env.allocatedAmountPaise
            totalSpent += env.spentAmountPaise
        }

        val selectedDetail = envelopes.find { it.categoryId == selectedCategoryId }

        val activeDebts = debts.filter { !it.isSettled }
        val owedToYou = activeDebts.filter { it.type == com.rushi.coinmaster.data.local.model.DebtType.LENT }.sumOf { it.remainingPaise }
        val youOwe = activeDebts.filter { it.type == com.rushi.coinmaster.data.local.model.DebtType.BORROWED }.sumOf { it.remainingPaise }

        HomeUiState(
            netWorth = netWorth,
            accounts = accounts,
            budgetPeriod = budgetPeriod,
            envelopes = envelopes,
            recentTransactions = recentTransactions,
            totalSpentPaise = totalSpent,
            totalBudgetedPaise = totalBudgeted,
            selectedCategoryDetail = selectedDetail,
            owedToYouPaise = owedToYou,
            youOwePaise = youOwe
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }
}
