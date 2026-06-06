package com.rushi.coinmaster.ui.accounts

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.databinding.ItemAccountBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper

class AccountsAdapter(
    private val onEditClick: (Long) -> Unit,
    private val onDeleteClick: (AccountEntity) -> Unit
) : ListAdapter<AccountEntity, AccountsAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)
        holder.bind(account)
    }

    inner class AccountViewHolder(val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(account: AccountEntity) {
            val context = binding.root.context
            val languageCode = LocaleHelper.getLanguage(context)

            binding.tvAccountName.text = account.name
            
            // Format Account Type Label
            binding.tvAccountType.text = when (account.type) {
                AccountType.CASH -> context.getString(R.string.acc_type_cash)
                AccountType.BANK_ACCOUNT -> context.getString(R.string.acc_type_bank)
                AccountType.CREDIT_CARD -> context.getString(R.string.acc_type_card)
                AccountType.INVESTMENTS -> context.getString(R.string.acc_type_invest)
            }

            // Format Balance Display
            if (account.type == AccountType.CREDIT_CARD) {
                binding.tvAccountBalance.setTextColor(ContextCompat.getColor(context, R.color.error))
                // Credit card represents debt, showing as negative amount in listing
                binding.tvAccountBalance.text = CurrencyFormatter.format(-account.balancePaise, languageCode)
            } else {
                binding.tvAccountBalance.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                binding.tvAccountBalance.text = CurrencyFormatter.format(account.balancePaise, languageCode)
            }

            // Set Brand Color Indicator
            try {
                binding.viewColorIndicator.setBackgroundColor(Color.parseColor(account.colorHex))
            } catch (e: Exception) {
                binding.viewColorIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.primary))
            }

            // Setup Click Listeners
            binding.root.setOnClickListener {
                onEditClick(account.id)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(account)
            }
        }
    }

    private class AccountDiffCallback : DiffUtil.ItemCallback<AccountEntity>() {
        override fun areItemsTheSame(oldItem: AccountEntity, newItem: AccountEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AccountEntity, newItem: AccountEntity): Boolean {
            return oldItem == newItem
        }
    }
}
