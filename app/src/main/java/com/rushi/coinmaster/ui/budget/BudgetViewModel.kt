package com.rushi.coinmaster.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.ExpenseType
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.domain.usecase.AddTransactionUseCase
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.domain.usecase.BudgetValidationResult
import com.rushi.coinmaster.domain.usecase.ValidateZeroBalanceUseCase
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import java.util.Calendar
import javax.inject.Inject
import com.rushi.coinmaster.R
import dagger.hilt.android.qualifiers.ApplicationContext

sealed class BudgetUiEvent {
    data class Error(val message: String) : BudgetUiEvent()
    object SuccessActivation : BudgetUiEvent()
    object SuccessSave : BudgetUiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val validateZeroBalanceUseCase: ValidateZeroBalanceUseCase,
    private val expenseCategoryRepository: com.rushi.coinmaster.data.repository.ExpenseCategoryRepository,
    private val incomeStreamRepository: com.rushi.coinmaster.data.repository.IncomeStreamRepository,
    private val accountRepository: AccountRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    // Expose all active accounts
    val activeAccountsState: StateFlow<List<AccountEntity>> = accountRepository.getAccountsFlow()
        .map { accounts -> accounts.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun withdrawFromSavings(
        categoryId: Long,
        sourceAccountId: Long,
        destAccountId: Long,
        amountStr: String,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            if (amountStr.isBlank()) {
                onResult(Result.failure(IllegalArgumentException("Amount cannot be empty.")))
                return@launch
            }
            val amountPaise = try {
                MoneyMath.rupeesToPaise(amountStr)
            } catch (e: Exception) {
                onResult(Result.failure(IllegalArgumentException("Invalid amount format.")))
                return@launch
            }
            if (amountPaise <= 0L) {
                onResult(Result.failure(IllegalArgumentException("Amount must be greater than zero.")))
                return@launch
            }
            if (sourceAccountId == destAccountId) {
                onResult(Result.failure(IllegalArgumentException("Source and destination accounts must be different.")))
                return@launch
            }

            val budgetPeriod = budgetPeriodState.value ?: budgetRepository.getOrCreateBudgetPeriodForDate(System.currentTimeMillis())

            val transaction = TransactionEntity(
                amountPaise = amountPaise,
                type = TransactionType.TRANSFER,
                accountId = sourceAccountId,
                transferToAccountId = destAccountId,
                categoryId = categoryId,
                budgetPeriodId = budgetPeriod.id,
                date = System.currentTimeMillis(),
                note = "Withdrawal from savings"
            )

            try {
                val result = addTransactionUseCase(transaction)
                onResult(result)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    // Expose the computed sum of all active income streams
    val totalIncomeStreamsPaise: StateFlow<Long> = incomeStreamRepository.getIncomeStreamsFlow()
        .map { list -> list.filter { !it.isDeleted }.sumOf { it.amountPaise } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _selectedPeriodId = MutableStateFlow<Int?>(null)
    val selectedPeriodId: StateFlow<Int?> = _selectedPeriodId.asStateFlow()

    // Reactively track the current budget period entity
    val budgetPeriodState: StateFlow<BudgetPeriodEntity?> = _selectedPeriodId.flatMapLatest { periodId ->
        if (periodId == null) {
            flowOf(null)
        } else {
            budgetRepository.getBudgetPeriodsFlow().map { periods ->
                periods.find { it.id == periodId }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Reactively track categories combined with allocations/spent for the period
    val envelopesState: StateFlow<List<EnvelopeWithAllocation>> = _selectedPeriodId.flatMapLatest { periodId ->
        if (periodId == null) {
            flowOf(emptyList())
        } else {
            budgetRepository.getEnvelopesWithAllocationsFlow(periodId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parentCategoriesState: StateFlow<List<com.rushi.coinmaster.domain.model.ExpenseCategory>> = expenseCategoryRepository.getExpenseCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedCategoriesState: StateFlow<List<com.rushi.coinmaster.data.local.model.GroupedCategory>> = combine(
        envelopesState,
        parentCategoriesState
    ) { envelopes, parents ->
        val parentMap = parents.associateBy { it.id }
        val groupedEnvelopes = envelopes.groupBy { it.expenseCategoryId }
        val resultList = mutableListOf<com.rushi.coinmaster.data.local.model.GroupedCategory>()

        parentMap.forEach { (parentId, parentCategory) ->
            val list = groupedEnvelopes[parentId] ?: emptyList()

            if (parentCategory.bucketType == BucketType.NEEDS) {
                val needsEnvelopes = list.filter { it.bucketType == BucketType.NEEDS }
                resultList.add(
                    com.rushi.coinmaster.data.local.model.GroupedCategory(
                        id = parentId,
                        name = parentCategory.name,
                        colorHex = parentCategory.colorHex,
                        iconName = parentCategory.iconName,
                        bucketType = BucketType.NEEDS,
                        allocatedAmountPaise = needsEnvelopes.sumOf { it.allocatedAmountPaise },
                        spentAmountPaise = needsEnvelopes.sumOf { it.spentAmountPaise },
                        envelopes = needsEnvelopes
                    )
                )
            } else if (parentCategory.bucketType == BucketType.WANTS) {
                val wantsEnvelopes = list.filter { it.bucketType == BucketType.WANTS }
                resultList.add(
                    com.rushi.coinmaster.data.local.model.GroupedCategory(
                        id = parentId,
                        name = parentCategory.name,
                        colorHex = parentCategory.colorHex,
                        iconName = parentCategory.iconName,
                        bucketType = BucketType.WANTS,
                        allocatedAmountPaise = wantsEnvelopes.sumOf { it.allocatedAmountPaise },
                        spentAmountPaise = wantsEnvelopes.sumOf { it.spentAmountPaise },
                        envelopes = wantsEnvelopes
                    )
                )
            } else {
                // Fallback for implicit categories matching child envelopes
                val needsEnvelopes = list.filter { it.bucketType == BucketType.NEEDS }
                val wantsEnvelopes = list.filter { it.bucketType == BucketType.WANTS }

                if (needsEnvelopes.isNotEmpty()) {
                    resultList.add(
                        com.rushi.coinmaster.data.local.model.GroupedCategory(
                            id = parentId,
                            name = parentCategory.name,
                            colorHex = parentCategory.colorHex,
                            iconName = parentCategory.iconName,
                            bucketType = BucketType.NEEDS,
                            allocatedAmountPaise = needsEnvelopes.sumOf { it.allocatedAmountPaise },
                            spentAmountPaise = needsEnvelopes.sumOf { it.spentAmountPaise },
                            envelopes = needsEnvelopes
                        )
                    )
                }
                if (wantsEnvelopes.isNotEmpty()) {
                    resultList.add(
                        com.rushi.coinmaster.data.local.model.GroupedCategory(
                            id = parentId,
                            name = parentCategory.name,
                            colorHex = parentCategory.colorHex,
                            iconName = parentCategory.iconName,
                            bucketType = BucketType.WANTS,
                            allocatedAmountPaise = wantsEnvelopes.sumOf { it.allocatedAmountPaise },
                            spentAmountPaise = wantsEnvelopes.sumOf { it.spentAmountPaise },
                            envelopes = wantsEnvelopes
                        )
                    )
                }
            }
        }

        // Envelopes without a parent category are not shown in Needs/Wants sections.
        // Users must assign an envelope to a named category via the category detail dialog.

        resultList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showCopyPreviousState: StateFlow<Boolean> = _selectedPeriodId.flatMapLatest { periodId ->
        if (periodId == null) {
            flowOf(false)
        } else {
            combine(
                budgetPeriodState,
                envelopesState,
                budgetRepository.getBudgetPeriodsFlow()
            ) { period, envelopes, periods ->
                if (period == null || period.isActive) {
                    false
                } else {
                    val sorted = periods.sortedBy { it.startDate }
                    val currentIndex = sorted.indexOfFirst { it.id == periodId }
                    if (currentIndex > 0) {
                        val prevPeriod = sorted[currentIndex - 1]
                        val hasCurrentAllocations = envelopes.any { it.allocatedAmountPaise > 0L }
                        val prevHasAllocations = budgetRepository.hasAllocations(prevPeriod.id)
                        !hasCurrentAllocations && prevHasAllocations
                    } else {
                        false
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Reactively track all active categories (pool)
    val allCategoriesState: StateFlow<List<CategoryEntity>> = budgetRepository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactively track all active parent categories
    val expenseCategoriesState: StateFlow<List<com.rushi.coinmaster.domain.model.ExpenseCategory>> = expenseCategoryRepository.getExpenseCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactively track live unallocated / validation state
    val unallocatedState: StateFlow<BudgetValidationResult> = combine(
        budgetPeriodState,
        envelopesState
    ) { period, envelopes ->
        if (period == null) {
            BudgetValidationResult(isValid = false, differencePaise = 0L)
        } else {
            val allocatedAmounts = envelopes.map { it.allocatedAmountPaise }
            validateZeroBalanceUseCase(period.incomePaise, allocatedAmounts)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetValidationResult(false, 0L))

    // UI Navigation/Message events
    private val _uiEvent = MutableSharedFlow<BudgetUiEvent>()
    val uiEvent: SharedFlow<BudgetUiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            budgetRepository.getBudgetPeriodsFlow().collectLatest { periods ->
                if (_selectedPeriodId.value == null && periods.isNotEmpty()) {
                    val today = System.currentTimeMillis()
                    val todayPeriod = periods.find { today >= it.startDate && today <= it.endDate }
                    _selectedPeriodId.value = todayPeriod?.id ?: periods.first().id
                }
            }
        }
    }

    fun selectPeriod(id: Int) {
        _selectedPeriodId.value = id
    }

    fun copyAllocationsFromPreviousPeriod() {
        val currentId = _selectedPeriodId.value ?: return
        viewModelScope.launch {
            val periods = budgetRepository.getBudgetPeriods().sortedBy { it.startDate }
            val currentIndex = periods.indexOfFirst { it.id == currentId }
            if (currentIndex > 0) {
                val prevPeriod = periods[currentIndex - 1]
                budgetRepository.copyAllocations(prevPeriod.id, currentId)
            }
        }
    }

    fun selectPreviousPeriod() {
        val currentId = _selectedPeriodId.value ?: return
        viewModelScope.launch {
            val periods = budgetRepository.getBudgetPeriods().sortedBy { it.startDate }
            val currentIndex = periods.indexOfFirst { it.id == currentId }
            if (currentIndex > 0) {
                _selectedPeriodId.value = periods[currentIndex - 1].id
            }
        }
    }

    fun selectNextPeriod() {
        val currentId = _selectedPeriodId.value ?: return
        viewModelScope.launch {
            val periods = budgetRepository.getBudgetPeriods().sortedBy { it.startDate }
            val currentIndex = periods.indexOfFirst { it.id == currentId }
            if (currentIndex != -1 && currentIndex < periods.lastIndex) {
                _selectedPeriodId.value = periods[currentIndex + 1].id
            }
        }
    }

    fun saveAllocation(categoryId: Long, amountStr: String) {
        val periodId = _selectedPeriodId.value ?: return
        viewModelScope.launch {
            try {
                val amountPaise = MoneyMath.rupeesToPaise(amountStr)
                budgetRepository.saveAllocation(periodId, categoryId, amountPaise)
            } catch (e: Exception) {
                _uiEvent.emit(BudgetUiEvent.Error(context.getString(R.string.error_invalid_allocation_format)))
            }
        }
    }

    fun activateBudgetPeriod() {
        val period = budgetPeriodState.value ?: return
        viewModelScope.launch {
            val validation = unallocatedState.value
            if (!validation.isValid) {
                val absoluteDiff = Math.abs(validation.differencePaise)
                val diffStr = "₹" + String.format("%.2f", absoluteDiff / 100.0)
                val msg = if (validation.differencePaise > 0L) {
                    context.getString(R.string.error_cannot_activate_unallocated, diffStr)
                } else {
                    context.getString(R.string.error_cannot_activate_over_allocated, diffStr)
                }
                _uiEvent.emit(BudgetUiEvent.Error(msg))
                return@launch
            }
            budgetRepository.updateBudgetPeriod(period.copy(isActive = true))
            _uiEvent.emit(BudgetUiEvent.SuccessActivation)
        }
    }

    fun setupBudgetPeriod(
        id: Int,
        startDate: Long,
        endDate: Long,
        needsPercent: Int,
        wantsPercent: Int,
        savingsPercent: Int
    ) {
        viewModelScope.launch {
            try {
                val activeStreams = incomeStreamRepository.getIncomeStreams()
                val incomePaise = activeStreams.filter { !it.isDeleted }.sumOf { it.amountPaise }
                
                // Overlap checks
                val overlapping = budgetRepository.getOverlappingPeriod(startDate, endDate, id)
                if (overlapping != null) {
                    _uiEvent.emit(BudgetUiEvent.Error("Budget period dates overlap with an existing period."))
                    return@launch
                }

                val existing = if (id != 0) budgetRepository.getBudgetPeriod(id) else null
                val budgetPeriod = existing?.copy(
                    startDate = startDate,
                    endDate = endDate,
                    incomePaise = incomePaise,
                    needsPercent = needsPercent,
                    wantsPercent = wantsPercent,
                    savingsPercent = savingsPercent
                ) ?: BudgetPeriodEntity(
                    startDate = startDate,
                    endDate = endDate,
                    incomePaise = incomePaise,
                    needsPercent = needsPercent,
                    wantsPercent = wantsPercent,
                    savingsPercent = savingsPercent
                )
                
                val savedId = if (id != 0) {
                    budgetRepository.updateBudgetPeriod(budgetPeriod)
                    id
                } else {
                    budgetRepository.insertBudgetPeriod(budgetPeriod).toInt()
                }

                // Recalculate transaction budget periods
                budgetRepository.recalculateTransactionBudgetPeriods()
                
                _selectedPeriodId.value = savedId
                _uiEvent.emit(BudgetUiEvent.SuccessSave)
            } catch (e: Exception) {
                _uiEvent.emit(BudgetUiEvent.Error(context.getString(R.string.error_setup_save_failed)))
            }
        }
    }

    // Category operations
    suspend fun getCategoryById(id: Long): CategoryEntity? {
        return budgetRepository.getCategoriesFlow().first().find { it.id == id }
    }

    fun saveCategory(
        id: Long,
        name: String,
        bucketType: BucketType?,
        colorHex: String,
        iconName: String,
        initialAllocationPaise: Long? = null,
        expenseCategoryId: Long? = null
    ) {
        viewModelScope.launch {
            val periodId = _selectedPeriodId.value ?: 0
            if (name.isBlank()) {
                _uiEvent.emit(BudgetUiEvent.Error(context.getString(R.string.error_envelope_name_empty)))
                return@launch
            }
            // Check duplicate envelope name (case-insensitive, non-deleted, excluding self)
            val exists = budgetRepository.getCategoriesFlow().first().any {
                it.name.equals(name.trim(), ignoreCase = true) && !it.isDeleted && it.id != id
            }
            if (exists) {
                _uiEvent.emit(BudgetUiEvent.Error("An envelope with this name already exists."))
                return@launch
            }

            val category = if (id == 0L) {
                CategoryEntity(
                    name = name.trim(),
                    bucketType = bucketType,
                    colorHex = colorHex,
                    iconName = iconName,
                    displayOrder = 0,
                    expenseCategoryId = expenseCategoryId
                )
            } else {
                val existing = budgetRepository.getCategoriesFlow().first().find { it.id == id }
                existing?.copy(
                    name = name.trim(),
                    bucketType = bucketType,
                    colorHex = colorHex,
                    iconName = iconName,
                    expenseCategoryId = expenseCategoryId
                ) ?: return@launch
            }
            if (id == 0L) {
                val newId = budgetRepository.insertCategory(category)
                if (periodId != 0 && initialAllocationPaise != null && initialAllocationPaise > 0L) {
                    budgetRepository.saveAllocation(periodId, newId, initialAllocationPaise)
                }
            } else {
                budgetRepository.updateCategory(category)
            }
            _uiEvent.emit(BudgetUiEvent.SuccessSave)
        }
    }

    fun assignCategoryToBucket(categoryId: Long, bucketType: BucketType) {
        viewModelScope.launch {
            val existing = budgetRepository.getCategoriesFlow().first().find { it.id == categoryId }
            if (existing != null) {
                budgetRepository.updateCategory(existing.copy(bucketType = bucketType))
                _uiEvent.emit(BudgetUiEvent.SuccessSave)
            }
        }
    }

    fun assignExpenseCategoryToBucket(expenseCategoryId: Long, bucketType: BucketType) {
        viewModelScope.launch {
            val existing = expenseCategoryRepository.getExpenseCategoryById(expenseCategoryId)
            if (existing != null) {
                expenseCategoryRepository.updateExpenseCategory(existing.copy(bucketType = bucketType))
                _uiEvent.emit(BudgetUiEvent.SuccessSave)
            }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            budgetRepository.softDeleteCategory(id)
            _uiEvent.emit(BudgetUiEvent.SuccessSave)
        }
    }

    fun updateExpenseTypeForCategories(categoryIds: List<Long>, expenseType: ExpenseType) {
        viewModelScope.launch {
            val allCategories = budgetRepository.getCategoriesFlow().first()
            val categoriesToUpdate = allCategories.filter { it.id in categoryIds }.map {
                it.copy(expenseType = expenseType)
            }
            if (categoriesToUpdate.isNotEmpty()) {
                budgetRepository.updateCategories(categoriesToUpdate)
                _uiEvent.emit(BudgetUiEvent.SuccessSave)
            }
        }
    }
}
