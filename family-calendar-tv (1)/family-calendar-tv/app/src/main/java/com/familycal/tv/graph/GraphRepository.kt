package com.familycal.tv.graph

import com.familycal.tv.model.FamilyEvent
import com.familycal.tv.model.FamilyMember
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

// --- Raw Graph API response shapes -----------------------------------------

data class GraphCalendarViewResponse(
    @SerializedName("value") val events: List<GraphEvent>
)

data class GraphEvent(
    val id: String,
    val subject: String,
    val start: GraphDateTime,
    val end: GraphDateTime,
    val isAllDay: Boolean,
    val location: GraphLocation?
)

data class GraphDateTime(val dateTime: String, val timeZone: String)
data class GraphLocation(val displayName: String?)

interface GraphApi {
    // calendarView expands recurring events into individual instances,
    // which is what you want for a "what's happening this week" display.
    @GET("me/calendarView")
    suspend fun getCalendarView(
        @Header("Authorization") bearerToken: String,
        @Query("startDateTime") start: String,
        @Query("endDateTime") end: String,
        @Query("\$orderby") orderBy: String = "start/dateTime",
        @Query("\$top") top: Int = 100
    ): GraphCalendarViewResponse

    @retrofit2.http.POST("me/events")
    suspend fun createEvent(
        @Header("Authorization") bearerToken: String,
        @retrofit2.http.Body body: NewGraphEvent
    ): GraphEvent
}

data class NewGraphEvent(
    val subject: String,
    val start: GraphDateTime,
    val end: GraphDateTime,
    val location: GraphLocation? = null
)

/**
 * Fetches each signed-in family member's calendar in parallel and merges
 * them into one sorted, color-tagged list for the combined TV view.
 */
class GraphRepository {

    private val api: GraphApi = Retrofit.Builder()
        .baseUrl("https://graph.microsoft.com/v1.0/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GraphApi::class.java)

    /**
     * [members] maps each FamilyMember to their current access token.
     * Fetches all calendars concurrently, then merges + sorts by start time.
     */
    suspend fun getCombinedFamilyEvents(
        members: Map<FamilyMember, String>,
        rangeStart: Date,
        rangeEnd: Date
    ): List<FamilyEvent> = coroutineScope {
        val iso = isoFormatter()
        val startStr = iso.format(rangeStart)
        val endStr = iso.format(rangeEnd)

        val deferredPerMember = members.map { (member, token) ->
            async(Dispatchers.IO) {
                runCatching {
                    api.getCalendarView(
                        bearerToken = "Bearer $token",
                        start = startStr,
                        end = endStr
                    ).events.map { it.toFamilyEvent(member) }
                }.getOrElse {
                    // One family member's token expiring/failing shouldn't blank
                    // the whole screen -- just skip their events for this refresh.
                    emptyList()
                }
            }
        }

        deferredPerMember.awaitAll().flatten().sortedBy { it.startEpochMillis }
    }

    private fun GraphEvent.toFamilyEvent(owner: FamilyMember) = FamilyEvent(
        id = id,
        subject = subject,
        startEpochMillis = Instant.parse(start.dateTime + "Z").toEpochMilli(),
        endEpochMillis = Instant.parse(end.dateTime + "Z").toEpochMilli(),
        isAllDay = isAllDay,
        location = location?.displayName,
        owner = owner
    )

    private fun isoFormatter(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
}

private suspend fun <T> List<kotlinx.coroutines.Deferred<T>>.awaitAll(): List<T> =
    withContext(Dispatchers.Default) { map { it.await() } }
