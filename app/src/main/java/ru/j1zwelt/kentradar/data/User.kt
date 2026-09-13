package ru.j1zwelt.kentradar.data

data class User(
    val uid: String,
    val userName: String,
    val name: String,
    val status: Int,
    val latitude: Double,
    val longitude: Double,
    val isFriend: Boolean
)