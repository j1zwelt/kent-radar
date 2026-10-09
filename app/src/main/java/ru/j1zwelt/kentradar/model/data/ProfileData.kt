package ru.j1zwelt.kentradar.model.data

data class ProfileData(
    val userName: String = "", val name: String = "", var avatar: String? = null
) {
    fun toMap(): Map<String, Any> =
        mapOf("userName" to userName, "name" to name, "avatar" to avatar!!)
}