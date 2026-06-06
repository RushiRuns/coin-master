package com.rushi.coinmaster.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
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

    private val calendar = Calendar.getInstance()
    private val defaultMonthId = calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1)

    private val _selectedMonthId = MutableStateFlow(defaultMonthId)
    val selectedMonthId: StateFlow<Int> = _selectedMonthId.asStateFlow()

    // Reactively track the current budget month entity
    val budgetMonthState: StateFlow<BudgetMonthEntity?> = _selectedMonthId.flatMapLatest { monthId ->
        budgetRepository.getBudgetMonthsFlow().map { months ->
            months.find { it.id == monthId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Reactively track categories combined with allocations/spent for the month
    val envelopesState: StateFlow<List<EnvelopeWithAllocation>> = _selectedMonthId.flatMapLatest { monthId ->
        budgetRepository.getEnvelopesWithAllocationsFlow(monthId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactively track live unallocated / validation state
    val unallocatedState: StateFlow<BudgetValidationResult> = combine(
        budgetMonthState,
        envelopesState
    ) { month, envelopes ->
        if (month == null) {
            BudgetValidationResult(isValid = false, differencePaise = 0L)
        } else {
            val allocatedAmounts = envelopes.map { it.allocatedAmountPaise }
            validateZeroBalanceUseCase(month.incomePaise, allocatedAmounts)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetValidationResult(false, 0L))

    // UI Navigation/Message events
    private val _uiEvent = MutableSharedFlow<BudgetUiEvent>()
    val uiEvent: SharedFlow<BudgetUiEvent> = _uiEvent.asSharedFlow()

    fun selectMonth(id: Int) {
        _selectedMonthId.value = id
    }

    fun selectPreviousMonth() {
        val current = _selectedMonthId.value
        val year = current / 100
        val month = current % 100
        val prevYear = if (month == 1) year - 1 else year
        val prevMonth = if (month == 1) 12 else month - 1
        _selectedMonthId.value = prevYear * 100 + prevMonth
    }

    fun selectNextMonth() {
        val current = _selectedMonthId.value
        val year = current / 100
        val month = current % 100
        val nextYear = if (month == 12) year + 1 else year
        val nextMonth = if (month == 12) 1 else month + 1
        _selectedMonthId.value = nextYear * 100 + nextMonth
    }

    fun saveAllocation(categoryId: Long, amountStr: String) {
        viewModelScope.launch {
            try {
                val amountPaise = MoneyMath.rupeesToPaise(amountStr)
                budgetRepository.saveAllocation(_selectedMonthId.value, categoryId, amountPaise)
            } catch (e: Exception) {
                _uiEvent.emit(BudgetUiEvent.Error(context.getString(R.string.error_invalid_allocation_format)))
            }
        }
    }

    fun activateBudgetMonth() {
        val month = budgetMonthState.value ?: return
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
            budgetRepository.updateBudgetMonth(month.copy(isActive = true))
            _uiEvent.emit(BudgetUiEvent.SuccessActivation)
        }
    }

    fun setupBudgetMonth(
        id: Int,
        month: Int,
        year: Int,
        incomeStr: String,
        needsPercent: Int,
        wantsPercent: Int,
        savingsPercent: Int
    ) {
        viewModelScope.launch {
            try {
                val incomePaise = MoneyMath.rupeesToPaise(incomeStr)
                val existing = budgetRepository.getBudgetMonth(id)
                val budgetMonth = existing?.copy(
                    incomePaise = incomePaise,
                    needsPercent = needsPercent,
                    wantsPercent = wantsPercent,
                    savingsPercent = savingsPercent
                ) ?: BudgetMonthEntity(
                    id = id,
                    month = month,
                    year = year,
                    incomePaise = incomePaise,
                    needsPercent = needsPercent,
                    wantsPercent = wantsPercent,
                    savingsPercent = savingsPercent
                )
                budgetRepository.insertBudgetMonth(budgetMonth)
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

    fun saveCategory(id: Long, name: String, bucketType: BucketType, colorHex: String, iconName: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiEvent.emit(BudgetUiEvent.Error(context.getString(R.string.error_envelope_name_empty)))
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
                budgetRepository.insertCategory(category)
            } else {
                budgetRepository.updateCategory(category)
            }
            _uiEvent.emit(BudgetUiEvent.SuccessSave)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            budgetRepository.softDeleteCategory(id)
            _uiEvent.emit(BudgetUiEvent.SuccessSave)
        }
    }
}
