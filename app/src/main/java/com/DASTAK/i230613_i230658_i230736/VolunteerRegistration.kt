package com.DASTAK.i230613_i230658_i230736

data class VolunteerRegistration(
    val registrationId: Int,
    val eventId: Int,
    val eventName: String,
    val eventDate: String,
    val eventTime: String?,
    val eventLocation: String,
    val volunteer: VolunteerInfo,
    val registeredAt: String,
    val status: String
)

data class VolunteerInfo(
    val volunteerId: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val profileImage: String?
)