package com.rushi.coinmaster.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.preferences.AppPreferences
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.IncomeStreamRepository
import com.rushi.coinmaster.domain.model.IncomeStream
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val incomeStreamRepository: IncomeStreamRepository
) : ViewModel() {

    // Step 1 State
    var userName: String = ""
    var preferredCurrency: String = "INR"

    // Step 2 State
    var accountName: String = ""
    var accountType: AccountType = AccountType.BANK_ACCOUNT
    var accountBalanceStr: String = ""

    // Step 3 State: List of income streams
    val incomeStreams = mutableListOf<IncomeStream>()

    private val _onboardingSuccess = MutableSharedFlow<Unit>()
    val onboardingSuccess: SharedFlow<Unit> = _onboardingSuccess

    fun validateStep1(): Boolean {
        return userName.trim().isNotEmpty()
    }

    fun validateStep2(): Boolean {
        if (accountName.trim().isEmpty()) return false
        val balance = accountBalanceStr.toDoubleOrNull()
        return balance != null && balance >= 0.0
    }

    fun addIncomeStream(name: String, amountRupees: Double) {
        val amountPaise = MoneyMath.rupeesToPaise(amountRupees)
        incomeStreams.add(
            IncomeStream(
                name = name,
                amountPaise = amountPaise,
                accountId = null
            )
        )
    }

    fun removeIncomeStream(index: Int) {
        if (index in incomeStreams.indices) {
            incomeStreams.removeAt(index)
        }
    }

    fun getTotalIncomeRupees(): Double {
        return MoneyMath.paiseToRupeesDouble(incomeStreams.sumOf { it.amountPaise })
    }

    fun validateStep3(): Boolean {
        return incomeStreams.isNotEmpty() && incomeStreams.sumOf { it.amountPaise } > 0L
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            if (!validateStep1() || !validateStep2() || !validateStep3()) return@launch

            // 1. Save preferences
            appPreferences.setUserName(userName.trim())
            appPreferences.setPreferredCurrency(preferredCurrency)
            
            // 2. Insert first account
            val balancePaise = MoneyMath.rupeesToPaise(accountBalanceStr.toDouble())
            val firstAccount = AccountEntity(
                name = accountName.trim(),
                type = accountType,
                balancePaise = balancePaise,
                colorHex = "#4285F4", // Brand Slate Blue default color
                iconName = "ic_account"
            )
            val firstAccountId = accountRepository.insertAccount(firstAccount)

            // 3. Seed default envelopes
            budgetRepository.seedDefaultCategories()

            // 4. Save income streams linked to the first account
            val totalIncomePaise = incomeStreams.sumOf { it.amountPaise }
            incomeStreams.forEach { stream ->
                incomeStreamRepository.insertIncomeStream(
                    stream.copy(accountId = firstAccountId)
                )
            }

            // 5. Create first BudgetPeriod starting today and ending 1 month later
            val calendar = Calendar.getInstance()
            
            // Start Date: today at 00:00:00.000
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startDate = calendar.timeInMillis

            // End Date: 1 month later minus 1 day at 23:59:59.999
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endDate = calendar.timeInMillis
            
            val firstBudget = BudgetPeriodEntity(
                startDate = startDate,
                endDate = endDate,
                incomePaise = totalIncomePaise,
                needsPercent = 50,
                wantsPercent = 30,
                savingsPercent = 20,
                isActive = false
            )
            budgetRepository.insertBudgetPeriod(firstBudget)

            // 6. Complete state
            appPreferences.setOnboardingComplete(true)
            
            _onboardingSuccess.emit(Unit)
        }
    }
}
