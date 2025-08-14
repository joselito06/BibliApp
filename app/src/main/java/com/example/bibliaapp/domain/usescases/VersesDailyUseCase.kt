package com.example.bibliaapp.domain.usescases

import com.example.bibliaapp.data.database.repositories.VersesDailyRepository
import com.example.bibliaapp.data.networking.models.VersesDaily
import javax.inject.Inject

class VersesDailyUseCase @Inject constructor(val verseRepo: VersesDailyRepository) {

    suspend operator fun invoke(fileName: String, filter: ((VersesDaily) -> Boolean)? = null): List<VersesDaily> {
        val list: List<VersesDaily> = verseRepo.GetAll(fileName, filter)
        return list
    }

}