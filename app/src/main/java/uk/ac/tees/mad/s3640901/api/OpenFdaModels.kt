package uk.ac.tees.mad.s3640901.api

import com.google.gson.annotations.SerializedName

data class OpenFdaResponse(
    @SerializedName("results")
    val results: List<OpenFdaLabel> = emptyList()
)

data class OpenFdaLabel(
    @SerializedName("openfda")
    val openFda: OpenFdaIdentifiers? = null
)

data class OpenFdaIdentifiers(
    @SerializedName("generic_name")
    val genericName: List<String>? = null,

    @SerializedName("brand_name")
    val brandName: List<String>? = null
)

data class MedicineSuggestion(
    val name: String
)