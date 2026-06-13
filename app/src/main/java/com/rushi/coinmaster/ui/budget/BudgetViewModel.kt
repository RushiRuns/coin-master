package com.rushi.coinmaster.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.data.repository.BudgetRepository
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
    private val validateZeroBalanceUseCase: ValidateZeroBalanceUseCase
) : ViewModel() {

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
        incomeStr: String,
        needsPercent: Int,
        wantsPercent: Int,
        savingsPercent: Int
    ) {
        viewModelScope.launch {
            try {
                val incomePaise = MoneyMath.rupeesToPaise(incomeStr)
                
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
        initialAllocationPaise: Long? = null
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
                    displayOrder = 0
                )
            } else {
                val existing = budgetRepository.getCategoriesFlow().first().find { it.id == id }
                existing?.copy(
                    name = name.trim(),
                    bucketType = bucketType,
                    colorHex = colorHex,
                    iconName = iconName
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

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            budgetRepository.softDeleteCategory(id)
            _uiEvent.emit(BudgetUiEvent.SuccessSave)
        }
    }
}
