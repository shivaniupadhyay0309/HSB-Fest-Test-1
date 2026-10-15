package com.zenith26.fest.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zenith26.fest.databinding.ItemRegistrationRowBinding
import com.zenith26.fest.models.RegistrationItem

class RegistrationAdapter(
    private val onCancelClick: (RegistrationItem) -> Unit
) : ListAdapter<RegistrationItem, RegistrationAdapter.RegistrationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegistrationViewHolder {
        val binding = ItemRegistrationRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RegistrationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegistrationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RegistrationViewHolder(
        private val binding: ItemRegistrationRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RegistrationItem) {
            binding.tvRegStudentName.text = item.studentName
            val phonePart = if (!item.phone.isNullOrBlank()) " • ${item.phone}" else ""
            binding.tvRegRollEmail.text = "${item.rollNumber} • ${item.email}$phonePart"
            binding.tvRegEventTitle.text = "🎯 ${item.eventTitle}"
            binding.tvRegTimestamp.text = item.registeredAt

            binding.btnCancelRegistration.setOnClickListener {
                onCancelClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RegistrationItem>() {
        override fun areItemsTheSame(oldItem: RegistrationItem, newItem: RegistrationItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RegistrationItem, newItem: RegistrationItem): Boolean =
            oldItem == newItem
    }
}
