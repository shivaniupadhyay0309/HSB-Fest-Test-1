package com.zenith26.fest.models

import com.google.gson.annotations.SerializedName

/**
 * Registration request payload matching RegistrationIn from backend.
 */
data class RegistrationIn(
    @SerializedName("student_name") val studentName: String,
    @SerializedName("roll_number") val rollNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null
)

/**
 * Successful registration response matching RegistrationOut.
 */
data class RegistrationOut(
    @SerializedName("id") val id: Int,
    @SerializedName("event_id") val eventId: Int,
    @SerializedName("student_name") val studentName: String,
    @SerializedName("registered_at") val registeredAt: String
)

/**
 * Registration record with event title for Admin Dashboard ledger.
 */
data class RegistrationItem(
    @SerializedName("id") val id: Int,
    @SerializedName("event_id") val eventId: Int,
    @SerializedName("event_title") val eventTitle: String,
    @SerializedName("student_name") val studentName: String,
    @SerializedName("roll_number") val rollNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("registered_at") val registeredAt: String
)
