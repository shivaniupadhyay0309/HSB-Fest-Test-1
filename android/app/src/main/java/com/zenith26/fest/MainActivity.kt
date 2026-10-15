package com.zenith26.fest

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.zenith26.fest.api.ApiClient
import com.zenith26.fest.databinding.ActivityMainBinding
import com.zenith26.fest.screens.AdminDashboardFragment
import com.zenith26.fest.screens.SettingsDialogFragment
import com.zenith26.fest.screens.StudentPortalFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAdminScreenActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize centralized API client with stored preferences
        ApiClient.init(this)

        if (savedInstanceState == null) {
            showStudentPortal()
        }

        binding.btnNavToggle.setOnClickListener {
            if (isAdminScreenActive) {
                showStudentPortal()
            } else {
                showAdminDashboard()
            }
        }

        binding.tvNavbarLogo.setOnClickListener {
            showStudentPortal()
        }

        binding.btnServerConfig.setOnClickListener {
            val dialog = SettingsDialogFragment()
            dialog.setOnSavedListener {
                val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (current is StudentPortalFragment) {
                    current.loadEvents()
                } else if (current is AdminDashboardFragment) {
                    current.loadAllDashboardData()
                }
            }
            dialog.show(supportFragmentManager, "SettingsDialog")
        }

        // Back button navigation: return to student portal if in admin screen
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAdminScreenActive) {
                    showStudentPortal()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showStudentPortal() {
        isAdminScreenActive = false
        binding.btnNavToggle.text = getString(R.string.admin_dashboard)
        replaceFragment(StudentPortalFragment())
    }

    private fun showAdminDashboard() {
        isAdminScreenActive = true
        binding.btnNavToggle.text = getString(R.string.student_portal)
        replaceFragment(AdminDashboardFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
