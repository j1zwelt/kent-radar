package ru.j1zwelt.kentradar.model

data class User(
    val name: String,
    val userName: String,
    val status: Int,
    val longitude: Double,
    val latitude: Double,
    var uid: String = "",
    var isFriend: Boolean = false,
    var avatarBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
)