package com.DASTAK.i230613_i230658_i230736.models

data class listing(
    var id: String = "",
    var title: String = "",
    var subtitle: String = "",
    var imageBase64: String = "", // Store image as Base64
    var imageRes: Int = 0,
    var date: String = "",
    var userId: String = "",
    var timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", 0, "", "", 0L)
}