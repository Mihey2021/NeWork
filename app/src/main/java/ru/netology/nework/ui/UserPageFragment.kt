package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import ru.netology.nework.R
import ru.netology.nework.adapters.FeedPagerAdapter
import ru.netology.nework.adapters.UserPagePagerAdapter
import ru.netology.nework.databinding.FragmentUserPageBinding

class UserPageFragment: Fragment(R.layout.fragment_user_page) {

    lateinit var binding: FragmentUserPageBinding
    lateinit var adapter: UserPagePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentUserPageBinding.inflate(layoutInflater)
        init()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val userId = arguments?.getLong("userId")
        return binding.root
    }

    private fun init() {
        val viewPager = binding.viewPager
        adapter = UserPagePagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isSaveEnabled = false

        TabLayoutMediator(binding.tabs, viewPager) { tab, pos ->
            when (pos) {
                0 -> tab.text = getString(R.string.posts)
                1 -> tab.text = getString(R.string.events)
                2 -> tab.text = getString(R.string.jobs)
                //if (showingJobs) getString(R.string.jobs) else getString(R.string.events)
            }
        }.attach()
    }

}