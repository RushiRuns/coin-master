package com.rushi.coinmaster.ui.home

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
import com.rushi.coinmaster.databinding.ItemAccountHorizontalBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper

class AccountsHorizontalAdapter(
    private val onItemClick: (Long) -> Unit
) : ListAdapter<AccountEntity, AccountsHorizontalAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountHorizontalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AccountViewHolder(private val binding: ItemAccountHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(account: AccountEntity) {
            val context = binding.root.context
            val languageCode = LocaleHelper.getLanguage(context)

            binding.tvAccountName.text = account.name

            binding.tvAccountType.text = when (account.type) {
                AccountType.CASH -> context.getString(R.string.acc_type_cash)
                AccountType.BANK_ACCOUNT -> context.getString(R.string.acc_type_bank)
                AccountType.CREDIT_CARD -> context.getString(R.string.acc_type_card)
                AccountType.INVESTMENTS -> context.getString(R.string.acc_type_invest)
            }

            if (account.type == AccountType.CREDIT_CARD) {
                binding.tvAccountBalance.setTextColor(ContextCompat.getColor(context, R.color.error))
                binding.tvAccountBalance.text = CurrencyFormatter.format(-account.balancePaise, languageCode)
            } else {
                binding.tvAccountBalance.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                binding.tvAccountBalance.text = CurrencyFormatter.format(account.balancePaise, languageCode)
            }

            try {
                binding.viewColorIndicator.setBackgroundColor(Color.parseColor(account.colorHex))
            } catch (e: Exception) {
                binding.viewColorIndicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
            }

            binding.root.setOnClickListener {
                onItemClick(account.id)
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
