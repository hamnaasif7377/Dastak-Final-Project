package com.DASTAK.i230613_i230658_i230736.models

data class listing(
    var id: String = "",
    var userId: Int = 0,
    var title: String = "",
    var subtitle: String = "",
    var imageBase64: String = "",
    var date: String = "",
    var timestamp: Long = 0L
) {
    constructor() : this("", 0, "", "", "", "", 0L)


    fun getUserIdAsInt(): Int {
        return when (userId) {
            is Int -> userId as Int
            is Long -> (userId as Long).toInt()
            is String -> (userId as String).toIntOrNull() ?: 0
            is Double -> (userId as Double).toInt()
            else -> 0
        }
    }
}