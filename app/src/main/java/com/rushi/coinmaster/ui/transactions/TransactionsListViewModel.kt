package com.rushi.coinmaster.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.TransactionRepository
import com.rushi.coinmaster.ui.home.TransactionDisplayItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class TransactionFilter {
    DAY, WEEK, MONTH
}

data class TransactionsUiState(
    val transactions: List<TransactionDisplayItem> = emptyList(),
    val selectedDateMillis: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsListViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TransactionFilter.DAY)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter.asStateFlow()

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: All Transactions, 1: Daily Activity
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    val uiState: StateFlow<TransactionsUiState> = combine(
        _activeTab,
        _selectedFilter,
        _selectedDateMillis
    ) { tab, filter, dateMillis ->
        Triple(tab, filter, dateMillis)
    }.flatMapLatest { (tab, filter, dateMillis) ->
        val range = getRange(tab, filter, dateMillis)
        combine(
            transactionRepository.getTransactionsBetweenDatesFlow(range.first, range.second),
            accountRepository.getAccountsFlow(),
            budgetRepository.getCategoriesFlow()
        ) { transactions, accounts, categories ->
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }

            val displayItems = transactions.map { t ->
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
            TransactionsUiState(displayItems, dateMillis)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun setFilter(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    fun setDate(dateMillis: Long) {
        _selectedDateMillis.value = dateMillis
    }

    fun setTab(tab: Int) {
        _activeTab.value = tab
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    private fun getRange(tab: Int, filter: TransactionFilter, dateMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        if (tab == 1) {
            // Daily Activity Date range
            calendar.timeInMillis = dateMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val end = calendar.timeInMillis
            return Pair(start, end)
        } else {
            // Filter based ranges (for today/now context)
            val today = System.currentTimeMillis()
            calendar.timeInMillis = today
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val end = calendar.timeInMillis

            val calendarStart = Calendar.getInstance()
            calendarStart.timeInMillis = today
            calendarStart.set(Calendar.HOUR_OF_DAY, 0)
            calendarStart.set(Calendar.MINUTE, 0)
            calendarStart.set(Calendar.SECOND, 0)
            calendarStart.set(Calendar.MILLISECOND, 0)

            when (filter) {
                TransactionFilter.DAY -> {
                    // Start of today
                }
                TransactionFilter.WEEK -> {
                    // Start of calendar week
                    calendarStart.set(Calendar.DAY_OF_WEEK, calendarStart.firstDayOfWeek)
                }
                TransactionFilter.MONTH -> {
                    // Start of calendar month
                    calendarStart.set(Calendar.DAY_OF_MONTH, 1)
                }
            }
            return Pair(calendarStart.timeInMillis, end)
        }
    }
}
