package ru.netology.nework.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.MenuProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapters.PagerAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.ActivityMainBinding
import ru.netology.nework.utils.MenuState
import ru.netology.nework.utils.MenuStates
import ru.netology.nework.viewmodels.AuthViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var navController: NavController

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        NavigationUI.setupActionBarWithNavController(this, navController)
        var currentMenuProvider: MenuProvider? = null
        authViewModel.authData.observe(this) {
            MenuState.setMenuState(MenuStates.SHOW_STATE)
            clearMenuProvider(currentMenuProvider)
            addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.main_menu, menu)
                    val authorized = authViewModel.authorized
                    if (MenuState.getMenuState() == MenuStates.SHOW_STATE) {
                        menu.setGroupVisible(R.id.authenticated, authorized)
                        menu.setGroupVisible(R.id.unauthenticated, !authorized)
                    } else {
                        menu.setGroupVisible(R.id.authenticated, false)
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
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
            }.apply {
                currentMenuProvider = this
            })
        }
    }

    private fun clearMenuProvider(currentMenuProvider: MenuProvider?) {
        currentMenuProvider?.let { removeMenuProvider(it) }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}