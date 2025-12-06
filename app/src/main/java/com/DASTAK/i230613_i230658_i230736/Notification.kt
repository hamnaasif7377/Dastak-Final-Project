package com.DASTAK.i230613_i230658_i230736

data class Notification(
    val notificationId: Int,
    val notificationType: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String,
    val eventId: Int?,
    val eventName: String?,
    val registrationId: Int?,
    val registrationStatus: String?,
    val sender: NotificationSender?
)

data class NotificationSender(
    val userId: Int,
    val name: String,
    val profileImage: String?,
    val role: String? = null
)