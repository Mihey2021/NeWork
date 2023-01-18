package ru.netology.nework.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapters.PagerAdapter
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.utils.MenuState
import ru.netology.nework.utils.MenuStates

@AndroidEntryPoint
class FeedFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)
        initial(binding)
        return binding.root
    }

    private fun initial(binding: FragmentFeedBinding) {
        val viewPager = binding.viewPager
        viewPager.adapter = PagerAdapter(this)
        TabLayoutMediator(binding.tabs, viewPager) { tab, pos ->
            when (pos) {
                0 -> {
                    tab.text = getString(R.string.posts)
                }
                1 -> {
                    tab.text = getString(R.string.events)
                }
            }
        }.attach()

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
//                when (tab.position) {
//                    0 -> {
//                        binding.fabPostAdd.visibility = View.VISIBLE
//                    }
//                    1 -> {
//                        binding.fabEventAdd.visibility = View.VISIBLE
//                    }
//                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
//                when (tab.position) {
//                    0 -> {
//                        binding.fabEventAdd.visibility = View.GONE
//                    }
//                    1 -> {
//                        binding.fabPostAdd.visibility = View.GONE
//                    }
//                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        MenuState.setMenuState(MenuStates.SHOW_STATE)
        requireActivity().invalidateMenu()
    }

    override fun onPause() {
        super.onPause()
        MenuState.setMenuState(MenuStates.HIDE_STATE)
        requireActivity().invalidateMenu()
    }
}