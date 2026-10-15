package com.zenith26.fest.screens

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.zenith26.fest.R
import com.zenith26.fest.api.ApiClient
import com.zenith26.fest.databinding.DialogEventDetailBinding
import com.zenith26.fest.models.Event
import com.zenith26.fest.models.RegistrationIn
import com.zenith26.fest.utils.CategoryThemeHelper
import com.zenith26.fest.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EventDetailDialogFragment : DialogFragment() {

    private var _binding: DialogEventDetailBinding? = null
    private val binding get() = _binding!!

    private var eventId: Int = -1
    private var currentEvent: Event? = null
    private var onRegistrationSuccess: (() -> Unit)? = null

    companion object {
        private const val ARG_EVENT_ID = "arg_event_id"

        fun newInstance(eventId: Int): EventDetailDialogFragment {
            return EventDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_EVENT_ID, eventId)
                }
            }
        }
    }

    fun setOnRegistrationSuccessListener(listener: () -> Unit) {
        this.onRegistrationSuccess = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = arguments?.getInt(ARG_EVENT_ID, -1) ?: -1
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnModalClose.setOnClickListener {
            dismiss()
        }

        binding.btnSubmitRegistration.setOnClickListener {
            validateAndSubmitRegistration()
        }

        fetchEventDetail()
    }

    private fun fetchEventDetail() {
        if (eventId <= 0) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.api.getEventDetail(eventId)
                if (response.isSuccessful && response.body() != null) {
                    currentEvent = response.body()
                    populateEventData(currentEvent!!)
                } else {
                    val error = ApiClient.parseErrorMessage(response)
                    showFormAlert(error, isSuccess = false)
                }
            } catch (e: Exception) {
                val error = NetworkUtils.getReadableErrorMessage(e)
                showFormAlert(error, isSuccess = false)
            }
        }
    }

    private fun populateEventData(event: Event) {
        binding.tvModalIcon.text = event.icon ?: "⚡"
        binding.tvModalTitle.text = event.title
        binding.tvModalCategory.text = event.category
        binding.tvModalDateTime.text = "${event.eventDate} • ${event.eventTime}"
        binding.tvModalVenue.text = event.venue
        binding.tvModalFee.text = event.formattedFee
        binding.tvModalDescription.text = event.description ?: "No description provided."

        CategoryThemeHelper.applyCategoryBadge(binding.tvModalCategory, event.category)

        if (event.isFull) {
            binding.tvModalSeats.text = "FULL (${event.capacity}/${event.capacity})"
            binding.tvModalSeats.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.badge_full_text)
            )
            binding.tvEventFullBanner.visibility = View.VISIBLE
            binding.layoutFormFields.visibility = View.GONE
        } else {
            binding.tvModalSeats.text = "${event.availableSeats} / ${event.capacity}"
            binding.tvModalSeats.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.accent_emerald)
            )
            binding.tvEventFullBanner.visibility = View.GONE
            binding.layoutFormFields.visibility = View.VISIBLE
        }
    }

    private fun validateAndSubmitRegistration() {
        // Reset errors
        binding.errStudentName.visibility = View.GONE
        binding.errRollNumber.visibility = View.GONE
        binding.errEmail.visibility = View.GONE
        binding.layoutFormAlert.visibility = View.GONE

        val name = binding.etStudentName.text.toString().trim()
        val roll = binding.etRollNumber.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim().ifBlank { null }
        val email = binding.etEmail.text.toString().trim()

        var isValid = true

        if (name.isEmpty()) {
            binding.errStudentName.visibility = View.VISIBLE
            isValid = false
        }
        if (roll.isEmpty()) {
            binding.errRollNumber.visibility = View.VISIBLE
            isValid = false
        }
        if (email.isEmpty() || !email.contains("@")) {
            binding.errEmail.visibility = View.VISIBLE
            isValid = false
        }

        if (!isValid) return

        // Submit to API
        val registrationIn = RegistrationIn(
            studentName = name,
            rollNumber = roll,
            phone = phone,
            email = email
        )

        binding.btnSubmitRegistration.isEnabled = false
        binding.pbSubmitting.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.api.register(eventId, registrationIn)
                binding.pbSubmitting.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val eventTitle = currentEvent?.title ?: "Event"
                    showFormAlert(
                        "Registration confirmed for $eventTitle! Your seat is reserved.",
                        isSuccess = true
                    )
                    binding.layoutFormFields.visibility = View.GONE

                    // Inform listener to refresh seat counts
                    onRegistrationSuccess?.invoke()

                    // Automatically close dialog after brief delay
                    delay(1600)
                    dismiss()
                } else {
                    binding.btnSubmitRegistration.isEnabled = true
                    val serverError = ApiClient.parseErrorMessage(response)
                    showFormAlert(serverError, isSuccess = false)
                }
            } catch (e: Exception) {
                binding.btnSubmitRegistration.isEnabled = true
                binding.pbSubmitting.visibility = View.GONE
                val networkError = NetworkUtils.getReadableErrorMessage(e)
                showFormAlert(networkError, isSuccess = false)
            }
        }
    }

    private fun showFormAlert(message: String, isSuccess: Boolean) {
        binding.layoutFormAlert.visibility = View.VISIBLE
        binding.tvAlertMessage.text = message

        if (isSuccess) {
            binding.tvAlertIcon.text = "✅"
            binding.layoutFormAlert.setBackgroundResource(R.drawable.bg_stat_card)
            binding.layoutFormAlert.backgroundTintList = ContextCompat.getColorStateList(
                requireContext(),
                R.color.status_green_bg
            )
            binding.tvAlertMessage.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.status_green_text)
            )
        } else {
            binding.tvAlertIcon.text = "⚠️"
            binding.layoutFormAlert.setBackgroundResource(R.drawable.bg_alert_warning)
            binding.layoutFormAlert.backgroundTintList = null
            binding.tvAlertMessage.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.status_red_text)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
