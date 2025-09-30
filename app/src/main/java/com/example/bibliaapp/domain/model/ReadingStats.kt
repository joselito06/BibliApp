package com.example.bibliaapp.domain.model

data class ReadingStats(
    val booksRead: Int,
    val totalBooks: Int,
    val chaptersRead: Int,
    val versesRead: Int,
    val readingStreak: Int,
    val totalReadingTimeMinutes: Int
) {
    val progressPercentage: Float
        get() = if (totalBooks > 0) booksRead.toFloat() / totalBooks else 0f

    val totalReadingTimeFormatted: String
        get() {
            val hours = totalReadingTimeMinutes / 60
            val minutes = totalReadingTimeMinutes % 60
            return when {
                hours == 0 -> "${minutes}min"
                minutes == 0 -> "${hours}h"
                else -> "${hours}h ${minutes}min"
            }
        }
}
