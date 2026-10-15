package com.zenith26.fest.screens

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.zenith26.fest.api.ApiClient
import com.zenith26.fest.databinding.DialogSettingsBinding

class SettingsDialogFragment : DialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    private var onSavedListener: (() -> Unit)? = null

    fun setOnSavedListener(listener: () -> Unit) {
        this.onSavedListener = listener
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
        _binding = DialogSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etSettingsBaseUrl.setText(ApiClient.getBaseUrl())

        binding.btnSettingsCancel.setOnClickListener { dismiss() }

        binding.btnSettingsSave.setOnClickListener {
            val newUrl = binding.etSettingsBaseUrl.text.toString().trim()
            if (newUrl.isNotEmpty() && (newUrl.startsWith("http://") || newUrl.startsWith("https://"))) {
                ApiClient.setBaseUrl(requireContext(), newUrl)
                Toast.makeText(requireContext(), "Server URL updated to $newUrl", Toast.LENGTH_SHORT).show()
                onSavedListener?.invoke()
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid URL starting with http:// or https://", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
