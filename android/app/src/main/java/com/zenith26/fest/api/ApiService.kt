package com.zenith26.fest.api

import com.zenith26.fest.models.Event
import com.zenith26.fest.models.EventCreate
import com.zenith26.fest.models.RegistrationIn
import com.zenith26.fest.models.RegistrationItem
import com.zenith26.fest.models.RegistrationOut
import com.zenith26.fest.models.Stats
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface defining the exact contract for Zenith '26 backend endpoints.
 */
interface ApiService {

    /**
     * GET /api/events
     * Supports optional category filter and title search.
     */
    @GET("api/events")
    suspend fun getEvents(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): Response<List<Event>>

    /**
     * GET /api/events/{event_id}
     * Returns event detail or 404.
     */
    @GET("api/events/{event_id}")
    suspend fun getEventDetail(
        @Path("event_id") eventId: Int
    ): Response<Event>

    /**
     * POST /api/events/{event_id}/register
     * Submits student registration. Returns 201 on success or 400/409 on error.
     */
    @POST("api/events/{event_id}/register")
    suspend fun register(
        @Path("event_id") eventId: Int,
        @Body body: RegistrationIn
    ): Response<RegistrationOut>

    /**
     * GET /api/registrations
     * Fetches registrations list for admin ledger.
     */
    @GET("api/registrations")
    suspend fun getRegistrations(
        @Query("event_id") eventId: Int? = null
    ): Response<List<RegistrationItem>>

    /**
     * GET /api/stats
     * Computes live fest analytics telemetry.
     */
    @GET("api/stats")
    suspend fun getStats(): Response<Stats>

    /**
     * POST /api/events (Stretch Goal 1)
     * Creates a new event in fest catalog.
     */
    @POST("api/events")
    suspend fun createEvent(
        @Body body: EventCreate
    ): Response<Event>

    /**
     * DELETE /api/registrations/{registration_id} (Stretch Goal 2)
     * Cancels registration and immediately releases seat.
     */
    @DELETE("api/registrations/{registration_id}")
    suspend fun deleteRegistration(
        @Path("registration_id") registrationId: Int
    ): Response<Map<String, Any>>
}
