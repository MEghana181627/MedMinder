package uk.ac.tees.mad.s3640901.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFdaService {

    @GET("drug/label.json")
    suspend fun searchDrugLabels(
        @Query("search") search: String,
        @Query("limit") limit: Int = 20
    ): OpenFdaResponse

    companion object {
        fun create(): OpenFdaService {
            return Retrofit.Builder()
                .baseUrl("https://api.fda.gov/")
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()
                .create(OpenFdaService::class.java)
        }
    }
}