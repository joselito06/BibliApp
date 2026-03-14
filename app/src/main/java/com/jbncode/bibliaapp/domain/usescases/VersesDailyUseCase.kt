package com.jbncode.bibliaapp.domain.usescases

import com.jbncode.bibliaapp.data.database.repositories.VersesDailyRepository
import com.jbncode.bibliaapp.data.networking.models.VersesDaily
import javax.inject.Inject

class VersesDailyUseCase @Inject constructor(val verseRepo: VersesDailyRepository) {

    suspend operator fun invoke(fileName: String, filter: ((VersesDaily) -> Boolean)? = null): List<VersesDaily> {
        val list: List<VersesDaily> = verseRepo.GetAll(fileName, filter)
        return list
    }

}