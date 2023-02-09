package ru.netology.nework.filter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Filters @Inject constructor(){

    private val _filterBy: MutableStateFlow<Long> = MutableStateFlow(0L)
    val filterBy = _filterBy.asStateFlow()

    init {
        _filterBy.value = 0L
    }

    @Synchronized
    fun setFilterBy(userId: Long) {
        if (_filterBy.value == userId) return
        _filterBy.value = userId
    }

}