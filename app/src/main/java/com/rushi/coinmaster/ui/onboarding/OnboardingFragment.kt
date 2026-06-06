package com.rushi.coinmaster.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewPager
        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false // Disable swipe gestures to enforce linear validation

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateUiForPage(position)
            }
        })

        // Setup button clicks
        binding.btnPrevious.setOnClickListener {
            if (binding.viewPager.currentItem > 0) {
                binding.viewPager.currentItem--
            }
        }

        binding.btnNext.setOnClickListener {
            handleNextClick()
        }

        // Listen for completion event
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onboardingSuccess.collect {
                    // Navigate to the home screen
                    findNavController().navigate(R.id.action_onboardingFragment_to_homeFragment)
                }
            }
        }

        // Initial UI state
        updateUiForPage(0)
    }

    private fun handleNextClick() {
        val currentPosition = binding.viewPager.currentItem
        val currentFragment = childFragmentManager.findFragmentByTag("f$currentPosition")

        when (currentPosition) {
            0 -> {
                val step1 = currentFragment as? NameCurrencyFragment
                if (step1?.validateAndShowError() == true) {
                    binding.viewPager.currentItem++
                }
            }
            1 -> {
                val step2 = currentFragment as? AddFirstAccountFragment
                if (step2?.validateAndShowError() == true) {
                    binding.viewPager.currentItem++
                }
            }
            2 -> {
                val step3 = currentFragment as? SetIncomeFragment
                if (step3?.validateAndShowError() == true) {
                    viewModel.completeOnboarding()
                }
            }
        }
    }

    private fun updateUiForPage(position: Int) {
        binding.tvPageIndicator.text = getString(R.string.app_name) + " - ${position + 1} of 3"
        
        if (position == 0) {
            binding.btnPrevious.visibility = View.INVISIBLE
        } else {
            binding.btnPrevious.visibility = View.VISIBLE
        }

        if (position == 2) {
            binding.btnNext.text = getString(R.string.btn_complete)
        } else {
            binding.btnNext.text = getString(R.string.btn_next)
        }
    }

    private inner class OnboardingPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> NameCurrencyFragment()
                1 -> AddFirstAccountFragment()
                2 -> SetIncomeFragment()
                else -> throw IllegalStateException("Invalid position: $position")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
