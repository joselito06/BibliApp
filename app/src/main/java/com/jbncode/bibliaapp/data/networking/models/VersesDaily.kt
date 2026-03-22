package com.jbncode.bibliaapp.data.networking.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class VersesDaily(
    @SerializedName("verse_content") val verse_content: String?,
    @SerializedName("verse_direction") val verse_direction: String?,
    @SerializedName("verse_id") val verse_id: Int,
    @SerializedName("code") val code: String
)
