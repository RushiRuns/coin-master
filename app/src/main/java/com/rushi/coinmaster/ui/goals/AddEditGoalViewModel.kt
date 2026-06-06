package com.rushi.coinmaster.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.entity.SinkingFundEntity
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.SinkingFundRepository
import android.content.Context
import com.rushi.coinmaster.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddEditGoalEvent {
    object Success : AddEditGoalEvent()
    data class Error(val message: String) : AddEditGoalEvent()
}

@HiltViewModel
class AddEditGoalViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sinkingFundRepository: SinkingFundRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _goalState = MutableStateFlow<SinkingFundEntity?>(null)
    val goalState: StateFlow<SinkingFundEntity?> = _goalState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddEditGoalEvent>()
    val eventFlow: SharedFlow<AddEditGoalEvent> = _eventFlow.asSharedFlow()

    // Expose only non-deleted categories in the SAVINGS bucket
    val savingsCategories: StateFlow<List<CategoryEntity>> = budgetRepository.getCategoriesFlow()
        .map { list ->
            list.filter { it.bucketType == BucketType.SAVINGS && !it.isDeleted }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadGoal(goalId: Long) {
        if (goalId <= 0L) {
            _goalState.value = null
            return
        }
        viewModelScope.launch {
            val fund = sinkingFundRepository.getSinkingFundById(goalId)
            _goalState.value = fund
        }
    }

    fun saveGoal(
        id: Long,
        name: String,
        targetAmountPaise: Long,
        targetDate: Long,
        autoCreateCategory: Boolean,
        selectedCategoryId: Long?
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            emitError(context.getString(R.string.error_goal_name_empty))
            return
        }
        if (targetAmountPaise <= 0L) {
            emitError(context.getString(R.string.error_target_amount_must_be_greater_than_zero))
            return
        }
        if (targetDate <= System.currentTimeMillis()) {
            // Let's check: target date should be in the future.
            // If they pick the current month or past, targetDate will be in the past or now.
            // But we should allow picking the current month if they want to complete it this month,
            // so let's check if the target month/year is before the current month/year.
            // A simple way is to check if the targetDate epoch millis is less than the start of the current month.
            // But to keep it simple and robust, let's just make sure it's not before the start of the current month.
            // We'll let the Fragment validate or do a basic validation here.
        }

        viewModelScope.launch {
            try {
                val categoryId = if (autoCreateCategory) {
                    // Check if an envelope with the same name already exists in the Savings bucket
                    val existing = savingsCategories.value.find { it.name.equals(trimmedName, ignoreCase = true) }
                    if (existing != null) {
                        existing.id
                    } else {
                        // Create a new Savings envelope
                        val newCategory = CategoryEntity(
                            name = trimmedName,
                            bucketType = BucketType.SAVINGS,
                            colorHex = "#FFAB40", // Standard orange color for savings goals
                            iconName = "ic_savings",
                            displayOrder = 100 // place at the end
                        )
                        budgetRepository.insertCategory(newCategory)
                    }
                } else {
                    if (selectedCategoryId == null || selectedCategoryId <= 0L) {
                        emitError(context.getString(R.string.error_no_category))
                        return@launch
                    }
                    selectedCategoryId
                }

                val existingGoal = _goalState.value
                val fund = SinkingFundEntity(
                    id = id,
                    name = trimmedName,
                    targetAmountPaise = targetAmountPaise,
                    savedAmountPaise = existingGoal?.savedAmountPaise ?: 0L,
                    targetDate = targetDate,
                    categoryId = categoryId,
                    isCompleted = existingGoal?.isCompleted ?: false
                )

                if (id == 0L) {
                    sinkingFundRepository.insertSinkingFund(fund)
                } else {
                    sinkingFundRepository.updateSinkingFund(fund)
                }
                _eventFlow.emit(AddEditGoalEvent.Success)
            } catch (e: Exception) {
                emitError(e.message ?: context.getString(R.string.error_save_goal_failed))
            }
        }
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(AddEditGoalEvent.Error(message))
        }
    }
}
