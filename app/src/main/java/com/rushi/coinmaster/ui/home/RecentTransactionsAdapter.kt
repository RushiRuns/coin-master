package com.rushi.coinmaster.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.databinding.ItemRecentTransactionBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.DateFormatter
import com.rushi.coinmaster.util.LocaleHelper

class RecentTransactionsAdapter :
    ListAdapter<TransactionDisplayItem, RecentTransactionsAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemRecentTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(private val binding: ItemRecentTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionDisplayItem) {
            val context = binding.root.context
            val languageCode = LocaleHelper.getLanguage(context)

            // Setup Date
            binding.tvTransactionDate.text = DateFormatter.formatDate(item.dateMillis, languageCode)

            // Setup Details based on Transaction Type
            when (item.type) {
                TransactionType.EXPENSE -> {
                    binding.tvTransactionTitle.text = item.categoryName ?: context.getString(R.string.title_budget)
                    
                    val desc = context.getString(R.string.text_expense_desc, item.accountName)
                    binding.tvTransactionSubtitle.text = if (item.note.isNullOrBlank()) desc else "$desc - ${item.note}"

                    // Set amount with negative sign and red color
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.error))
                    binding.tvTransactionAmount.text = "-${CurrencyFormatter.format(item.amountPaise, languageCode)}"

                    // Set Dot Color to Category Color
                    try {
                        binding.viewTypeDot.setBackgroundColor(Color.parseColor(item.categoryColorHex ?: "#DADCE0"))
                    } catch (e: Exception) {
                        binding.viewTypeDot.setBackgroundColor(ContextCompat.getColor(context, R.color.text_secondary))
                    }
                }
                TransactionType.INCOME -> {
                    binding.tvTransactionTitle.text = context.getString(R.string.ob_income_title)

                    val desc = context.getString(R.string.text_income_desc, item.accountName)
                    binding.tvTransactionSubtitle.text = if (item.note.isNullOrBlank()) desc else "$desc - ${item.note}"

                    // Set amount with positive sign and green color
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    binding.tvTransactionAmount.text = "+${CurrencyFormatter.format(item.amountPaise, languageCode)}"

                    // Set Dot Color to Green
                    binding.viewTypeDot.setBackgroundColor(ContextCompat.getColor(context, R.color.primary))
                }
                TransactionType.TRANSFER -> {
                    binding.tvTransactionTitle.text = context.getString(R.string.acc_type_cash) // Fallback title
                    binding.tvTransactionTitle.setText(R.string.text_expense_desc) // Wait, let's look at the label
                    binding.tvTransactionTitle.text = "Transfer"

                    val desc = context.getString(
                        R.string.text_transfer_desc,
                        item.accountName,
                        item.transferToAccountName ?: "Unknown"
                    )
                    binding.tvTransactionSubtitle.text = if (item.note.isNullOrBlank()) desc else "$desc - ${item.note}"

                    // Set amount with gray color
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    binding.tvTransactionAmount.text = CurrencyFormatter.format(item.amountPaise, languageCode)

                    // Set Dot Color to Gray
                    binding.viewTypeDot.setBackgroundColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
                TransactionType.BALANCE_CORRECTION -> {
                    binding.tvTransactionTitle.text = "Correction"
                    binding.tvTransactionSubtitle.text = "Balance adjustment for ${item.accountName}" + 
                        (if (item.note.isNullOrBlank()) "" else " - ${item.note}")

                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    binding.tvTransactionAmount.text = CurrencyFormatter.format(item.amountPaise, languageCode)

                    binding.viewTypeDot.setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
                }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionDisplayItem>() {
        override fun areItemsTheSame(oldItem: TransactionDisplayItem, newItem: TransactionDisplayItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionDisplayItem, newItem: TransactionDisplayItem): Boolean {
            return oldItem == newItem
        }
    }
}
