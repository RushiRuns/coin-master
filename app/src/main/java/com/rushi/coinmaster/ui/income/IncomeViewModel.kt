package com.rushi.coinmaster.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.repository.IncomeStreamRepository
import com.rushi.coinmaster.domain.model.IncomeStream
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val incomeStreamRepository: IncomeStreamRepository
) : ViewModel() {

    /** Live list of active (non-deleted) income streams. */
    val incomeStreams: StateFlow<List<IncomeStream>> = incomeStreamRepository
        .getIncomeStreamsFlow()
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Total of all active income streams in paise. */
    val totalIncomePaise: StateFlow<Long> = incomeStreams
        .map { list -> list.sumOf { it.amountPaise } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    /**
     * Add a new income stream.
     * @param name   Display name for the source (e.g. "Salary").
     * @param amountRupees Amount in decimal rupees (will be converted to paise).
     */
    fun addIncomeStream(name: String, amountRupees: Double) {
        viewModelScope.launch {
            val amountPaise = MoneyMath.rupeesToPaise(amountRupees)
            incomeStreamRepository.insertIncomeStream(
                IncomeStream(
                    name = name,
                    amountPaise = amountPaise,
                    accountId = null
                )
            )
        }
    }

    /** Soft-delete an income stream by id. */
    fun deleteIncomeStream(id: Long) {
        viewModelScope.launch {
            incomeStreamRepository.softDeleteIncomeStream(id)
        }
    }
}
