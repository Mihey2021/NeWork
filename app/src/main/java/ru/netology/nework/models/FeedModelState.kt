package ru.netology.nework.models

data class FeedModelState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val refreshing: Boolean = false,
    val errorMessage: String? = "",
    val needRefresh: Boolean = false,
)
