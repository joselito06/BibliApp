package com.example.bibliaapp.domain.usescases

import com.example.bibliaapp.data.database.repositories.BiblesRepository
import com.example.bibliaapp.data.networking.models.BibleVersionInfo
import javax.inject.Inject

class BibleUseCase @Inject constructor(val biblesRepo: BiblesRepository) {
    suspend operator fun invoke( filter: ((BibleVersionInfo) -> Boolean)? = null): List<BibleVersionInfo> {
        val list: List<BibleVersionInfo> = biblesRepo.getAll("listaBiblia", filter)
        return list
    }
}