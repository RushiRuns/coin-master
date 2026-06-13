package com.rushi.coinmaster.ui.debts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.local.model.DebtType
import com.rushi.coinmaster.databinding.ItemDebtBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.DateFormatter
import com.rushi.coinmaster.util.LocaleHelper

class DebtsAdapter(
    private val onItemClick: (Long) -> Unit
) : ListAdapter<DebtEntity, DebtsAdapter.DebtViewHolder>(DebtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val binding = ItemDebtBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DebtViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DebtViewHolder(private val binding: ItemDebtBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DebtEntity) {
            val context = binding.root.context
            val languageCode = LocaleHelper.getLanguage(context)

            binding.tvPersonName.text = item.personName
            binding.tvRemainingAmount.text = CurrencyFormatter.format(item.remainingPaise, languageCode)
            binding.tvTotalAmount.text = "Total: ${CurrencyFormatter.format(item.amountPaise, languageCode)}"

            if (item.type == DebtType.LENT) {
                binding.tvDebtType.text = "LENT"
                binding.tvDebtType.setTextColor(ContextCompat.getColor(context, R.color.color_positive))
            } else {
                binding.tvDebtType.text = "BORROWED"
                binding.tvDebtType.setTextColor(ContextCompat.getColor(context, R.color.color_negative))
            }

            if (item.dueDate != null) {
                binding.tvDueDate.visibility = View.VISIBLE
                binding.tvDueDate.text = "Due: ${DateFormatter.formatDate(item.dueDate, languageCode)}"
            } else {
                binding.tvDueDate.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(item.id)
            }
        }
    }

    private class DebtDiffCallback : DiffUtil.ItemCallback<DebtEntity>() {
        override fun areItemsTheSame(oldItem: DebtEntity, newItem: DebtEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DebtEntity, newItem: DebtEntity): Boolean {
            return oldItem == newItem
        }
    }
}
