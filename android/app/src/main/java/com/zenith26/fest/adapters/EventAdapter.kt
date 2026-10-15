package com.zenith26.fest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zenith26.fest.R
import com.zenith26.fest.databinding.ItemEventCardBinding
import com.zenith26.fest.models.Event
import com.zenith26.fest.utils.CategoryThemeHelper

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
        // Entrance fade-in animation
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_in)
        holder.itemView.startAnimation(animation)
    }

    inner class EventViewHolder(
        private val binding: ItemEventCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.tvEventIcon.text = event.icon ?: "⚡"
            binding.tvEventTitle.text = event.title
            binding.tvEventDescription.text = event.description ?: "Join us for this signature fest event!"
            binding.tvDateTime.text = "${event.eventDate} • ${event.eventTime}"
            binding.tvVenue.text = event.venue
            binding.tvEventFee.text = event.formattedFee

            // Dynamic Category Badge
            CategoryThemeHelper.applyCategoryBadge(binding.tvCategoryBadge, event.category)

            // FULL Badge & Seats count
            if (event.isFull) {
                binding.tvFullBadge.visibility = View.VISIBLE
                binding.tvSeatsCount.text = "FULL (${event.capacity}/${event.capacity})"
                binding.tvSeatsCount.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.badge_full_text)
                )
            } else {
                binding.tvFullBadge.visibility = View.GONE
                binding.tvSeatsCount.text = "${event.availableSeats} / ${event.capacity} left"
                binding.tvSeatsCount.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.accent_emerald)
                )
            }

            // View Details click
            binding.btnViewDetails.setOnClickListener {
                onEventClick(event)
            }
            binding.root.setOnClickListener {
                onEventClick(event)
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
