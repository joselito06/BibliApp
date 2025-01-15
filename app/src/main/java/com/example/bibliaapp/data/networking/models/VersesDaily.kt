package com.example.bibliaapp.data.networking.models

import com.google.gson.annotations.SerializedName

data class VersesDaily(
    @SerializedName("verse_content") val verse_content: String,
    @SerializedName("verse_direction") val verse_direction: String,
    @SerializedName("verse_id") val verse_id: Int,
    @SerializedName("code") val code: String
)
