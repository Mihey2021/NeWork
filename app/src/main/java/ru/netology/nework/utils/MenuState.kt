package ru.netology.nework.utils

object MenuState {
    private var menuState = MenuStates.SHOW_STATE

    fun setMenuState(menuState: MenuStates) {
        this.menuState = menuState
    }

    fun getMenuState() = this.menuState
}

enum class MenuStates(val id: Int) {
    SHOW_STATE(1),
    HIDE_STATE(1)

}