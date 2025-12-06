package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class OfflineSyncManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val client = OkHttpClient()

    companion object {
        private const val KEY_PENDING_SAVES = "pending_saves"
        private const val KEY_PENDING_UNSAVES = "pending_unsaves"
        private const val KEY_CACHED_EVENTS = "cached_events"
        private const val KEY_SAVED_EVENT_IDS = "saved_event_ids"
    }

    // -------------------------------
    // SAVE OPERATIONS
    // -------------------------------

    fun addPendingSave(userId: Int, eventId: Int) {
        val pending = getPendingSaves().toMutableList()
        val saveData = SaveOperation(userId, eventId)

        // Remove from unsaves if it exists there
        removePendingUnsave(userId, eventId)

        // Add to pending saves if not already there
        if (!pending.contains(saveData)) {
            pending.add(saveData)
            prefs.edit().putString(KEY_PENDING_SAVES, gson.toJson(pending)).apply()
        }

        // Add to local saved IDs
        addToLocalSavedIds(eventId)
    }

    fun addPendingUnsave(userId: Int, eventId: Int) {
        val pending = getPendingUnsaves().toMutableList()
        val unsaveData = SaveOperation(userId, eventId)

        // Remove from saves if it exists there
        removePendingSave(userId, eventId)

        // Add to pending unsaves if not already there
        if (!pending.contains(unsaveData)) {
            pending.add(unsaveData)
            prefs.edit().putString(KEY_PENDING_UNSAVES, gson.toJson(pending)).apply()
        }

        // Remove from local saved IDs
        removeFromLocalSavedIds(eventId)
    }

    private fun removePendingSave(userId: Int, eventId: Int) {
        val pending = getPendingSaves().toMutableList()
        pending.removeAll { it.userId == userId && it.eventId == eventId }
        prefs.edit().putString(KEY_PENDING_SAVES, gson.toJson(pending)).apply()
    }

    private fun removePendingUnsave(userId: Int, eventId: Int) {
        val pending = getPendingUnsaves().toMutableList()
        pending.removeAll { it.userId == userId && it.eventId == eventId }
        prefs.edit().putString(KEY_PENDING_UNSAVES, gson.toJson(pending)).apply()
    }

    fun getPendingSaves(): List<SaveOperation> {
        val json = prefs.getString(KEY_PENDING_SAVES, "[]")
        val type = object : TypeToken<List<SaveOperation>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getPendingUnsaves(): List<SaveOperation> {
        val json = prefs.getString(KEY_PENDING_UNSAVES, "[]")
        val type = object : TypeToken<List<SaveOperation>>() {}.type
        return gson.fromJson(json, type)
    }

    // -------------------------------
    // LOCAL SAVED EVENT IDS
    // -------------------------------

    private fun addToLocalSavedIds(eventId: Int) {
        val ids = getLocalSavedEventIds().toMutableSet()
        ids.add(eventId)
        prefs.edit().putString(KEY_SAVED_EVENT_IDS, gson.toJson(ids)).apply()
    }

    private fun removeFromLocalSavedIds(eventId: Int) {
        val ids = getLocalSavedEventIds().toMutableSet()
        ids.remove(eventId)
        prefs.edit().putString(KEY_SAVED_EVENT_IDS, gson.toJson(ids)).apply()
    }

    fun getLocalSavedEventIds(): Set<Int> {
        val json = prefs.getString(KEY_SAVED_EVENT_IDS, "[]")
        val type = object : TypeToken<Set<Int>>() {}.type
        return gson.fromJson(json, type) ?: emptySet()
    }

    fun isEventSavedLocally(eventId: Int): Boolean {
        return getLocalSavedEventIds().contains(eventId)
    }

    // -------------------------------
    // EVENT CACHING
    // -------------------------------

    fun cacheEvent(event: Event) {
        val cached = getCachedEvents().toMutableList()
        // Remove if already exists
        cached.removeAll { it.event_id == event.event_id }
        // Add new/updated version
        cached.add(event)
        prefs.edit().putString(KEY_CACHED_EVENTS, gson.toJson(cached)).apply()
    }

    fun getCachedEvents(): List<Event> {
        val json = prefs.getString(KEY_CACHED_EVENTS, "[]")
        val type = object : TypeToken<List<Event>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getCachedEvent(eventId: Int): Event? {
        return getCachedEvents().find { it.event_id == eventId }
    }

    fun getSavedEventsFromCache(userId: Int): List<Event> {
        val savedIds = getLocalSavedEventIds()
        return getCachedEvents().filter { savedIds.contains(it.event_id) }
    }

    // -------------------------------
    // SYNC WITH SERVER
    // -------------------------------

    fun syncWithServer(apiBaseUrl: String, onComplete: (success: Boolean, synced: Int) -> Unit) {
        val pendingSaves = getPendingSaves()
        val pendingUnsaves = getPendingUnsaves()

        if (pendingSaves.isEmpty() && pendingUnsaves.isEmpty()) {
            onComplete(true, 0)
            return
        }

        var completed = 0
        val total = pendingSaves.size + pendingUnsaves.size
        var hasError = false

        // Sync saves
        pendingSaves.forEach { saveOp ->
            syncSaveOperation(apiBaseUrl, saveOp, "save") { success ->
                if (success) {
                    removePendingSave(saveOp.userId, saveOp.eventId)
                } else {
                    hasError = true
                }
                completed++
                if (completed == total) {
                    onComplete(!hasError, total)
                }
            }
        }

        // Sync unsaves
        pendingUnsaves.forEach { saveOp ->
            syncSaveOperation(apiBaseUrl, saveOp, "unsave") { success ->
                if (success) {
                    removePendingUnsave(saveOp.userId, saveOp.eventId)
                } else {
                    hasError = true
                }
                completed++
                if (completed == total) {
                    onComplete(!hasError, total)
                }
            }
        }
    }

    private fun syncSaveOperation(
        apiBaseUrl: String,
        saveOp: SaveOperation,
        action: String,
        onComplete: (success: Boolean) -> Unit
    ) {
        val formBody = FormBody.Builder()
            .add("user_id", saveOp.userId.toString())
            .add("event_id", saveOp.eventId.toString())
            .add("action", action)
            .build()

        val request = Request.Builder()
            .url("${apiBaseUrl}save_event.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OfflineSync", "Failed to sync $action operation", e)
                onComplete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "{}")
                    val status = jsonResponse.getString("status")
                    onComplete(status == "success")
                } catch (e: Exception) {
                    Log.e("OfflineSync", "Error parsing sync response", e)
                    onComplete(false)
                }
            }
        })
    }

    // -------------------------------
    // CLEAR DATA
    // -------------------------------

    fun clearAllOfflineData() {
        prefs.edit().clear().apply()
    }

    fun hasPendingOperations(): Boolean {
        return getPendingSaves().isNotEmpty() || getPendingUnsaves().isNotEmpty()
    }
}

data class SaveOperation(
    val userId: Int,
    val eventId: Int
)