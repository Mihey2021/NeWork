package ru.netology.nework.models.jobs

data class Job(
    val id: Long,
    val name: String,
    val position: String,
    val start: String,
    val finish: String? = null,
    val link: String? = null,
) : java.io.Serializable
