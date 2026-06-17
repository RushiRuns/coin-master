package com.rushi.coinmaster.util

import android.content.Context
import com.rushi.coinmaster.R

object IconHelper {
    fun getIconDrawableResId(context: Context, iconName: String): Int {
        val resId = context.resources.getIdentifier(
            iconName,
            "drawable",
            context.packageName
        )
        if (resId != 0) return resId

        val legacyMapped = when (iconName) {
            "ic_rent" -> "ic_home"
            "ic_groceries" -> "ic_shopping_cart"
            "ic_utilities" -> "ic_bolt"
            "ic_dining" -> "ic_restaurant"
            "ic_entertainment" -> "ic_movie"
            "ic_shopping" -> "ic_shopping_cart"
            "ic_savings" -> "ic_savings"
            "ic_emergency" -> "ic_local_hospital"
            else -> null
        }
        if (legacyMapped != null) {
            val fallbackResId = context.resources.getIdentifier(
                legacyMapped,
                "drawable",
                context.packageName
            )
            if (fallbackResId != 0) return fallbackResId
        }

        return R.drawable.ic_category
    }
}
