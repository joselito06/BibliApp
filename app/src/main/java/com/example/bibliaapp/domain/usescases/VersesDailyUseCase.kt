package com.example.bibliaapp.domain.usescases

import com.example.bibliaapp.data.database.repositories.VersesDailyRepository
import com.example.bibliaapp.data.networking.models.VersesDaily
import com.example.bibliaapp.domain.model.Books
import javax.inject.Inject

class VersesDailyUseCase @Inject constructor(val verseRepo: VersesDailyRepository) {

    suspend operator fun invoke(): List<VersesDaily> {
        val list: List<VersesDaily> = verseRepo.GetAll()
        return list
    }

}