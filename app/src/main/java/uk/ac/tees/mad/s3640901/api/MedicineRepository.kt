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
        val cleanPrefix = prefix.trim()

        if (cleanPrefix.length < 2) {
            return Result.success(emptyList())
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val query =
                    "(openfda.generic_name:$cleanPrefix* " +
                            "OR openfda.brand_name:$cleanPrefix*)"

                val response = service.searchDrugLabels(
                    search = query,
                    limit = 50
                )

                response.results
                    .flatMap { label ->
                        label.openFda?.genericName.orEmpty() +
                                label.openFda?.brandName.orEmpty()
                    }
                    .map { it.trim() }
                    .filter {
                        it.isNotBlank() &&
                                it.startsWith(
                                    cleanPrefix,
                                    ignoreCase = true
                                )
                    }
                    .distinctBy {
                        it.lowercase()
                    }
                    .sorted()
                    .take(20)
                    .map {
                        MedicineSuggestion(it)
                    }
            }.recover { exception ->
                if (
                    exception is HttpException &&
                    exception.code() == 404
                ) {
                    emptyList()
                } else {
                    throw exception
                }
            }
        }
    }
}