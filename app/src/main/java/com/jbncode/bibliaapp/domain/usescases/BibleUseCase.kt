package com.jbncode.bibliaapp.domain.usescases

import com.jbncode.bibliaapp.data.database.repositories.BiblesRepository
import com.jbncode.bibliaapp.data.networking.models.BibleVersionInfo
import javax.inject.Inject

class BibleUseCase @Inject constructor(val biblesRepo: BiblesRepository) {
    suspend operator fun invoke( filter: ((BibleVersionInfo) -> Boolean)? = null): List<BibleVersionInfo> {
        val list: List<BibleVersionInfo> = biblesRepo.getAll("listaBiblia", filter)
        return list
    }
}