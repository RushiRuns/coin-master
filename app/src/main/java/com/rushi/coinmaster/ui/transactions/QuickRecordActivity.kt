package com.rushi.coinmaster.ui.transactions

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rushi.coinmaster.MainActivity
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.preferences.AppPreferences
import com.rushi.coinmaster.databinding.ActivityQuickRecordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuickRecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickRecordBinding
    private val viewModel: TransactionViewModel by viewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    private var accountsList: List<AccountEntity> = emptyList()
    private var categoriesList: List<CategoryEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityQuickRecordBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            val complete = appPreferences.isOnboardingComplete.first()
            if (!complete) {
                // Redirect to onboarding/MainActivity if setup is incomplete
                val intent = Intent(this@QuickRecordActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
                return@launch
            }

            setContentView(binding.root)
            setupUI()
            observeViewModel()
        }
    }

    override fun onStart() {
        super.onStart()
        window?.let { win ->
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.9).toInt()
            win.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun setupUI() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.accountsState.collect { accounts ->
                        accountsList = accounts
                        val accountNames = accounts.map { it.name }
                        val sourceAdapter = ArrayAdapter(
                            this@QuickRecordActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            accountNames
                        )
                        binding.actvAccount.setAdapter(sourceAdapter)

                        if (accountNames.isNotEmpty() && binding.actvAccount.text.isEmpty()) {
                            binding.actvAccount.setText(accountNames[0], false)
                        }
                    }
                }

                launch {
                    viewModel.categoriesState.collect { categories ->
                        categoriesList = categories
                        val categoryNames = categories.map { it.name }
                        val categoryAdapter = ArrayAdapter(
                            this@QuickRecordActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            categoryNames
                        )
                        binding.actvCategory.setAdapter(categoryAdapter)

                        if (categoryNames.isNotEmpty() && binding.actvCategory.text.isEmpty()) {
                            binding.actvCategory.setText(categoryNames[0], false)
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is TransactionUiEvent.Success -> {
                                Toast.makeText(
                                    this@QuickRecordActivity,
                                    getString(R.string.text_transaction_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                            is TransactionUiEvent.Error -> {
                                Toast.makeText(
                                    this@QuickRecordActivity,
                                    event.message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveTransaction() {
        val amountStr = binding.etAmount.text.toString()

        val selectedSourceName = binding.actvAccount.text.toString()
        val sourceAccountId = accountsList.find { it.name == selectedSourceName }?.id ?: 0L

        val selectedCategoryName = binding.actvCategory.text.toString()
        val categoryId = categoriesList.find { it.name == selectedCategoryName }?.id

        val note = binding.etNote.text.toString()

        viewModel.saveTransaction(
            amountStr = amountStr,
            type = TransactionType.EXPENSE,
            accountId = sourceAccountId,
            transferToAccountId = null,
            categoryId = categoryId,
            date = System.currentTimeMillis(),
            note = note
        )
    }
}
