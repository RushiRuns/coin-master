package com.rushi.coinmaster.ui.analysis

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rushi.coinmaster.data.local.model.GroupedCategory
import com.rushi.coinmaster.databinding.ItemCategoryAnalysisBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.IconHelper
import com.rushi.coinmaster.util.LocaleHelper

class CategoryAnalysisAdapter(
    private val onCategoryClick: (GroupedCategory) -> Unit,
    private val onViewTransactionsClick: (GroupedCategory) -> Unit
) : ListAdapter<GroupedCategory, CategoryAnalysisAdapter.CategoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryAnalysisBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryAnalysisBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: GroupedCategory) {
            val context = binding.root.context
            val languageCode = LocaleHelper.getLanguage(context)

            binding.tvCategoryName.text = category.name

            // Set Color Indicator
            try {
                binding.viewCategoryColor.setBackgroundColor(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                binding.viewCategoryColor.setBackgroundColor(Color.GRAY)
            }

            // Set Icon
            binding.ivCategoryIcon.setImageResource(
                IconHelper.getIconDrawableResId(context, category.iconName)
            )

            // Dynamic color tint for icon background
            try {
                binding.ivCategoryIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                binding.ivCategoryIcon.imageTintList = ColorStateList.valueOf(Color.GRAY)
            }

            val spent = category.spentAmountPaise
            val allocated = category.allocatedAmountPaise

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

                binding.progressBar.setIndicatorColor(progressColor)
                binding.tvPercentage.setTextColor(progressColor)
            } else {
                binding.tvPercentage.text = "0%"
                binding.progressBar.progress = 0
                binding.progressBar.setIndicatorColor(Color.GRAY)
                binding.tvPercentage.setTextColor(Color.GRAY)
            }

            binding.root.setOnClickListener {
                onCategoryClick(category)
            }

            binding.tvViewTransactions.setOnClickListener {
                onViewTransactionsClick(category)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<GroupedCategory>() {
            override fun areItemsTheSame(oldItem: GroupedCategory, newItem: GroupedCategory): Boolean {
                return oldItem.id == newItem.id && oldItem.bucketType == newItem.bucketType
            }

            override fun areContentsTheSame(oldItem: GroupedCategory, newItem: GroupedCategory): Boolean {
                return oldItem == newItem
            }
        }
    }
}
