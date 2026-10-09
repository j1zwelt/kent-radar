package ru.j1zwelt.kentradar.model.data

import ru.j1zwelt.kentradar.model.User

data class UserData(
    val name: String = "",
    val userName: String = "",
    val status: Int = 0,
    val longitude: Double = 0.0,
    val latitude: Double = 0.0,
    val avatar: String = ""
) {
    fun toUser(): User = User(name, userName, status, longitude, latitude)
}