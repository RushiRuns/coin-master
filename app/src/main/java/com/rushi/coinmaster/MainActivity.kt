package com.rushi.coinmaster

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.rushi.coinmaster.data.preferences.AppPreferences
import com.rushi.coinmaster.databinding.ActivityMainBinding
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appPreferences.isOnboardingComplete.collect { complete ->
                    val graph = navController.navInflater.inflate(R.navigation.nav_graph)
                    if (complete) {
                        graph.setStartDestination(R.id.accountsFragment)
                    } else {
                        graph.setStartDestination(R.id.onboardingFragment)
                    }
                    navController.graph = graph
                }
            }
        }
    }
}