package com.DASTAK.i230613_i230658_i230736

import org.json.JSONObject

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
    val organizer: Organizer,
    val latitude: Double?,
    val longitude: Double?
) {
    companion object {
        fun fromJson(obj: JSONObject): Event {
            val organizerObj = obj.getJSONObject("organizer")
            return Event(
                event_id = obj.getInt("event_id"),
                event_name = obj.getString("event_name"),
                event_location = obj.getString("event_location"),
                event_date = obj.getString("event_date"),
                event_time = obj.optString("event_time", null),
                event_description = obj.getString("event_description"),
                poster_image = obj.optString("poster_image", null),
                volunteer_tasks = obj.optString("volunteer_tasks", null),
                things_to_bring = obj.optString("things_to_bring", null),
                meeting_point = obj.optString("meeting_point", null),
                contact_info = obj.optString("contact_info", null),
                status = obj.getString("status"),
                participant_count = obj.optInt("participant_count", 0),
                created_at = obj.getString("created_at"),
                organizer = Organizer(
                    user_id = organizerObj.getInt("user_id"),
                    name = organizerObj.getString("name"),
                    profile_image = organizerObj.optString("profile_image", null)
                ),
                latitude = if(obj.has("latitude") && !obj.isNull("latitude")) obj.getDouble("latitude") else null,
                longitude = if(obj.has("longitude") && !obj.isNull("longitude")) obj.getDouble("longitude") else null
            )
        }
    }
}

data class Organizer(
    val user_id: Int,
    val name: String,
    val profile_image: String?
)