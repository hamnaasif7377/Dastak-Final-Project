package com.DASTAK.i230613_i230658_i230736

data class Event(
    val event_id: Int,
    val event_name: String,
    val event_location: String,
    val event_date: String,
    val event_time: String?,
    val event_description: String,
    val poster_image: String?,
    val volunteer_tasks: String?,
    val things_to_bring: String?,
    val meeting_point: String?,
    val contact_info: String?,
    val status: String,
    val participant_count: Int,
    val created_at: String,
    val organizer: Organizer
)

data class Organizer(
    val user_id: Int,
    val name: String,
    val profile_image: String?,
    val email: String? = null
)