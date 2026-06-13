package com.rushi.coinmaster.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.local.model.DebtType
import com.rushi.coinmaster.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DebtsUiState(
    val activeDebts: List<DebtEntity> = emptyList(),
    val settledDebts: List<DebtEntity> = emptyList(),
    val totalLentPaise: Long = 0L,
    val totalBorrowedPaise: Long = 0L
)

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val debtRepository: DebtRepository
) : ViewModel() {

    val uiState: StateFlow<DebtsUiState> = debtRepository.getDebtsFlow()
        .map { debts ->
            val active = debts.filter { !it.isSettled }
            val settled = debts.filter { it.isSettled }
            
            val totalLent = active.filter { it.type == DebtType.LENT }.sumOf { it.remainingPaise }
            val totalBorrowed = active.filter { it.type == DebtType.BORROWED }.sumOf { it.remainingPaise }
            
            DebtsUiState(
                activeDebts = active,
                settledDebts = settled,
                totalLentPaise = totalLent,
                totalBorrowedPaise = totalBorrowed
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DebtsUiState()
        )
}
