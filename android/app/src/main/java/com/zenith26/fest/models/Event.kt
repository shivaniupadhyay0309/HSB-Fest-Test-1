package com.zenith26.fest.models

import com.google.gson.annotations.SerializedName

/**
 * Event model matching EventOut and EventDetail from FastAPI backend.
 */
data class Event(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("event_time") val eventTime: String,
    @SerializedName("venue") val venue: String,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("fee") val fee: Int = 0,
    @SerializedName("icon") val icon: String? = "✨",
    @SerializedName("available_seats") val availableSeats: Int
) {
    val isFull: Boolean get() = availableSeats <= 0

    val formattedFee: String get() = if (fee == 0) "Free" else "₹$fee"

    val formattedSeats: String get() = "$availableSeats / $capacity"

    val registeredCount: Int get() = (capacity - availableSeats).coerceAtLeast(0)

    val utilizationPct: Float get() = if (capacity > 0) {
        ((registeredCount.toFloat() / capacity.toFloat()) * 100f)
    } else 0f

    val isHighDemand: Boolean get() = utilizationPct >= 90f
}

/**
 * Request payload for creating a new fest event (Stretch Goal 1).
 */
data class EventCreate(
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("event_time") val eventTime: String,
    @SerializedName("venue") val venue: String,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("fee") val fee: Int = 0,
    @SerializedName("icon") val icon: String? = "✨"
)
