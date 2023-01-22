package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapters.PagerAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.utils.MenuState
import ru.netology.nework.utils.MenuStates
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val postViewModel: PostViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    private var showingJobs: Boolean = false
    private var filterBy: Long = -1L

    lateinit var adapter: PagerAdapter

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //outState.putLong(FILTER_BY_USER_ID, filterBy)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        if (savedInstanceState != null)
//            filterBy = savedInstanceState.getLong(FILTER_BY_USER_ID)

        val binding = FragmentFeedBinding.inflate(layoutInflater)
        init(binding)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

//        if (savedInstanceState != null)
//            filterBy = savedInstanceState.getLong(FILTER_BY_USER_ID)

        postViewModel.filterBy.observe(viewLifecycleOwner) { it ->
            //if (filterBy == it) return@observe

            showingJobs = (it != 0L)
            with(binding) {
                val tabEvents = tabs.getTabAt(1)
                val tabJobs = tabs.getTabAt(2)
                when (it) {
                    0L -> {
                        if (tabJobs != null) {
                            tabs.removeTab(tabJobs)
                        }

                        if (tabEvents == null) {
                            val newTab = tabs.newTab()
                                .also { newTab -> newTab.text = getString(R.string.events) }
                            tabs.addTab(newTab)
                        }
                    }
                    else -> {
                        if (tabEvents != null) {
                            tabs.removeTab(tabEvents)
                        }

                        if (tabJobs == null) {
                            val newTab = tabs.newTab()
                                .also { newTab -> newTab.text = getString(R.string.jobs) }
                            tabs.addTab(newTab)
                        }
                    }
                }
            }

            filterBy = it
            init(binding)
        }

        authViewModel.authUser.observe(viewLifecycleOwner) {
            setActionBarSubTitle(it?.name)
        }

        authViewModel.authData.observe(viewLifecycleOwner) {
            if (it != null) {
                authViewModel.getUserById(it.id)
            } else {
                setActionBarSubTitle()
            }
        }

        return binding.root
    }

    private fun init(binding: FragmentFeedBinding) {
        val viewPager = binding.viewPager
        adapter = PagerAdapter(this, showingJobs)
        viewPager.adapter = adapter

        TabLayoutMediator(binding.tabs, viewPager) { tab, pos ->
            when (pos) {
                0 -> tab.text = getString(R.string.posts)
                1 -> tab.text =
                    if (showingJobs) getString(R.string.jobs) else getString(R.string.events)
            }
        }.attach()
    }

    private fun setActionBarSubTitle(subTitle: String? = null) {
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.subtitle = subTitle ?: ""
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

    companion object {
        const val FILTER_BY_USER_ID = "FILTER_BY_USER_ID"

    }
}