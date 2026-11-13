package com.smartcodeapps.constructionprizewheel.data

import com.smartcodeapps.constructionprizewheel.data.db.PrizeDao
import com.smartcodeapps.constructionprizewheel.data.db.toDomain
import com.smartcodeapps.constructionprizewheel.data.db.toEntity
import com.smartcodeapps.constructionprizewheel.data.image.ImageStore
import com.smartcodeapps.constructionprizewheel.domain.ConfigError
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.domain.PrizeConfigValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SaveResult {
    data object Success : SaveResult
    data class Invalid(val errors: List<ConfigError>) : SaveResult
}

@Singleton
class PrizeRepository @Inject constructor(
    private val dao: PrizeDao,
    private val imageStore: ImageStore
) {
    fun observePrizes(): Flow<List<Prize>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun saveAll(prizes: List<Prize>): SaveResult {
        val errors = PrizeConfigValidator.validate(prizes)
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)

        dao.replaceAll(prizes.map { (if (it.id < 0) it.copy(id = 0) else it).toEntity() })
        imageStore.deletePrizeImagesExcept(prizes.mapNotNull { it.imagePath })
        return SaveResult.Success
    }

    suspend fun clearAll() {
        dao.deleteAll()
        imageStore.deleteAllPrizeImages()
    }
}
