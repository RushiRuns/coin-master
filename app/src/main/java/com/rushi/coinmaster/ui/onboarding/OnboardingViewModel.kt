package com.rushi.coinmaster.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.preferences.AppPreferences
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
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
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    // Step 1 State
    var userName: String = ""
    var preferredCurrency: String = "INR"

    // Step 2 State
    var accountName: String = ""
    var accountType: AccountType = AccountType.BANK_ACCOUNT
    var accountBalanceStr: String = ""

    // Step 3 State
    var monthlyIncomeStr: String = ""

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

    fun validateStep3(): Boolean {
        val income = monthlyIncomeStr.toDoubleOrNull()
        return income != null && income > 0.0
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
            accountRepository.insertAccount(firstAccount)

            // 3. Seed default envelopes
            budgetRepository.seedDefaultCategories()

            // 4. Create first BudgetMonth
            val incomePaise = MoneyMath.rupeesToPaise(monthlyIncomeStr.toDouble())
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1 // 1-indexed
            val budgetMonthId = year * 100 + month
            
            val firstBudget = BudgetMonthEntity(
                id = budgetMonthId,
                month = month,
                year = year,
                incomePaise = incomePaise,
                needsPercent = 50,
                wantsPercent = 30,
                savingsPercent = 20,
                isActive = false
            )
            budgetRepository.insertBudgetMonth(firstBudget)

            // 5. Complete state
            appPreferences.setOnboardingComplete(true)
            
            _onboardingSuccess.emit(Unit)
        }
    }
}
