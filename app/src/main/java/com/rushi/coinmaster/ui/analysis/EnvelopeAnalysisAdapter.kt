package com.rushi.coinmaster.ui.analysis

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.databinding.ItemCategoryAnalysisBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.IconHelper
import com.rushi.coinmaster.util.LocaleHelper

class EnvelopeAnalysisAdapter(
    private val onViewTransactionsClick: (EnvelopeWithAllocation) -> Unit
) : ListAdapter<EnvelopeWithAllocation, EnvelopeAnalysisAdapter.EnvelopeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnvelopeViewHolder {
        val binding = ItemCategoryAnalysisBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EnvelopeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EnvelopeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EnvelopeViewHolder(
        private val binding: ItemCategoryAnalysisBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(envelope: EnvelopeWithAllocation) {
            val context = binding.root.context
            val languageCode = LocaleHelper.getLanguage(context)

            binding.tvCategoryName.text = envelope.categoryName

            // Set Color Indicator
            try {
                binding.viewCategoryColor.setBackgroundColor(Color.parseColor(envelope.colorHex))
            } catch (e: Exception) {
                binding.viewCategoryColor.setBackgroundColor(Color.GRAY)
            }

            // Set Icon
            binding.ivCategoryIcon.setImageResource(
                IconHelper.getIconDrawableResId(context, envelope.iconName)
            )

            // Dynamic color tint for icon background
            try {
                binding.ivCategoryIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(envelope.colorHex))
            } catch (e: Exception) {
                binding.ivCategoryIcon.imageTintList = ColorStateList.valueOf(Color.GRAY)
            }

            val spent = envelope.spentAmountPaise
            val allocated = envelope.allocatedAmountPaise

            binding.tvAmountSummary.text = "${CurrencyFormatter.format(spent, languageCode)} / ${CurrencyFormatter.format(allocated, languageCode)}"

            if (allocated > 0L) {
                val ratio = spent.toFloat() / allocated.toFloat()
                val percent = (ratio * 100).toInt()
                binding.tvPercentage.text = "$percent%"
                binding.progressBar.progress = percent.coerceAtMost(100)

                val progressColor = when {
                    ratio < 0.75f -> Color.parseColor("#4CAF50") // Green
                    ratio < 1.0f -> Color.parseColor("#FF9800") // Orange
                    else -> Color.parseColor("#F44336") // Red
                }

                binding.progressBar.progressTintList = ColorStateList.valueOf(progressColor)
                binding.tvPercentage.setTextColor(progressColor)
            } else {
                binding.tvPercentage.text = "0%"
                binding.progressBar.progress = 0
                binding.progressBar.progressTintList = ColorStateList.valueOf(Color.GRAY)
                binding.tvPercentage.setTextColor(Color.GRAY)
            }

            binding.tvViewTransactions.setOnClickListener {
                onViewTransactionsClick(envelope)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<EnvelopeWithAllocation>() {
            override fun areItemsTheSame(oldItem: EnvelopeWithAllocation, newItem: EnvelopeWithAllocation): Boolean {
                return oldItem.categoryId == newItem.categoryId
            }

            override fun areContentsTheSame(oldItem: EnvelopeWithAllocation, newItem: EnvelopeWithAllocation): Boolean {
                return oldItem == newItem
            }
        }
    }
}
