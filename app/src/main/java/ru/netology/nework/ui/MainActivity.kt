package ru.netology.nework.ui

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.filter.Filters
import ru.netology.nework.models.DeepLinks
import ru.netology.nework.utils.MenuState
import ru.netology.nework.utils.MenuStates
import ru.netology.nework.viewmodels.AuthViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var navController: NavController

    private var filterValue: Long = 0L

    @Inject
    lateinit var appAuth: AppAuth

    @Inject
    lateinit var filters: Filters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        NavigationUI.setupActionBarWithNavController(this, navController)

        var currentMenuProvider: MenuProvider? = null
        authViewModel.authData.observe(this) { authData ->
            MenuState.setMenuState(MenuStates.SHOW_STATE)
            clearMenuProvider(currentMenuProvider)
            addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.main_menu, menu)
                    val authorized = authViewModel.authorized
                    if (MenuState.getMenuState() == MenuStates.SHOW_STATE) {
                        menu.setGroupVisible(R.id.authenticated, authorized)
                        menu.setGroupVisible(
                            R.id.navigationMyWall,
                            (authorized && filterValue != authData?.id)
                        )
                        menu.setGroupVisible(R.id.navigationFeed, (filterValue != 0L))
                        menu.setGroupVisible(R.id.unauthenticated, !authorized)
                    } else {
                        menu.setGroupVisible(R.id.authenticated, false)
                        menu.setGroupVisible(R.id.navigationMyWall, false)
                        menu.setGroupVisible(R.id.navigationFeed, false)
                        menu.setGroupVisible(R.id.unauthenticated, false)
                    }

                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.signIn -> {
                            findNavController(R.id.nav_host_fragment).navigate(R.id.action_feedFragment_to_authFragment)
                            true
                        }
                        R.id.signUp -> {
                            findNavController(R.id.nav_host_fragment).navigate(R.id.action_feedFragment_to_registrationFragment)
                            true
                        }
                        R.id.logout -> {
                            if (MenuState.getMenuState() == MenuStates.SHOW_STATE) {
                                appAuth.removeAuth()
                                authViewModel.clearAuthUser()
                                true
                            } else {
                                false
                            }
                        }
                        R.id.myWall -> {
                            findNavController(R.id.nav_host_fragment).navigate(Uri.parse("${DeepLinks.USER_PAGE.link}${appAuth.getAuthorizedUserId()}"))
                            true
                        }

                        R.id.feed -> {
                            findNavController(R.id.nav_host_fragment).navigateUp()
                            true
                        }
                        else -> false
                    }
            }.apply {
                currentMenuProvider = this
            })
        }

        lifecycleScope.launch {
            filters.filterBy.collectLatest { userId ->
                filterValue = userId
                this@MainActivity.invalidateMenu()
            }
        }
    }

    private fun clearMenuProvider(currentMenuProvider: MenuProvider?) {
        currentMenuProvider?.let { removeMenuProvider(it) }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}