package uk.ac.tees.mad.s3640901.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MedicineRepository(
    private val service: OpenFdaService = OpenFdaService.create()
) {

    suspend fun suggestMedicine(
        prefix: String
    ): Result<List<MedicineSuggestion>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val cleanPrefix = prefix
                    .trim()
                    .lowercase()

                if (cleanPrefix.length < 2) {
                    return@runCatching emptyList<MedicineSuggestion>()
                }

                val query =
                    "(openfda.generic_name:$cleanPrefix* " +
                            "OR openfda.brand_name:$cleanPrefix*)"

                val response = try {
                    service.searchDrugLabels(
                        search = query,
                        limit = 20
                    )
                } catch (exception: HttpException) {
                    if (exception.code() == 404) {
                        return@runCatching emptyList<MedicineSuggestion>()
                    } else {
                        throw exception
                    }
                }

                response.results
                    .flatMap { label ->
                        label.openFda?.genericName.orEmpty() +
                                label.openFda?.brandName.orEmpty()
                    }
                    .map { medicineName ->
                        medicineName.trim()
                    }
                    .filter { medicineName ->
                        medicineName.isNotBlank() &&
                                medicineName.startsWith(
                                    prefix = cleanPrefix,
                                    ignoreCase = true
                                )
                    }
                    .distinctBy { medicineName ->
                        medicineName.lowercase()
                    }
                    .sorted()
                    .take(20)
                    .map { medicineName ->
                        MedicineSuggestion(
                            name = medicineName
                        )
                    }
            }
        }
    }
}