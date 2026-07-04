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

data class SpendingSummary(
    val todayPaise: Long = 0L,
    val weeklyPaise: Long = 0L,
    val monthlyPaise: Long = 0L
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

    val spendingSummary: StateFlow<SpendingSummary> = flow {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        // Today Start
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        // Week Start
        val calWeek = Calendar.getInstance()
        calWeek.timeInMillis = now
        calWeek.set(Calendar.HOUR_OF_DAY, 0)
        calWeek.set(Calendar.MINUTE, 0)
        calWeek.set(Calendar.SECOND, 0)
        calWeek.set(Calendar.MILLISECOND, 0)
        calWeek.set(Calendar.DAY_OF_WEEK, calWeek.firstDayOfWeek)
        val weekStart = calWeek.timeInMillis

        // Month Start
        val calMonth = Calendar.getInstance()
        calMonth.timeInMillis = now
        calMonth.set(Calendar.DAY_OF_MONTH, 1)
        calMonth.set(Calendar.HOUR_OF_DAY, 0)
        calMonth.set(Calendar.MINUTE, 0)
        calMonth.set(Calendar.SECOND, 0)
        calMonth.set(Calendar.MILLISECOND, 0)
        val monthStart = calMonth.timeInMillis

        val minStart = minOf(todayStart, weekStart, monthStart)

        emitAll(
            transactionRepository.getTransactionsBetweenDatesFlow(minStart, Long.MAX_VALUE).map { transactions ->
                val todayTotal = transactions
                    .filter { it.date >= todayStart && it.type == TransactionType.EXPENSE }
                    .sumOf { it.amountPaise }
                val weeklyTotal = transactions
                    .filter { it.date >= weekStart && it.type == TransactionType.EXPENSE }
                    .sumOf { it.amountPaise }
                val monthlyTotal = transactions
                    .filter { it.date >= monthStart && it.type == TransactionType.EXPENSE }
                    .sumOf { it.amountPaise }
                SpendingSummary(todayTotal, weeklyTotal, monthlyTotal)
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SpendingSummary(0, 0, 0)
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    data class DeepLinkFilter(
        val categoryIds: List<Long>,
        val startMillis: Long,
        val endMillis: Long,
        val displayLabel: String
    )

    private val _deepLinkFilter = MutableStateFlow<DeepLinkFilter?>(null)
    val deepLinkFilter: StateFlow<DeepLinkFilter?> = _deepLinkFilter.asStateFlow()

    fun applyDeepLinkFilter(filter: DeepLinkFilter) {
        _deepLinkFilter.value = filter
    }

    fun clearDeepLinkFilter() {
        _deepLinkFilter.value = null
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        _activeTab,
        _selectedFilter,
        _selectedDateMillis,
        _searchQuery,
        _deepLinkFilter
    ) { tab, filter, dateMillis, query, deepLink ->
        QueryParameters(tab, filter, dateMillis, query, deepLink)
    }.flatMapLatest { params ->
        val transactionsFlow = if (params.deepLink != null) {
            transactionRepository.getTransactionsByCategoryIdsFlow(
                params.deepLink.categoryIds,
                params.deepLink.startMillis,
                params.deepLink.endMillis
            )
        } else {
            val range = getRange(params.tab, params.filter, params.dateMillis)
            transactionRepository.getTransactionsBetweenDatesFlow(range.first, range.second)
        }

        combine(
            transactionsFlow,
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
            }.filter { item ->
                if (params.query.isBlank()) {
                    true
                } else {
                    val q = params.query.trim().lowercase()
                    val noteMatch = item.note?.lowercase()?.contains(q) == true
                    val categoryMatch = item.categoryName?.lowercase()?.contains(q) == true
                    val accountMatch = item.accountName.lowercase().contains(q)
                    val transferMatch = item.transferToAccountName?.lowercase()?.contains(q) == true
                    val amountStr = String.format("%.2f", item.amountPaise / 100.0)
                    val amountMatch = amountStr.contains(q)
                    noteMatch || categoryMatch || accountMatch || transferMatch || amountMatch
                }
            }
            TransactionsUiState(displayItems, params.dateMillis)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

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

private data class QueryParameters(
    val tab: Int,
    val filter: TransactionFilter,
    val dateMillis: Long,
    val query: String,
    val deepLink: TransactionsListViewModel.DeepLinkFilter? = null
)
