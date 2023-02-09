package ru.netology.nework.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.netology.nework.ui.EventsFragment
import ru.netology.nework.ui.FeedFragment
import ru.netology.nework.ui.JobsFragment
import ru.netology.nework.ui.PostsFragment

class FeedPagerAdapter(
    fragmentActivity: FeedFragment,
    var showingJobs: Boolean = false,
) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PostsFragment()
            else -> EventsFragment()
        }
    }
}