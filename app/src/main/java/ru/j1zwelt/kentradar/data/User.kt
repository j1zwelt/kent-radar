package ru.j1zwelt.kentradar.data

import androidx.compose.ui.graphics.ImageBitmap

data class User(
    val uid: String,
    val userName: String,
    val name: String,
    val status: Int,
    val latitude: Double,
    val longitude: Double,
    val isFriend: Boolean,
    var avatarBitmap: ImageBitmap? = null
)