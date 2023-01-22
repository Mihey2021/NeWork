package ru.netology.nework.models.event

import ru.netology.nework.models.Attachment
import ru.netology.nework.models.Coordinates

data class EventCreateRequest(
    val id: Long,
    val content: String,
    val datetime: String,
    val coords: Coordinates? = null,
    val type: EventType? = null,
    val attachment: Attachment? = null,
    val link: String? = null,
    val speakerIds: List<Long> = listOf(),
)
