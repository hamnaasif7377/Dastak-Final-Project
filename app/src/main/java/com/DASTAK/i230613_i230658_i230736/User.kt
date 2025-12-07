package com.DASTAK.i230613_i230658_i230736

data class User(
    var id: Any = 0,  // ✅ Changed to Any to handle Int, Long, or String
    var name: String = "",
    var username: String = "",
    var email: String = "",
    var password: String = "",
    var location: String = "",
    var profileImageBase64: String = "",
    var contributions: Any = 0,  // ✅ Changed to Any to handle Int or Long
    var timestamp: Any = 0L  // ✅ Changed to Any to handle Long or Int
) {
    // No-argument constructor required by Firebase
    constructor() : this(0, "", "", "", "", "", "", 0, 0L)

    // Helper to get id as Int
    fun getIdAsInt(): Int {
        return when (id) {
            is Int -> id as Int
            is Long -> (id as Long).toInt()
            is String -> (id as String).toIntOrNull() ?: 0
            is Double -> (id as Double).toInt()
            else -> 0
        }
    }

    // Helper to get contributions as Int
    fun getContributionsAsInt(): Int {
        return when (contributions) {
            is Int -> contributions as Int
            is Long -> (contributions as Long).toInt()
            is String -> (contributions as String).toIntOrNull() ?: 0
            is Double -> (contributions as Double).toInt()
            else -> 0
        }
    }

    // Helper to get timestamp as Long
    fun getTimestampAsLong(): Long {
        return when (timestamp) {
            is Long -> timestamp as Long
            is Int -> (timestamp as Int).toLong()
            is String -> (timestamp as String).toLongOrNull() ?: 0L
            is Double -> (timestamp as Double).toLong()
            else -> 0L
        }
    }
}