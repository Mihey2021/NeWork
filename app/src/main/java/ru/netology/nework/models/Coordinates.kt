package ru.netology.nework.models

data class Coordinates(
    val lat: String = "",
    val long: String = "",
) {
    override fun toString(): String {
        return "${this.lat};${this.long}"
    }
}
