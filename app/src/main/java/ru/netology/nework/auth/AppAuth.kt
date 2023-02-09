package ru.netology.nework.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nework.models.Token
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext
    private val context: Context,
) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authStateFlow: MutableStateFlow<Token?> = MutableStateFlow(null)
    val authStateFlow = _authStateFlow.asStateFlow()

    init {
        val token = prefs.getString(TOKEN_KEY, null)
        val id = prefs.getLong(ID_KEY, 0)

        if (token == null || id == 0L) {
            removeAuth()
        } else {
            _authStateFlow.value = Token(id, token)
        }
    }

    @Synchronized
    fun setAuth(id: Long, token: String) {
        _authStateFlow.value = Token(id, token)
        prefs.edit {
            putString(TOKEN_KEY, token)
            putLong(ID_KEY, id)
        }
    }

    fun getAuthorizedUserId(): Long =
        authStateFlow.value?.id ?: 0L


    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = null
        prefs.edit {
            remove(TOKEN_KEY)
            remove(ID_KEY)
        }
    }

    companion object {
        private const val ID_KEY = "ID_KEY"
        private const val TOKEN_KEY = "TOKEN_KEY"
    }
}