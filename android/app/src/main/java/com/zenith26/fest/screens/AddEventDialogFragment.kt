package com.zenith26.fest.screens

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.zenith26.fest.api.ApiClient
import com.zenith26.fest.databinding.DialogAddEventBinding
import com.zenith26.fest.models.EventCreate
import com.zenith26.fest.utils.NetworkUtils
import kotlinx.coroutines.launch

class AddEventDialogFragment : DialogFragment() {

    private var _binding: DialogAddEventBinding? = null
    private val binding get() = _binding!!

    private var onEventCreated: (() -> Unit)? = null

    fun setOnEventCreatedListener(listener: () -> Unit) {
        this.onEventCreated = listener
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
        _binding = DialogAddEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categories = listOf("Technical", "Cultural", "Sports", "Gaming", "Workshop")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spNewCategory.adapter = spinnerAdapter

        binding.btnAddEventClose.setOnClickListener { dismiss() }
        binding.btnCancelAddEvent.setOnClickListener { dismiss() }

        binding.btnSubmitAddEvent.setOnClickListener {
            submitNewEvent()
        }
    }

    private fun submitNewEvent() {
        val title = binding.etNewTitle.text.toString().trim()
        val category = binding.spNewCategory.selectedItem.toString()
        val icon = binding.etNewIcon.text.toString().trim().ifBlank { "⚡" }
        val date = binding.etNewDate.text.toString().trim()
        val time = binding.etNewTime.text.toString().trim()
        val venue = binding.etNewVenue.text.toString().trim()
        val capacityStr = binding.etNewCapacity.text.toString().trim()
        val feeStr = binding.etNewFee.text.toString().trim()
        val description = binding.etNewDescription.text.toString().trim()

        if (title.isEmpty() || date.isEmpty() || time.isEmpty() || venue.isEmpty()) {
            showError("Please fill in all required fields.")
            return
        }

        val capacity = capacityStr.toIntOrNull() ?: 0
        if (capacity <= 0) {
            showError("Capacity must be greater than 0.")
            return
        }

        val fee = feeStr.toIntOrNull() ?: 0

        val newEvent = EventCreate(
            title = title,
            category = category,
            icon = icon,
            eventDate = date,
            eventTime = time,
            venue = venue,
            capacity = capacity,
            fee = fee,
            description = description.ifBlank { null }
        )

        binding.btnSubmitAddEvent.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.api.createEvent(newEvent)
                binding.btnSubmitAddEvent.isEnabled = true

                if (response.isSuccessful) {
                    onEventCreated?.invoke()
                    dismiss()
                } else {
                    val error = ApiClient.parseErrorMessage(response)
                    showError(error)
                }
            } catch (e: Exception) {
                binding.btnSubmitAddEvent.isEnabled = true
                showError(NetworkUtils.getReadableErrorMessage(e))
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvAddEventError.text = msg
        binding.tvAddEventError.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
