package com.zenith26.fest.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zenith26.fest.databinding.ItemCategoryStatBinding

class CategoryBreakdownAdapter(
    private var categoryList: List<Pair<String, Int>> = emptyList()
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    fun submitData(data: Map<String, Int>) {
        categoryList = data.toList().sortedByDescending { it.second }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryStatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (category, count) = categoryList[position]
        holder.bind(category, count)
    }

    override fun getItemCount(): Int = categoryList.size

    inner class ViewHolder(
        private val binding: ItemCategoryStatBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: String, count: Int) {
            binding.tvCatStatName.text = category
            binding.tvCatStatCount.text = count.toString()
        }
    }
}
