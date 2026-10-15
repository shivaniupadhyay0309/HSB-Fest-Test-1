package com.zenith26.fest.models

import com.google.gson.annotations.SerializedName

/**
 * Standard FastAPI error response model containing the error detail.
 */
data class ApiErrorResponse(
    @SerializedName("detail") val detail: String? = null
)
