package com.zenith26.fest.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.zenith26.fest.R

object CategoryThemeHelper {

    data class CategoryColor(
        val bgColor: Int,
        val textColor: Int
    )

    fun getCategoryColors(context: Context, category: String?): CategoryColor {
        return when (category?.trim()?.lowercase()) {
            "technical" -> CategoryColor(
                ContextCompat.getColor(context, R.color.badge_technical_bg),
                ContextCompat.getColor(context, R.color.badge_technical_text)
            )
            "cultural" -> CategoryColor(
                ContextCompat.getColor(context, R.color.badge_cultural_bg),
                ContextCompat.getColor(context, R.color.badge_cultural_text)
            )
            "sports" -> CategoryColor(
                ContextCompat.getColor(context, R.color.badge_sports_bg),
                ContextCompat.getColor(context, R.color.badge_sports_text)
            )
            "gaming" -> CategoryColor(
                ContextCompat.getColor(context, R.color.badge_gaming_bg),
                ContextCompat.getColor(context, R.color.badge_gaming_text)
            )
            "workshop" -> CategoryColor(
                ContextCompat.getColor(context, R.color.badge_workshop_bg),
                ContextCompat.getColor(context, R.color.badge_workshop_text)
            )
            else -> CategoryColor(
                ContextCompat.getColor(context, R.color.brand_50),
                ContextCompat.getColor(context, R.color.brand_600)
            )
        }
    }

    /**
     * Applies dynamic pill badge styling matching original Tailwind CSS classes.
     */
    fun applyCategoryBadge(textView: TextView, category: String?) {
        val colors = getCategoryColors(textView.context, category)
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(colors.bgColor)
        }
        textView.background = drawable
        textView.setTextColor(colors.textColor)
        textView.text = category ?: "General"
    }
}
