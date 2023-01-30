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
import ru.netology.nework.adapters.UserPagePagerAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentUserPageBinding
import ru.netology.nework.filter.Filters
import ru.netology.nework.viewmodels.CommonViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserPageFragment : Fragment(R.layout.fragment_user_page) {

    private val commonViewModel: CommonViewModel by activityViewModels()

    lateinit var binding: FragmentUserPageBinding
    lateinit var adapter: UserPagePagerAdapter

    @Inject
    lateinit var filters: Filters

    @Inject
    lateinit var appAuth: AppAuth

    private var userId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentUserPageBinding.inflate(layoutInflater)
        init()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        userId = arguments?.getLong("userId")
        filters.setFilterBy(userId ?: 0L)
        commonViewModel.getUserById(userId ?: 0L)

        commonViewModel.userDetail.observe(viewLifecycleOwner) {
            setActionBarTitle(it.name)
        }

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
                1 -> tab.text = getString(R.string.jobs)
            }
        }.attach()
    }

    private fun setActionBarTitle(userName: String? = "") {
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.title =
            if (userId == appAuth.getAuthorizedUserId()) getString(R.string.my_wall) else userName
    }

}