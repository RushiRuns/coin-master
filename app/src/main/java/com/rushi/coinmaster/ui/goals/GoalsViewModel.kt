package com.rushi.coinmaster.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.repository.SinkingFundRepository
import com.rushi.coinmaster.domain.usecase.ComputeSinkingFundMonthlyContributionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalUiModel(
    val id: Long,
    val name: String,
    val targetAmountPaise: Long,
    val savedAmountPaise: Long,
    val targetDate: Long,
    val categoryId: Long,
    val categoryName: String,
    val isCompleted: Boolean,
    val monthlySavingsNeededPaise: Long,
    val percent: Int
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val sinkingFundRepository: SinkingFundRepository,
    private val computeContributionUseCase: ComputeSinkingFundMonthlyContributionUseCase
) : ViewModel() {

    val goalsList: StateFlow<List<GoalUiModel>> = sinkingFundRepository.getSinkingFundsWithProgressFlow()
        .map { list ->
            list.map { item ->
                val contribution = computeContributionUseCase(
                    targetAmountPaise = item.sinkingFund.targetAmountPaise,
                    savedAmountPaise = item.computedSavedAmountPaise,
                    targetDate = item.sinkingFund.targetDate
                )
                val pct = if (item.sinkingFund.targetAmountPaise > 0L) {
                    ((item.computedSavedAmountPaise * 100) / item.sinkingFund.targetAmountPaise).toInt()
                } else 0
                GoalUiModel(
                    id = item.sinkingFund.id,
                    name = item.sinkingFund.name,
                    targetAmountPaise = item.sinkingFund.targetAmountPaise,
                    savedAmountPaise = item.computedSavedAmountPaise,
                    targetDate = item.sinkingFund.targetDate,
                    categoryId = item.sinkingFund.categoryId,
                    categoryName = item.categoryName,
                    isCompleted = item.sinkingFund.isCompleted || item.computedSavedAmountPaise >= item.sinkingFund.targetAmountPaise,
                    monthlySavingsNeededPaise = contribution,
                    percent = Math.min(100, Math.max(0, pct))
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            val fund = sinkingFundRepository.getSinkingFundById(goalId)
            fund?.let {
                sinkingFundRepository.deleteSinkingFund(it)
            }
        }
    }
}
