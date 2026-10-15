package com.zenith26.fest.models

import com.google.gson.annotations.SerializedName

/**
 * Live fest analytics statistics matching StatsOut from FastAPI.
 */
data class Stats(
    @SerializedName("total_events") val totalEvents: Int,
    @SerializedName("total_registrations") val totalRegistrations: Int,
    @SerializedName("total_capacity") val totalCapacity: Int,
    @SerializedName("overall_utilization_pct") val overallUtilizationPct: Float,
    @SerializedName("upcoming_events_count") val upcomingEventsCount: Int,
    @SerializedName("events_by_category") val eventsByCategory: Map<String, Int>
)
