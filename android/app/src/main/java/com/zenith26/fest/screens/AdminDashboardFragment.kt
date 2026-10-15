package com.zenith26.fest.screens

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zenith26.fest.R
import com.zenith26.fest.adapters.AdminEventAdapter
import com.zenith26.fest.adapters.CategoryBreakdownAdapter
import com.zenith26.fest.adapters.RegistrationAdapter
import com.zenith26.fest.api.ApiClient
import com.zenith26.fest.databinding.FragmentAdminDashboardBinding
import com.zenith26.fest.models.Event
import com.zenith26.fest.models.RegistrationItem
import com.zenith26.fest.utils.NetworkUtils
import kotlinx.coroutines.launch
import java.util.Locale

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var adminEventAdapter: AdminEventAdapter
    private lateinit var registrationAdapter: RegistrationAdapter
    private lateinit var categoryBreakdownAdapter: CategoryBreakdownAdapter

    private var allEvents: List<Event> = emptyList()
    private var isViewingRegistrationsTab = false
    private var selectedFilterEventId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupTabs()
        setupActions()

        loadAllDashboardData()
    }

    private fun setupRecyclerViews() {
        // 1. Category breakdown
        categoryBreakdownAdapter = CategoryBreakdownAdapter()
        binding.rvCategoryBreakdown.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvCategoryBreakdown.adapter = categoryBreakdownAdapter

        // 2. Admin events table
        adminEventAdapter = AdminEventAdapter()
        binding.rvAdminEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminEvents.adapter = adminEventAdapter

        // 3. Registrations ledger
        registrationAdapter = RegistrationAdapter { item ->
            confirmDeleteRegistration(item)
        }
        binding.rvRegistrations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRegistrations.adapter = registrationAdapter
    }

    private fun setupTabs() {
        binding.tabEventsRoster.setOnClickListener {
            switchTab(isRegistrations = false)
        }
        binding.tabRegistrationsLedger.setOnClickListener {
            switchTab(isRegistrations = true)
        }
    }

    private fun switchTab(isRegistrations: Boolean) {
        isViewingRegistrationsTab = isRegistrations
        if (isRegistrations) {
            binding.tabRegistrationsLedger.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.tabRegistrationsLedger.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            binding.tabEventsRoster.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.tabEventsRoster.setTextColor(ContextCompat.getColor(requireContext(), R.color.slate_600))

            binding.sectionEventsRoster.visibility = View.GONE
            binding.sectionRegistrationsLedger.visibility = View.VISIBLE

            loadRegistrations()
        } else {
            binding.tabEventsRoster.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.tabEventsRoster.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            binding.tabRegistrationsLedger.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.tabRegistrationsLedger.setTextColor(ContextCompat.getColor(requireContext(), R.color.slate_600))

            binding.sectionEventsRoster.visibility = View.VISIBLE
            binding.sectionRegistrationsLedger.visibility = View.GONE
        }
    }

    private fun setupActions() {
        binding.adminSwipeRefresh.setOnRefreshListener {
            loadAllDashboardData()
        }

        binding.btnAdminRefresh.setOnClickListener {
            loadAllDashboardData()
        }

        binding.btnAdminRetryBanner.setOnClickListener {
            loadAllDashboardData()
        }

        binding.btnAdminAddEvent.setOnClickListener {
            val dialog = AddEventDialogFragment()
            dialog.setOnEventCreatedListener {
                Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show()
                loadAllDashboardData()
            }
            dialog.show(childFragmentManager, "AddEventDialog")
        }
    }

    fun loadAllDashboardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.adminErrorBanner.visibility = View.GONE
            if (!binding.adminSwipeRefresh.isRefreshing) {
                binding.pbAdminLoading.visibility = View.VISIBLE
            }

            try {
                // 1. Fetch live telemetry statistics
                val statsResponse = ApiClient.api.getStats()
                if (statsResponse.isSuccessful && statsResponse.body() != null) {
                    val stats = statsResponse.body()!!
                    binding.statTotalEvents.text = stats.totalEvents.toString()
                    binding.statTotalRegistrations.text = stats.totalRegistrations.toString()
                    binding.statUtilizationPct.text = String.format(Locale.US, "%.1f%%", stats.overallUtilizationPct)
                    binding.statCapacitySubtext.text = "${stats.totalRegistrations} / ${stats.totalCapacity} total seats"
                    binding.statUpcomingEvents.text = stats.upcomingEventsCount.toString()

                    categoryBreakdownAdapter.submitData(stats.eventsByCategory)
                }

                // 2. Fetch events table (sorted by utilization descending)
                val eventsResponse = ApiClient.api.getEvents()
                if (eventsResponse.isSuccessful && eventsResponse.body() != null) {
                    allEvents = eventsResponse.body()!!

                    // Sort descending by utilization %
                    val sortedEvents = allEvents.sortedByDescending { it.utilizationPct }
                    adminEventAdapter.submitList(sortedEvents)

                    setupEventFilterSpinner()
                }

                // 3. If registrations tab is active, fetch registrations
                if (isViewingRegistrationsTab) {
                    loadRegistrations()
                }

                binding.pbAdminLoading.visibility = View.GONE
                binding.adminSwipeRefresh.isRefreshing = false
            } catch (e: Exception) {
                binding.pbAdminLoading.visibility = View.GONE
                binding.adminSwipeRefresh.isRefreshing = false
                val errorMsg = NetworkUtils.getReadableErrorMessage(e)
                binding.tvAdminErrorMessage.text = errorMsg
                binding.adminErrorBanner.visibility = View.VISIBLE
            }
        }
    }

    private fun setupEventFilterSpinner() {
        val eventOptions = mutableListOf("All Events")
        eventOptions.addAll(allEvents.map { it.title })

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, eventOptions)
        binding.spFilterEvent.adapter = adapter

        binding.spFilterEvent.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFilterEventId = if (position == 0) {
                    null
                } else {
                    allEvents.getOrNull(position - 1)?.id
                }
                if (isViewingRegistrationsTab) {
                    loadRegistrations()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadRegistrations() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.api.getRegistrations(selectedFilterEventId)
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!
                    registrationAdapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.tvEmptyRegistrations.visibility = View.VISIBLE
                        binding.rvRegistrations.visibility = View.GONE
                    } else {
                        binding.tvEmptyRegistrations.visibility = View.GONE
                        binding.rvRegistrations.visibility = View.VISIBLE
                    }
                } else {
                    val err = ApiClient.parseErrorMessage(response)
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val err = NetworkUtils.getReadableErrorMessage(e)
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteRegistration(item: RegistrationItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Registration?")
            .setMessage("Are you sure you want to cancel the registration for ${item.studentName} in '${item.eventTitle}'? This will immediately free up the seat.")
            .setPositiveButton("Confirm Cancel") { _, _ ->
                executeDeleteRegistration(item.id)
            }
            .setNegativeButton("Keep Registration", null)
            .show()
    }

    private fun executeDeleteRegistration(registrationId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.api.deleteRegistration(registrationId)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Registration cancelled. Seat released.", Toast.LENGTH_SHORT).show()
                    loadAllDashboardData()
                } else {
                    val err = ApiClient.parseErrorMessage(response)
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val err = NetworkUtils.getReadableErrorMessage(e)
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
