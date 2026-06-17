package com.rushi.coinmaster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rushi.coinmaster.data.preferences.AppPreferences
import com.rushi.coinmaster.databinding.ActivityMainBinding
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val drawerLayout: androidx.drawerlayout.widget.DrawerLayout get() = binding.drawerLayout

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dynamically set start destination based on onboarding completion status
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Bind BottomNavigationView and both NavigationViews (sidebar)
        binding.bottomNavigation.setupWithNavController(navController)
        binding.navView.setupWithNavController(navController)
        binding.navViewSettings.setupWithNavController(navController)

        // Set drawer width to 60% of screen width dynamically
        val displayMetrics = resources.displayMetrics
        val drawerWidth = (displayMetrics.widthPixels * 0.6).toInt()
        val layoutParams = binding.navViewContainer.layoutParams
        layoutParams.width = drawerWidth
        binding.navViewContainer.layoutParams = layoutParams

        // Handle navigation selection state sync, auto-closing the drawer, and drawer locking/bottom nav visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Keep selection state in sync between both nav views and auto-close drawer
            if (destination.id == R.id.nav_settings) {
                binding.navView.checkedItem?.isChecked = false
            } else {
                binding.navViewSettings.checkedItem?.isChecked = false
            }
            binding.drawerLayout.closeDrawers()

            val sidebarTopLevelDestinations = setOf(
                R.id.homeFragment, R.id.transactionsFragment, R.id.budgetFragment,
                R.id.nav_categories, R.id.nav_expense_type, R.id.nav_income,
                R.id.nav_savings, R.id.nav_accounts, R.id.nav_notes,
                R.id.nav_transfers, R.id.nav_goals, R.id.nav_settings
            )

            if (destination.id == R.id.homeFragment
                || destination.id == R.id.transactionsFragment
                || destination.id == R.id.budgetFragment
            ) {
                binding.bottomNavigation.visibility = View.VISIBLE
                binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                binding.bottomNavigation.visibility = View.GONE
                // Lock drawer on detail/onboarding fragments, unlock on sidebar screens
                if (destination.id in sidebarTopLevelDestinations) {
                    binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
                } else if (destination.id == R.id.onboardingFragment
                    || destination.id == R.id.addEditAccountFragment
                    || destination.id == R.id.addEditEnvelopeFragment
                    || destination.id == R.id.addEditGoalFragment
                    || destination.id == R.id.addTransactionFragment
                    || destination.id == R.id.addEditDebtFragment
                    || destination.id == R.id.debtDetailsFragment
                    || destination.id == R.id.monthSetupFragment
                ) {
                    binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                } else {
                    binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appPreferences.isOnboardingComplete.collect { complete ->
                    val graph = navController.navInflater.inflate(R.navigation.nav_graph)
                    if (complete) {
                        graph.setStartDestination(R.id.homeFragment)
                    } else {
                        graph.setStartDestination(R.id.onboardingFragment)
                    }
                    navController.graph = graph

                }
            }
        }

        // Apply theme dynamically
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                appPreferences.appTheme.collect { theme ->
                    val mode = when (theme) {
                        "light" -> AppCompatDelegate.MODE_NIGHT_NO
                        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    if (AppCompatDelegate.getDefaultNightMode() != mode) {
                        AppCompatDelegate.setDefaultNightMode(mode)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}