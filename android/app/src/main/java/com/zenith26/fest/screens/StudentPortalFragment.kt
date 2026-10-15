package com.zenith26.fest.screens

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.zenith26.fest.R
import com.zenith26.fest.adapters.EventAdapter
import com.zenith26.fest.api.ApiClient
import com.zenith26.fest.databinding.FragmentStudentPortalBinding
import com.zenith26.fest.models.Event
import com.zenith26.fest.utils.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StudentPortalFragment : Fragment() {

    private var _binding: FragmentStudentPortalBinding? = null
    private val binding get() = _binding!!

    private lateinit var eventAdapter: EventAdapter
    private var selectedCategory: String = "All"
    private var searchQuery: String = ""
    private var searchDebounceJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentPortalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCategoryFilters()
        setupSearch()
        setupListeners()

        loadEvents()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            showEventDetailDialog(event)
        }

        // Responsive grid: 1 column on phone portrait, 2 on wide/tablet
        val isTabletOrLandscape = resources.configuration.screenWidthDp >= 600
        val spanCount = if (isTabletOrLandscape) 2 else 1
        binding.rvEvents.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.rvEvents.adapter = eventAdapter
    }

    private fun setupCategoryFilters() {
        val categoryViews = listOf(
            binding.btnCatAll to "All",
            binding.btnCatTechnical to "Technical",
            binding.btnCatCultural to "Cultural",
            binding.btnCatSports to "Sports",
            binding.btnCatGaming to "Gaming",
            binding.btnCatWorkshop to "Workshop"
        )

        for ((btn, cat) in categoryViews) {
            btn.setOnClickListener {
                if (selectedCategory != cat) {
                    selectedCategory = cat
                    updateCategoryButtonsUI(categoryViews)
                    loadEvents()
                }
            }
        }
    }

    private fun updateCategoryButtonsUI(categoryViews: List<Pair<TextView, String>>) {
        for ((btn, cat) in categoryViews) {
            if (cat.equals(selectedCategory, ignoreCase = true)) {
                btn.setBackgroundResource(R.drawable.bg_chip_selected)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                btn.setBackgroundResource(R.drawable.bg_chip_unselected)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.slate_700))
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                // Debounce search input to avoid flood while typing
                searchDebounceJob?.cancel()
                searchDebounceJob = lifecycleScope.launch {
                    delay(300)
                    searchQuery = query
                    loadEvents()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadEvents()
        }

        binding.btnRetry.setOnClickListener {
            loadEvents()
        }

        binding.btnResetFilters.setOnClickListener {
            selectedCategory = "All"
            binding.etSearch.text?.clear()
            val categoryViews = listOf(
                binding.btnCatAll to "All",
                binding.btnCatTechnical to "Technical",
                binding.btnCatCultural to "Cultural",
                binding.btnCatSports to "Sports",
                binding.btnCatGaming to "Gaming",
                binding.btnCatWorkshop to "Workshop"
            )
            updateCategoryButtonsUI(categoryViews)
            loadEvents()
        }
    }

    fun loadEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.layoutError.visibility = View.GONE
            binding.layoutEmpty.visibility = View.GONE

            if (!binding.swipeRefresh.isRefreshing) {
                binding.layoutLoading.visibility = View.VISIBLE
            }

            try {
                val catParam = if (selectedCategory.equals("All", ignoreCase = true)) null else selectedCategory
                val searchParam = if (searchQuery.isBlank()) null else searchQuery

                val response = ApiClient.api.getEvents(
                    category = catParam,
                    search = searchParam
                )

                binding.layoutLoading.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val events = response.body() ?: emptyList()
                    eventAdapter.submitList(events)

                    binding.tvResultsCount.text = "Showing ${events.size} event${if (events.size == 1) "" else "s"}"

                    if (events.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.rvEvents.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvEvents.visibility = View.VISIBLE
                    }
                } else {
                    val errorMsg = ApiClient.parseErrorMessage(response)
                    showError(errorMsg)
                }
            } catch (e: Exception) {
                binding.layoutLoading.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = NetworkUtils.getReadableErrorMessage(e)
                showError(errorMsg)
            }
        }
    }

    private fun showError(message: String) {
        binding.tvErrorMessage.text = message
        binding.layoutError.visibility = View.VISIBLE
        binding.rvEvents.visibility = View.GONE
        binding.tvResultsCount.text = "Error loading events"
    }

    private fun showEventDetailDialog(event: Event) {
        val dialog = EventDetailDialogFragment.newInstance(event.id)
        dialog.setOnRegistrationSuccessListener {
            // Immediately reload events to update available seat count
            loadEvents()
        }
        dialog.show(childFragmentManager, "EventDetailDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
