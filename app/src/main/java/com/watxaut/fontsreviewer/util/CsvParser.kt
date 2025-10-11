package com.watxaut.fontsreviewer.util

import android.content.Context
import com.watxaut.fontsreviewer.data.local.entity.FountainEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvParser {

    suspend fun parseFountainsFromAssets(
        context: Context,
        fileName: String = "2025_fonts_bcn.csv"
    ): List<FountainEntity> = withContext(Dispatchers.IO) {
        val fountains = mutableListOf<FountainEntity>()

        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    // Skip BOM if present
                    reader.mark(1)
                    val firstChar = reader.read()
                    if (firstChar != 0xFEFF) {
                        reader.reset()
                    }

                    // Skip header line
                    reader.readLine()

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { csvLine ->
                            parseLine(csvLine)?.let { fountain ->
                                fountains.add(fountain)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        fountains
    }

    private fun parseLine(line: String): FountainEntity? {
        return try {
            // Split by comma, handling quoted fields
            val fields = splitCsvLine(line)

            if (fields.size < 8) return null

            FountainEntity(
                codi = fields[0].trim(),
                nom = fields[1].trim(),
                carrer = fields[2].trim(),
                numeroCarrer = fields[3].trim(),
                latitude = fields[6].toDoubleOrNull() ?: return null,
                longitude = fields[7].toDoubleOrNull() ?: return null
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().removeSurrounding("\""))
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().removeSurrounding("\""))

        return result
    }
}
