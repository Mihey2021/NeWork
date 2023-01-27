package ru.netology.nework.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nework.models.Token
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Filters @Inject constructor(){

    //private val _filterBy: MutableStateFlow<Long> = MutableStateFlow(0L)
    //val filterBy = _filterBy.asStateFlow()
    private val _filterBy = MutableLiveData(0L)
    val filterBy: LiveData<Long>
        get() = _filterBy

    init {
        _filterBy.value = 0L
    }

    fun setFilterBy(userId: Long) {
        if (_filterBy.value == userId) return
        _filterBy.value = userId
    }

}