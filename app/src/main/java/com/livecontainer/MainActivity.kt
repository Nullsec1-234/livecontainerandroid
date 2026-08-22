package com.livecontainer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.*
import com.google.android.material.floatingactionbutton.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AppAdapter
    private var selectedNavItem: Int = R.id.navApps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up toolbar
        setSupportActionBar(toolbar)

        // Set up RecyclerView with grid layout (2 columns)
        recyclerViewApps.layoutManager = GridLayoutManager(this, 2)
        recyclerViewApps.setHasFixedSize(true)

        // Initialize adapter with sample guest data
        adapter = AppAdapter(this)
        recyclerViewApps.adapter = adapter

        // Load guest APKs and populate
        loadGuestApps()

        // Set up bottom navigation
        setupBottomNavigation()

        // Set up search FAB
        setupSearchFab()

        // Set up home indicator visibility
        homeIndicator.isVisible = true
    }

    private fun loadGuestApps() {
        // In a real implementation, this would scan the guests directory
        // and load APKs using ClassLoaderHelper and GuestManager
        // For now, add sample apps matching the UI reference

        val apps = listOf(
            AppItem(
                R.drawable.ic_launcher,
                getString(R.string.app_spotify),
                getString(R.string.app_version_spotify),
                getString(R.string.app_bundle_spotify),
                "com.spotify.client"
            ),
            AppItem(
                R.drawable.ic_launcher,
                getString(R.string.app_hd),
                getString(R.string.app_version_hd),
                getString(R.string.app_bundle_hd),
                "com.box.hd.flix.drama.hub"
            ),
            AppItem(
                R.drawable.ic_launcher,
                getString(R.string.app_kodi),
                getString(R.string.app_version_kodi),
                getString(R.string.app_bundle_kodi),
                "org.xbmc.kodi-ios"
            ),
            AppItem(
                R.drawable.ic_launcher,
                getString(R.string.app_arms),
                getString(R.string.app_version_arms),
                getString(R.string.app_bundle_arms),
                "com.armsx2.ios"
            ),
            AppItem(
                R.drawable.ic_launcher,
                getString(R.string.app_youtube),
                getString(R.string.app_version_youtube),
                getString(R.string.app_bundle_youtube),
                "com.google.ios.youtube"
            )
        )

        adapter.submitList(apps)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navQuellen -> {
                    selectedNavItem = R.id.navQuellen
                    true
                }
                R.id.navApps -> {
                    selectedNavItem = R.id.navApps
                    adapter.setActivePosition(0) // Spotify is active by default
                    true
                }
                R.id.navTweaks -> {
                    selectedNavItem = R.id.navTweaks
                    true
                }
                R.id.navEinstellungen -> {
                    selectedNavItem = R.id.navEinstellungen
                    true
                }
                else -> false
            }
        }

        // Set initial selected item
        bottomNavigation.setSelectedItemId(R.id.navApps)
    }

    private fun setupSearchFab() {
        searchButton.setOnClickListener {
            // Handle search action
        }
    }

    // App data model
    data class AppItem(
        val iconRes: Int,
        val name: String,
        val version: String,
        val bundle: String,
        val packageName: String
    )