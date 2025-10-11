package com.watxaut.fontsreviewer.data.repository

import android.content.Context
import com.watxaut.fontsreviewer.data.local.dao.FountainDao
import com.watxaut.fontsreviewer.data.mapper.toDomain
import com.watxaut.fontsreviewer.domain.model.Fountain
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import com.watxaut.fontsreviewer.util.CsvParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FountainRepositoryImpl @Inject constructor(
    private val fountainDao: FountainDao,
    @ApplicationContext private val context: Context
) : FountainRepository {

    override fun getAllFountains(): Flow<List<Fountain>> {
        return fountainDao.getAllFountains()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getFountainByCodi(codi: String): Fountain? {
        return fountainDao.getFountainByCodi(codi)?.toDomain()
    }

    override suspend fun initializeFountains() {
        val count = fountainDao.getFountainCount()
        if (count == 0) {
            val fountains = CsvParser.parseFountainsFromAssets(context)
            fountainDao.insertAll(fountains)
        }
    }

    override suspend fun getFountainCount(): Int {
        return fountainDao.getFountainCount()
    }
}
