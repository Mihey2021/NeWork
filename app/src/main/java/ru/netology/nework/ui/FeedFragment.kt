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
import ru.netology.nework.adapters.FeedPagerAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.filter.Filters
import ru.netology.nework.utils.MenuState
import ru.netology.nework.utils.MenuStates
import ru.netology.nework.viewmodels.CommonViewModel
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val commonViewModel: CommonViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    lateinit var adapter: FeedPagerAdapter
    lateinit var binding: FragmentFeedBinding

    private var showingJobs: Boolean = false

    @Inject
    lateinit var filters: Filters

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

        filters.setFilterBy(0L)

        val authId = appAuth.getAuthorizedUserId()
        commonViewModel.getUserById(authId)
        commonViewModel.userDetail.observe(viewLifecycleOwner){
            setActionBarSubTitle(if(authId == 0L) "" else it.name)
        }

        return binding.root
    }

    private fun init() {
        val viewPager = binding.viewPager
        adapter = FeedPagerAdapter(this, showingJobs)
        viewPager.adapter = adapter
        viewPager.isSaveEnabled = false

        TabLayoutMediator(binding.tabs, viewPager) { tab, pos ->
            when (pos) {
                0 -> tab.text = getString(R.string.posts)
                1 -> tab.text = getString(R.string.events)
            }
        }.attach()
    }

    private fun setActionBarSubTitle(title: String? = null) {
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.subtitle = title
    }

    override fun onResume() {
        super.onResume()
        MenuState.setMenuState(MenuStates.SHOW_STATE)
        requireActivity().invalidateMenu()
    }

    override fun onPause() {
        super.onPause()
        setActionBarSubTitle(null)
        MenuState.setMenuState(MenuStates.HIDE_STATE)
        requireActivity().invalidateMenu()
    }
}