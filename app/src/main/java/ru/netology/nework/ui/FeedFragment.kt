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
    lateinit var adapter: PagerAdapter
    lateinit var binding: FragmentFeedBinding

    private var showingJobs: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentFeedBinding.inflate(layoutInflater)
        init()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        postViewModel.filterBy.observe(viewLifecycleOwner) {
            showingJobs = (it != 0L)
            with(binding) {
                val tabTwo = tabs.getTabAt(1)
                if (tabTwo != null) {
                    tabs.removeTab(tabTwo)

                    val newTab = tabs.newTab()
                        .also { newTab ->
                            newTab.text =
                                if (it == 0L) getString(R.string.events) else getString(R.string.jobs)
                        }
                    tabs.addTab(newTab)
                }
                adapter.showingJobs = showingJobs
                viewPager.adapter?.notifyItemChanged(1)
            }
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

    private fun init() {
        val viewPager = binding.viewPager
        adapter = PagerAdapter(this, showingJobs)
        viewPager.adapter = adapter
        viewPager.isSaveEnabled = false

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