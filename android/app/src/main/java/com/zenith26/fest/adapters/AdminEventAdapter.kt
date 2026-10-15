package com.zenith26.fest.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zenith26.fest.R
import com.zenith26.fest.databinding.ItemAdminEventRowBinding
import com.zenith26.fest.models.Event
import com.zenith26.fest.utils.CategoryThemeHelper
import java.util.Locale

class AdminEventAdapter : ListAdapter<Event, AdminEventAdapter.AdminEventViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminEventViewHolder {
        val binding = ItemAdminEventRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdminEventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminEventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminEventViewHolder(
        private val binding: ItemAdminEventRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            val context = binding.root.context
            binding.tvAdminEventIcon.text = event.icon ?: "⚡"
            binding.tvAdminEventTitle.text = event.title
            binding.tvAdminEventVenue.text = "${event.venue} • ${event.eventDate}"
            binding.tvAdminEventRatio.text = "${event.registeredCount} / ${event.capacity}"

            CategoryThemeHelper.applyCategoryBadge(
                binding.tvAdminEventCategory,
                event.category
            )

            val pct = event.utilizationPct
            binding.tvAdminEventUtilBadge.text = String.format(Locale.US, "%.1f%%", pct)

            // Step 8.5: Highlight any event over 90% capacity in warning color
            if (pct >= 90f) {
                binding.rowRoot.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.status_red_bg)
                )
                val alertDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 999f
                    setColor(ContextCompat.getColor(context, R.color.badge_full_bg))
                    setStroke(1, ContextCompat.getColor(context, R.color.accent_rose))
                }
                binding.tvAdminEventUtilBadge.background = alertDrawable
                binding.tvAdminEventUtilBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.accent_rose)
                )
            } else {
                binding.rowRoot.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.white)
                )
                val normalDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 999f
                    setColor(ContextCompat.getColor(context, R.color.slate_100))
                }
                binding.tvAdminEventUtilBadge.background = normalDrawable
                binding.tvAdminEventUtilBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.slate_700)
                )
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean =
            oldItem == newItem
    }
}
