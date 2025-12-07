package com.DASTAK.i230613_i230658_i230736

data class User(
    var id: String = "",
    var name: String = "",
    var username: String = "",
    var email: String = "",
    var password: String = "",
    var location: String = "",
    var profileImageBase64: String = "",
    var contributions: Int = 0,
    var timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", "", "", 0, 0L)
}
