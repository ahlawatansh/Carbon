package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiCarbonResponse(
    val mealName: String,
    val originalCarbonScore: Double,
    val ingredientBreakdown: List<IngredientScore>? = emptyList(),
    val alternativeMealName: String,
    val alternativeCarbonScore: Double,
    val alternativeDescription: String
)

@JsonClass(generateAdapter = true)
data class IngredientScore(
    val name: String,
    val carbonScore: Double
)
